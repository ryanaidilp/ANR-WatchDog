/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Salomon BRYS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.anrwatchdog

import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * A watchdog timer thread that detects when the UI thread has frozen.
 *
 * @param timeoutInterval The interval, in milliseconds, between checks of the UI thread.
 *                        It is therefore the maximum time the UI may freeze before being reported as ANR.
 *                        Default is [DEFAULT_ANR_TIMEOUT] (5000ms).
 */
class ANRWatchDog @JvmOverloads constructor(
    private val timeoutInterval: Int = DEFAULT_ANR_TIMEOUT
) : Thread() {

    /**
     * Listener interface for ANR detection.
     */
    fun interface ANRListener {
        /**
         * Called when an ANR is detected.
         *
         * @param error The error describing the ANR.
         */
        fun onAppNotResponding(error: ANRError)
    }

    /**
     * Interceptor interface for custom ANR handling.
     */
    fun interface ANRInterceptor {
        /**
         * Called when main thread has frozen more time than defined by the timeout.
         *
         * @param duration The minimum time (in ms) the main thread has been frozen (may be more).
         * @return 0 or negative if the ANR should be reported immediately.
         *         A positive number of ms to postpone the reporting.
         */
        fun intercept(duration: Long): Long
    }

    /**
     * Listener interface for watchdog thread interruption.
     */
    fun interface InterruptionListener {
        fun onInterrupted(exception: InterruptedException)
    }

    private var anrListener: ANRListener = DEFAULT_ANR_LISTENER
    private var anrInterceptor: ANRInterceptor = DEFAULT_ANR_INTERCEPTOR
    private var interruptionListener: InterruptionListener = DEFAULT_INTERRUPTION_LISTENER

    private val uiHandler = Handler(Looper.getMainLooper())

    private var namePrefix: String? = ""
    private var logThreadsWithoutStackTrace = false
    private var ignoreDebugger = false

    @Volatile
    private var tick: Long = 0

    @Volatile
    private var reported = false

    private val ticker = Runnable {
        tick = 0
        reported = false
    }

    /**
     * Returns the timeout interval in milliseconds.
     */
    fun getTimeoutInterval(): Int = timeoutInterval

    /**
     * Sets an interface for when an ANR is detected.
     * If not set, the default behavior is to throw an error and crash the application.
     *
     * @param listener The new listener or null to reset to default.
     * @return itself for chaining.
     */
    fun setANRListener(listener: ANRListener?): ANRWatchDog {
        anrListener = listener ?: DEFAULT_ANR_LISTENER
        return this
    }

    /**
     * Sets an interface to intercept ANRs before they are reported.
     * If set, you can define if, given the current duration of the detected ANR
     * and external context, it is necessary to report the ANR.
     *
     * @param interceptor The new interceptor or null to reset to default.
     * @return itself for chaining.
     */
    fun setANRInterceptor(interceptor: ANRInterceptor?): ANRWatchDog {
        anrInterceptor = interceptor ?: DEFAULT_ANR_INTERCEPTOR
        return this
    }

    /**
     * Sets an interface for when the watchdog thread is interrupted.
     * If not set, the default behavior is to just log the interruption message.
     *
     * @param listener The new listener or null to reset to default.
     * @return itself for chaining.
     */
    fun setInterruptionListener(listener: InterruptionListener?): ANRWatchDog {
        interruptionListener = listener ?: DEFAULT_INTERRUPTION_LISTENER
        return this
    }

    /**
     * Set the prefix that a thread's name must have for the thread to be reported.
     * Note that the main thread is always reported.
     * Default "".
     *
     * @param prefix The thread name's prefix for a thread to be reported.
     * @return itself for chaining.
     */
    fun setReportThreadNamePrefix(prefix: String?): ANRWatchDog {
        namePrefix = prefix ?: ""
        return this
    }

    /**
     * Set that only the main thread will be reported.
     *
     * @return itself for chaining.
     */
    fun setReportMainThreadOnly(): ANRWatchDog {
        namePrefix = null
        return this
    }

    /**
     * Set that all threads will be reported (default behaviour).
     *
     * @return itself for chaining.
     */
    fun setReportAllThreads(): ANRWatchDog {
        namePrefix = ""
        return this
    }

    /**
     * Set that all running threads will be reported,
     * even those from which no stack trace could be extracted.
     * Default false.
     *
     * @param logThreadsWithoutStackTrace Whether or not all running threads should be reported.
     * @return itself for chaining.
     */
    fun setLogThreadsWithoutStackTrace(logThreadsWithoutStackTrace: Boolean): ANRWatchDog {
        this.logThreadsWithoutStackTrace = logThreadsWithoutStackTrace
        return this
    }

    /**
     * Set whether to ignore the debugger when detecting ANRs.
     * When ignoring the debugger, ANRWatchdog will detect ANRs even if the debugger is connected.
     * By default, it does not, to avoid interpreting debugging pauses as ANRs.
     * Default false.
     *
     * @param ignoreDebugger Whether to ignore the debugger.
     * @return itself for chaining.
     */
    fun setIgnoreDebugger(ignoreDebugger: Boolean): ANRWatchDog {
        this.ignoreDebugger = ignoreDebugger
        return this
    }

    override fun run() {
        name = "|ANR-WatchDog|"

        var interval = timeoutInterval.toLong()
        while (!isInterrupted) {
            val needPost = tick == 0L
            tick += interval
            if (needPost) {
                uiHandler.post(ticker)
            }

            try {
                sleep(interval)
            } catch (e: InterruptedException) {
                interruptionListener.onInterrupted(e)
                return
            }

            // If the main thread has not handled ticker, it is blocked. ANR.
            if (tick != 0L && !reported) {
                if (!ignoreDebugger && (Debug.isDebuggerConnected() || Debug.waitingForDebugger())) {
                    Log.w(
                        TAG,
                        "An ANR was detected but ignored because the debugger is connected " +
                                "(you can prevent this with setIgnoreDebugger(true))"
                    )
                    reported = true
                    continue
                }

                interval = anrInterceptor.intercept(tick)
                if (interval > 0) {
                    continue
                }

                val error = if (namePrefix != null) {
                    ANRError.create(tick, namePrefix, logThreadsWithoutStackTrace)
                } else {
                    ANRError.createMainOnly(tick)
                }

                anrListener.onAppNotResponding(error)
                interval = timeoutInterval.toLong()
                reported = true
            }
        }
    }

    companion object {
        private const val TAG = "ANRWatchdog"
        private const val DEFAULT_ANR_TIMEOUT = 5000

        private val DEFAULT_ANR_LISTENER = ANRListener { error -> throw error }

        private val DEFAULT_ANR_INTERCEPTOR = ANRInterceptor { 0 }

        private val DEFAULT_INTERRUPTION_LISTENER = InterruptionListener { exception ->
            Log.w(TAG, "Interrupted: ${exception.message}")
        }
    }
}
