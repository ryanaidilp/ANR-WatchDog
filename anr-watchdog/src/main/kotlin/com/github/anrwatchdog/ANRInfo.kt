package com.github.anrwatchdog

import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/**
 * Represents the cause/type of ANR detected.
 */
enum class ANRCause {
    /** Main thread is blocked waiting for a lock held by another thread */
    BLOCKED_ON_LOCK,

    /** Main thread is waiting (Object.wait, Thread.join, etc.) */
    WAITING,

    /** Main thread appears to be in an infinite loop or long computation */
    LONG_COMPUTATION,

    /** Main thread is performing I/O operations */
    IO_ON_MAIN_THREAD,

    /** Main thread is performing network operations */
    NETWORK_ON_MAIN_THREAD,

    /** Main thread is performing database operations */
    DATABASE_ON_MAIN_THREAD,

    /** Potential deadlock detected between threads */
    DEADLOCK,

    /** Unable to determine specific cause */
    UNKNOWN
}

/**
 * Structured information about a thread's state during ANR.
 */
data class ThreadStackInfo(
    val name: String,
    val id: Long,
    val state: Thread.State,
    val isMainThread: Boolean,
    val stackTrace: List<StackTraceElementInfo>,
    val lockInfo: LockInfo? = null
) : Serializable {

    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "id" to id,
        "state" to state.name,
        "isMainThread" to isMainThread,
        "stackTrace" to stackTrace.map { it.toMap() },
        "lockInfo" to lockInfo?.toMap()
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("id", id)
        put("state", state.name)
        put("isMainThread", isMainThread)
        put("stackTrace", JSONArray(stackTrace.map { it.toJson() }))
        lockInfo?.let { put("lockInfo", it.toJson()) }
    }

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Structured stack trace element information.
 */
data class StackTraceElementInfo(
    val className: String,
    val methodName: String,
    val fileName: String?,
    val lineNumber: Int,
    val isNativeMethod: Boolean
) : Serializable {

    fun toMap(): Map<String, Any?> = mapOf(
        "className" to className,
        "methodName" to methodName,
        "fileName" to fileName,
        "lineNumber" to lineNumber,
        "isNativeMethod" to isNativeMethod
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("className", className)
        put("methodName", methodName)
        put("fileName", fileName ?: JSONObject.NULL)
        put("lineNumber", lineNumber)
        put("isNativeMethod", isNativeMethod)
    }

    override fun toString(): String {
        val location = when {
            isNativeMethod -> "(Native Method)"
            fileName != null && lineNumber >= 0 -> "($fileName:$lineNumber)"
            fileName != null -> "($fileName)"
            else -> "(Unknown Source)"
        }
        return "$className.$methodName$location"
    }

    companion object {
        private const val serialVersionUID = 1L

        fun from(element: StackTraceElement) = StackTraceElementInfo(
            className = element.className,
            methodName = element.methodName,
            fileName = element.fileName,
            lineNumber = element.lineNumber,
            isNativeMethod = element.isNativeMethod
        )
    }
}

/**
 * Information about a lock that a thread is waiting for or holding.
 */
