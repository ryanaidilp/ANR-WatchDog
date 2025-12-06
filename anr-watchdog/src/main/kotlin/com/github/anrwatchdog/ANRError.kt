package com.github.anrwatchdog

import android.os.Looper
import java.io.Serializable
import java.util.TreeMap

/**
 * Error thrown by [ANRWatchDog] when an ANR is detected.
 * Contains the stack trace of the frozen UI thread.
 *
 * It is important to notice that, in an ANRError, all the "Caused by" are not really the cause
 * of the exception. Each "Caused by" is the stack trace of a running thread. Note that the main
 * thread always comes first.
 *
 * @property duration The minimum duration, in ms, for which the main thread has been blocked. May be more.
 */
class ANRError private constructor(
    thread: ThreadInfo.ThreadWrapper?,
    @JvmField val duration: Long
) : Error("Application Not Responding for at least $duration ms.", thread) {

    override fun fillInStackTrace(): Throwable {
        stackTrace = emptyArray()
        return this
    }

    private class ThreadInfo(
        private val name: String,
        private val stackTrace: Array<StackTraceElement>
    ) : Serializable {

        inner class ThreadWrapper(other: ThreadWrapper?) : Throwable(name, other) {
            override fun fillInStackTrace(): Throwable {
                setStackTrace(this@ThreadInfo.stackTrace)
                return this
            }
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L

        @JvmStatic
        internal fun create(
            duration: Long,
            prefix: String?,
            logThreadsWithoutStackTrace: Boolean
        ): ANRError {
            val mainThread = Looper.getMainLooper().thread

            val stackTraces = TreeMap<Thread, Array<StackTraceElement>> { lhs, rhs ->
                when {
                    lhs === rhs -> 0
                    lhs === mainThread -> 1
                    rhs === mainThread -> -1
                    else -> rhs.name.compareTo(lhs.name)
                }
            }

            Thread.getAllStackTraces().forEach { (thread, trace) ->
                if (thread === mainThread ||
                    (prefix != null &&
                            thread.name.startsWith(prefix) &&
                            (logThreadsWithoutStackTrace || trace.isNotEmpty()))
                ) {
                    stackTraces[thread] = trace
                }
            }

            // Sometimes main is not returned in getAllStackTraces() - ensure that we list it
            if (!stackTraces.containsKey(mainThread)) {
                stackTraces[mainThread] = mainThread.stackTrace
            }

            var wrapper: ThreadInfo.ThreadWrapper? = null
            for ((thread, trace) in stackTraces) {
                wrapper = ThreadInfo(getThreadTitle(thread), trace).ThreadWrapper(wrapper)
            }

            return ANRError(wrapper, duration)
        }

        @JvmStatic
        internal fun createMainOnly(duration: Long): ANRError {
            val mainThread = Looper.getMainLooper().thread
            val mainStackTrace = mainThread.stackTrace

            return ANRError(
                ThreadInfo(getThreadTitle(mainThread), mainStackTrace).ThreadWrapper(null),
                duration
            )
        }

        private fun getThreadTitle(thread: Thread): String {
            return "${thread.name} (state = ${thread.state})"
        }
    }
}
