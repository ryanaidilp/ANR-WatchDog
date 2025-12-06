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
 * @property info Structured ANR information for easy parsing and analysis.
 */
class ANRError private constructor(
    thread: ThreadInfo.ThreadWrapper?,
    @JvmField val duration: Long,
    @JvmField val info: ANRInfo
) : Error(buildMessage(duration, info), thread) {

    /**
     * Get the structured ANR information as a Map (useful for Flutter/React Native).
     */
    fun toMap(): Map<String, Any?> = info.toMap()

    /**
     * Get the structured ANR information as a JSON string.
     */
    fun toJsonString(): String = info.toJsonString()

    /**
     * Get the structured ANR information as a pretty-printed JSON string.
     */
    fun toJsonStringPretty(): String = info.toJsonStringPretty()

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

        private fun buildMessage(duration: Long, info: ANRInfo): String {
            return buildString {
                append("Application Not Responding for at least $duration ms.")
                append("\n\nCAUSE: ${info.cause.name}")
                append("\n${info.causeDescription}")

                if (info.isPotentialDeadlock) {
                    append("\n\nWARNING: Potential DEADLOCK detected!")
                }

                info.blockingThread?.let { blocking ->
                    append("\n\nBlocking thread: '${blocking.name}' (state: ${blocking.state})")
                }
            }
        }

        @JvmStatic
        internal fun create(
            duration: Long,
            prefix: String?,
            logThreadsWithoutStackTrace: Boolean
        ): ANRError {
            // Create structured info first
            val info = ANRInfo.analyze(duration, prefix, logThreadsWithoutStackTrace)

            // Build legacy thread chain for backwards compatibility
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

            return ANRError(wrapper, duration, info)
        }

        @JvmStatic
        internal fun createMainOnly(duration: Long): ANRError {
            val info = ANRInfo.analyzeMainOnly(duration)

            val mainThread = Looper.getMainLooper().thread
            val mainStackTrace = mainThread.stackTrace

            return ANRError(
                ThreadInfo(getThreadTitle(mainThread), mainStackTrace).ThreadWrapper(null),
                duration,
                info
            )
        }

        private fun getThreadTitle(thread: Thread): String {
            return "${thread.name} (state = ${thread.state})"
        }
    }
}