data class LockInfo(
    val lockClassName: String?,
    val lockIdentityHashCode: Int?,
    val ownerThreadName: String?,
    val ownerThreadId: Long?
) : Serializable {

    fun toMap(): Map<String, Any?> = mapOf(
        "lockClassName" to lockClassName,
        "lockIdentityHashCode" to lockIdentityHashCode,
        "ownerThreadName" to ownerThreadName,
        "ownerThreadId" to ownerThreadId
    )

    fun toJson(): JSONObject = JSONObject().apply {
        put("lockClassName", lockClassName ?: JSONObject.NULL)
        put("lockIdentityHashCode", lockIdentityHashCode ?: JSONObject.NULL)
        put("ownerThreadName", ownerThreadName ?: JSONObject.NULL)
        put("ownerThreadId", ownerThreadId ?: JSONObject.NULL)
    }

    private companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * Comprehensive, structured information about an ANR event.
 * Designed to be easily parseable for cross-platform integrations (Flutter, React Native, etc.)
 */
data class ANRInfo(
    /** Duration in milliseconds for which the main thread was blocked */
    val durationMs: Long,

    /** Timestamp when the ANR was detected (Unix timestamp in ms) */
    val timestamp: Long,

    /** The detected cause of the ANR */
    val cause: ANRCause,

    /** Human-readable description of the ANR cause */
    val causeDescription: String,

    /** Main thread information */
    val mainThread: ThreadStackInfo,

    /** Thread that is blocking the main thread (if applicable) */
    val blockingThread: ThreadStackInfo?,

    /** All captured threads (when reporting all threads) */
    val allThreads: List<ThreadStackInfo>,

    /** Whether this might be a deadlock situation */
    val isPotentialDeadlock: Boolean
) : Serializable {

    /**
     * Convert to a Map for easy serialization (e.g., for Flutter MethodChannel).
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "durationMs" to durationMs,
        "timestamp" to timestamp,
        "cause" to cause.name,
        "causeDescription" to causeDescription,
        "mainThread" to mainThread.toMap(),
        "blockingThread" to blockingThread?.toMap(),
        "allThreads" to allThreads.map { it.toMap() },
        "isPotentialDeadlock" to isPotentialDeadlock
    )

    /**
     * Convert to JSON string for easy parsing.
     */
    fun toJson(): JSONObject = JSONObject().apply {
        put("durationMs", durationMs)
        put("timestamp", timestamp)
        put("cause", cause.name)
        put("causeDescription", causeDescription)
        put("mainThread", mainThread.toJson())
        put("blockingThread", blockingThread?.toJson() ?: JSONObject.NULL)
        put("allThreads", JSONArray(allThreads.map { it.toJson() }))
        put("isPotentialDeadlock", isPotentialDeadlock)
    }

    /**
     * Convert to JSON string.
     */
    fun toJsonString(): String = toJson().toString()

    /**
     * Convert to pretty-printed JSON string.
     */
    fun toJsonStringPretty(): String = toJson().toString(2)

    companion object {
        private const val serialVersionUID = 1L

        // Common patterns that indicate specific ANR causes
        private val IO_PATTERNS = listOf(
            "java.io.", "java.nio.", "android.os.FileUtils",
            "java.util.zip.", "android.content.res.AssetManager"
        )

        private val NETWORK_PATTERNS = listOf(
            "java.net.", "javax.net.", "okhttp3.", "retrofit2.",
            "com.android.okhttp.", "android.net."
        )

        private val DATABASE_PATTERNS = listOf(
            "android.database.", "androidx.sqlite.", "androidx.room.",
            "net.sqlcipher.", "io.realm.", "org.greenrobot."
        )

        /**
         * Analyze threads and create structured ANR information.
         */
        @JvmStatic
        fun analyze(
            duration: Long,
            prefix: String?,
            logThreadsWithoutStackTrace: Boolean
        ): ANRInfo {
            val mainThread = Looper.getMainLooper().thread
            val allStackTraces = Thread.getAllStackTraces()

            // Ensure main thread is captured
            val mainStackTrace = allStackTraces[mainThread] ?: mainThread.stackTrace

            // Analyze the main thread
            val mainThreadInfo = createThreadInfo(mainThread, mainStackTrace, true)

            // Find threads matching the prefix filter
            val relevantThreads = mutableListOf<ThreadStackInfo>()
            var blockingThread: ThreadStackInfo? = null

            allStackTraces.forEach { (thread, trace) ->
                if (thread === mainThread) {
                    relevantThreads.add(mainThreadInfo)
                } else if (prefix == null ||
                    (thread.name.startsWith(prefix) && (logThreadsWithoutStackTrace || trace.isNotEmpty()))) {
                    val threadInfo = createThreadInfo(thread, trace, false)
                    relevantThreads.add(threadInfo)

                    // Check if this thread might be blocking the main thread
                    if (blockingThread == null && isBlockingMainThread(mainThread, thread, mainStackTrace, trace)) {
                        blockingThread = threadInfo
                    }
                }
            }

            // Analyze the cause
            val (cause, description) = analyzeCause(mainThread, mainStackTrace, blockingThread, relevantThreads)

            // Check for potential deadlock
            val isPotentialDeadlock = detectPotentialDeadlock(mainThread, relevantThreads)

            return ANRInfo(
                durationMs = duration,
                timestamp = System.currentTimeMillis(),
                cause = cause,
                causeDescription = description,
                mainThread = mainThreadInfo,
                blockingThread = blockingThread,
                allThreads = relevantThreads.sortedWith(
                    compareByDescending<ThreadStackInfo> { it.isMainThread }
                        .thenBy { it.name }
                ),
                isPotentialDeadlock = isPotentialDeadlock
            )
        }

        /**
         * Analyze for main thread only.
         */
        @JvmStatic
        fun analyzeMainOnly(duration: Long): ANRInfo {
            val mainThread = Looper.getMainLooper().thread
            val mainStackTrace = mainThread.stackTrace
            val mainThreadInfo = createThreadInfo(mainThread, mainStackTrace, true)

            val (cause, description) = analyzeCause(mainThread, mainStackTrace, null, listOf(mainThreadInfo))

            return ANRInfo(
                durationMs = duration,
                timestamp = System.currentTimeMillis(),
                cause = cause,
                causeDescription = description,
                mainThread = mainThreadInfo,
                blockingThread = null,
                allThreads = listOf(mainThreadInfo),
                isPotentialDeadlock = false
            )
        }

        private fun createThreadInfo(
            thread: Thread,
            stackTrace: Array<StackTraceElement>,
            isMainThread: Boolean
        ): ThreadStackInfo {
            return ThreadStackInfo(
                name = thread.name,
                id = thread.id,
                state = thread.state,
                isMainThread = isMainThread,
                stackTrace = stackTrace.map { StackTraceElementInfo.from(it) },
                lockInfo = null // Lock info requires ThreadMXBean which may not be available
            )
        }

        private fun analyzeCause(
            mainThread: Thread,
            mainStackTrace: Array<StackTraceElement>,
            blockingThread: ThreadStackInfo?,
            @Suppress("UNUSED_PARAMETER") allThreads: List<ThreadStackInfo>
        ): Pair<ANRCause, String> {
            val state = mainThread.state

            // Check thread state first
            when (state) {
                Thread.State.BLOCKED -> {
                    return if (blockingThread != null) {
                        ANRCause.BLOCKED_ON_LOCK to
                            "Main thread is BLOCKED waiting for a lock. Blocking thread: '${blockingThread.name}'"
                    } else {
                        ANRCause.BLOCKED_ON_LOCK to
                            "Main thread is BLOCKED waiting for a lock held by another thread"
                    }
                }
                Thread.State.WAITING, Thread.State.TIMED_WAITING -> {
                    return ANRCause.WAITING to
                        "Main thread is ${state.name} - possibly waiting for a resource or callback"
                }
                else -> { /* Continue analysis */ }
            }

            // Analyze stack trace for common patterns
            for (element in mainStackTrace) {
                val fullName = "${element.className}.${element.methodName}"

                // Check for network operations
                if (NETWORK_PATTERNS.any { fullName.contains(it) }) {
                    return ANRCause.NETWORK_ON_MAIN_THREAD to
                        "Network operation detected on main thread at: ${element.className}.${element.methodName}"
                }

                // Check for database operations
                if (DATABASE_PATTERNS.any { fullName.contains(it) }) {
                    return ANRCause.DATABASE_ON_MAIN_THREAD to
                        "Database operation detected on main thread at: ${element.className}.${element.methodName}"
                }

                // Check for I/O operations
                if (IO_PATTERNS.any { fullName.contains(it) }) {
                    return ANRCause.IO_ON_MAIN_THREAD to
                        "I/O operation detected on main thread at: ${element.className}.${element.methodName}"
                }
            }

            // If main thread is RUNNABLE and no blocking operations found, likely a long computation
            if (state == Thread.State.RUNNABLE) {
                val topFrame = mainStackTrace.firstOrNull()
                return ANRCause.LONG_COMPUTATION to
                    "Main thread appears busy with computation at: ${topFrame?.let { "${it.className}.${it.methodName}" } ?: "unknown location"}"
            }

            return ANRCause.UNKNOWN to
                "Main thread is ${state.name} - unable to determine specific cause"
        }

        @Suppress("UNUSED_PARAMETER")
        private fun isBlockingMainThread(
            mainThread: Thread,
            otherThread: Thread,
            mainStackTrace: Array<StackTraceElement>,
            otherStackTrace: Array<StackTraceElement>
        ): Boolean {
            // Simple heuristic: if main thread is BLOCKED and other thread is holding a lock
            if (mainThread.state == Thread.State.BLOCKED) {
                // Check if the other thread might be holding the lock
                // This is a simplified check - full lock analysis requires ThreadMXBean
                return otherThread.state == Thread.State.RUNNABLE ||
                       otherThread.state == Thread.State.TIMED_WAITING
            }
            return false
        }

        @Suppress("UNUSED_PARAMETER")
        private fun detectPotentialDeadlock(
            mainThread: Thread,
            threads: List<ThreadStackInfo>
        ): Boolean {
            // Simple deadlock detection: multiple threads in BLOCKED state
            val blockedCount = threads.count { it.state == Thread.State.BLOCKED }
            return blockedCount >= 2
        }
    }
}
