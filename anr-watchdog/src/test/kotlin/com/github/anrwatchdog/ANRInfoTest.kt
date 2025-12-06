package com.github.anrwatchdog

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ANRInfoTest {

    @Test
    fun `StackTraceElementInfo from creates correct info`() {
        val element = StackTraceElement("com.example.Test", "doSomething", "Test.kt", 42)

        val info = StackTraceElementInfo.from(element)

        assertEquals("com.example.Test", info.className)
        assertEquals("doSomething", info.methodName)
        assertEquals("Test.kt", info.fileName)
        assertEquals(42, info.lineNumber)
        assertFalse(info.isNativeMethod)
    }

    @Test
    fun `StackTraceElementInfo toMap contains all fields`() {
        val info = StackTraceElementInfo(
            className = "com.example.Test",
            methodName = "doSomething",
            fileName = "Test.kt",
            lineNumber = 42,
            isNativeMethod = false
        )

        val map = info.toMap()

        assertEquals("com.example.Test", map["className"])
        assertEquals("doSomething", map["methodName"])
        assertEquals("Test.kt", map["fileName"])
        assertEquals(42, map["lineNumber"])
        assertEquals(false, map["isNativeMethod"])
    }

    @Test
    fun `StackTraceElementInfo toJson creates valid JSON`() {
        val info = StackTraceElementInfo(
            className = "com.example.Test",
            methodName = "doSomething",
            fileName = "Test.kt",
            lineNumber = 42,
            isNativeMethod = false
        )

        val json = info.toJson()

        assertEquals("com.example.Test", json.getString("className"))
        assertEquals("doSomething", json.getString("methodName"))
        assertEquals("Test.kt", json.getString("fileName"))
        assertEquals(42, json.getInt("lineNumber"))
        assertEquals(false, json.getBoolean("isNativeMethod"))
    }

    @Test
    fun `StackTraceElementInfo toString formats correctly`() {
        val info = StackTraceElementInfo(
            className = "com.example.Test",
            methodName = "doSomething",
            fileName = "Test.kt",
            lineNumber = 42,
            isNativeMethod = false
        )

        assertEquals("com.example.Test.doSomething(Test.kt:42)", info.toString())
    }

    @Test
    fun `StackTraceElementInfo toString handles native method`() {
        val info = StackTraceElementInfo(
            className = "com.example.Test",
            methodName = "nativeCall",
            fileName = null,
            lineNumber = -2,
            isNativeMethod = true
        )

        assertEquals("com.example.Test.nativeCall(Native Method)", info.toString())
    }

    @Test
    fun `LockInfo toMap contains all fields`() {
        val lockInfo = LockInfo(
            lockClassName = "java.util.concurrent.locks.ReentrantLock",
            lockIdentityHashCode = 12345,
            ownerThreadName = "Worker-1",
            ownerThreadId = 10L
        )

        val map = lockInfo.toMap()

        assertEquals("java.util.concurrent.locks.ReentrantLock", map["lockClassName"])
        assertEquals(12345, map["lockIdentityHashCode"])
        assertEquals("Worker-1", map["ownerThreadName"])
        assertEquals(10L, map["ownerThreadId"])
    }

    @Test
    fun `LockInfo toJson creates valid JSON`() {
        val lockInfo = LockInfo(
            lockClassName = "java.util.concurrent.locks.ReentrantLock",
            lockIdentityHashCode = 12345,
            ownerThreadName = "Worker-1",
            ownerThreadId = 10L
        )

        val json = lockInfo.toJson()

        assertEquals("java.util.concurrent.locks.ReentrantLock", json.getString("lockClassName"))
        assertEquals(12345, json.getInt("lockIdentityHashCode"))
        assertEquals("Worker-1", json.getString("ownerThreadName"))
        assertEquals(10L, json.getLong("ownerThreadId"))
    }

    @Test
    fun `ThreadStackInfo toMap contains all fields`() {
        val stackTrace = listOf(
            StackTraceElementInfo("com.example.Test", "method1", "Test.kt", 10, false)
        )
        val threadInfo = ThreadStackInfo(
            name = "main",
            id = 1L,
            state = Thread.State.RUNNABLE,
            isMainThread = true,
            stackTrace = stackTrace,
            lockInfo = null
        )

        val map = threadInfo.toMap()

        assertEquals("main", map["name"])
        assertEquals(1L, map["id"])
        assertEquals("RUNNABLE", map["state"])
        assertEquals(true, map["isMainThread"])
        assertNotNull(map["stackTrace"])
    }

    @Test
    fun `ThreadStackInfo toJson creates valid JSON`() {
        val stackTrace = listOf(
            StackTraceElementInfo("com.example.Test", "method1", "Test.kt", 10, false)
        )
        val threadInfo = ThreadStackInfo(
            name = "main",
            id = 1L,
            state = Thread.State.RUNNABLE,
            isMainThread = true,
            stackTrace = stackTrace,
            lockInfo = null
        )

        val json = threadInfo.toJson()

        assertEquals("main", json.getString("name"))
        assertEquals(1L, json.getLong("id"))
        assertEquals("RUNNABLE", json.getString("state"))
        assertEquals(true, json.getBoolean("isMainThread"))
        assertTrue(json.has("stackTrace"))
    }

    @Test
    fun `ANRInfo analyzeMainOnly creates correct info`() {
        val duration = 5000L

        val info = ANRInfo.analyzeMainOnly(duration)

        assertEquals(duration, info.durationMs)
        assertNotNull(info.mainThread)
        assertTrue(info.mainThread.isMainThread)
        assertNotNull(info.cause)
        assertNotNull(info.causeDescription)
        assertFalse(info.isPotentialDeadlock)
    }

    @Test
    fun `ANRInfo analyze creates correct info`() {
        val duration = 5000L

        val info = ANRInfo.analyze(duration, "", false)

        assertEquals(duration, info.durationMs)
        assertNotNull(info.mainThread)
        assertTrue(info.mainThread.isMainThread)
        assertTrue(info.allThreads.isNotEmpty())
        assertNotNull(info.cause)
    }

    @Test
    fun `ANRInfo toMap contains all required fields`() {
        val info = ANRInfo.analyzeMainOnly(5000L)

        val map = info.toMap()

        assertTrue(map.containsKey("durationMs"))
        assertTrue(map.containsKey("timestamp"))
        assertTrue(map.containsKey("cause"))
        assertTrue(map.containsKey("causeDescription"))
        assertTrue(map.containsKey("mainThread"))
        assertTrue(map.containsKey("blockingThread"))
        assertTrue(map.containsKey("allThreads"))
        assertTrue(map.containsKey("isPotentialDeadlock"))
    }

    @Test
    fun `ANRInfo toJsonString creates valid JSON`() {
        val info = ANRInfo.analyzeMainOnly(5000L)

        val jsonString = info.toJsonString()

        // Should be parseable as JSON
        val parsed = JSONObject(jsonString)
        assertEquals(5000L, parsed.getLong("durationMs"))
        assertNotNull(parsed.getString("cause"))
    }

    @Test
    fun `ANRInfo toJsonStringPretty creates formatted JSON`() {
        val info = ANRInfo.analyzeMainOnly(5000L)

        val prettyJson = info.toJsonStringPretty()

        // Pretty JSON should have newlines and indentation
        assertTrue(prettyJson.contains("\n"))
        assertTrue(prettyJson.contains("  "))
    }

    @Test
    fun `ANRInfo is serializable`() {
        val info = ANRInfo.analyzeMainOnly(5000L)

        // Serialize
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(info) }

        // Deserialize
        val bais = ByteArrayInputStream(baos.toByteArray())
        val deserialized = ObjectInputStream(bais).use { it.readObject() as ANRInfo }

        assertEquals(info.durationMs, deserialized.durationMs)
        assertEquals(info.cause, deserialized.cause)
        assertEquals(info.mainThread.name, deserialized.mainThread.name)
    }

    @Test
    fun `ANRCause enum has expected values`() {
        val causes = ANRCause.values()

        assertTrue(causes.contains(ANRCause.BLOCKED_ON_LOCK))
        assertTrue(causes.contains(ANRCause.WAITING))
        assertTrue(causes.contains(ANRCause.LONG_COMPUTATION))
        assertTrue(causes.contains(ANRCause.IO_ON_MAIN_THREAD))
        assertTrue(causes.contains(ANRCause.NETWORK_ON_MAIN_THREAD))
        assertTrue(causes.contains(ANRCause.DATABASE_ON_MAIN_THREAD))
        assertTrue(causes.contains(ANRCause.DEADLOCK))
        assertTrue(causes.contains(ANRCause.UNKNOWN))
    }

    @Test
    fun `ThreadStackInfo is serializable`() {
        val stackTrace = listOf(
            StackTraceElementInfo("com.example.Test", "method1", "Test.kt", 10, false)
        )
        val threadInfo = ThreadStackInfo(
            name = "main",
            id = 1L,
            state = Thread.State.RUNNABLE,
            isMainThread = true,
            stackTrace = stackTrace,
            lockInfo = null
        )

        // Serialize
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(threadInfo) }

        // Deserialize
        val bais = ByteArrayInputStream(baos.toByteArray())
        val deserialized = ObjectInputStream(bais).use { it.readObject() as ThreadStackInfo }

        assertEquals(threadInfo.name, deserialized.name)
        assertEquals(threadInfo.id, deserialized.id)
        assertEquals(threadInfo.state, deserialized.state)
    }

    @Test
    fun `StackTraceElementInfo is serializable`() {
        val info = StackTraceElementInfo(
            className = "com.example.Test",
            methodName = "doSomething",
            fileName = "Test.kt",
            lineNumber = 42,
            isNativeMethod = false
        )

        // Serialize
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(info) }

        // Deserialize
        val bais = ByteArrayInputStream(baos.toByteArray())
        val deserialized = ObjectInputStream(bais).use { it.readObject() as StackTraceElementInfo }

        assertEquals(info.className, deserialized.className)
        assertEquals(info.methodName, deserialized.methodName)
    }

    @Test
    fun `LockInfo is serializable`() {
        val lockInfo = LockInfo(
            lockClassName = "java.util.concurrent.locks.ReentrantLock",
            lockIdentityHashCode = 12345,
            ownerThreadName = "Worker-1",
            ownerThreadId = 10L
        )

        // Serialize
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(lockInfo) }

        // Deserialize
        val bais = ByteArrayInputStream(baos.toByteArray())
        val deserialized = ObjectInputStream(bais).use { it.readObject() as LockInfo }

        assertEquals(lockInfo.lockClassName, deserialized.lockClassName)
        assertEquals(lockInfo.ownerThreadName, deserialized.ownerThreadName)
    }
}
