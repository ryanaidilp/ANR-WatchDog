package com.github.anrwatchdog

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
class ANRErrorTest {

    @Test
    fun `createMainOnly creates error with correct duration`() {
        val duration = 5000L

        val error = ANRError.createMainOnly(duration)

        assertNotNull(error)
        assertEquals(duration, error.duration)
    }

    @Test
    fun `createMainOnly message contains duration`() {
        val duration = 3000L

        val error = ANRError.createMainOnly(duration)

        assertTrue(error.message!!.contains("3000"))
        assertTrue(error.message!!.contains("Application Not Responding"))
    }

    @Test
    fun `create creates error with all threads`() {
        val duration = 5000L

        val error = ANRError.create(duration, "", false)

        assertNotNull(error)
        assertEquals(duration, error.duration)
    }

    @Test
    fun `create creates error with prefix filter`() {
        val duration = 5000L
        val prefix = "MyApp:"

        val error = ANRError.create(duration, prefix, false)

        assertNotNull(error)
        assertEquals(duration, error.duration)
    }

    @Test
    fun `create with logThreadsWithoutStackTrace`() {
        val duration = 5000L

        val error = ANRError.create(duration, "", true)

        assertNotNull(error)
        assertEquals(duration, error.duration)
    }

    @Test
    fun `error has cause`() {
        val error = ANRError.createMainOnly(5000L)

        // The cause should be the thread stack trace
        assertNotNull(error.cause)
    }

    @Test
    fun `error stack trace is empty`() {
        val error = ANRError.createMainOnly(5000L)

        // ANRError itself has empty stack trace (the cause has the actual trace)
        assertEquals(0, error.stackTrace.size)
    }

    @Test
    fun `error is serializable`() {
        val error = ANRError.createMainOnly(5000L)

        // Serialize
        val baos = ByteArrayOutputStream()
        ObjectOutputStream(baos).use { it.writeObject(error) }

        // Deserialize
        val bais = ByteArrayInputStream(baos.toByteArray())
        val deserializedError = ObjectInputStream(bais).use { it.readObject() as ANRError }

        assertNotNull(deserializedError)
        assertEquals(error.duration, deserializedError.duration)
        assertEquals(error.message, deserializedError.message)
    }

    @Test
    fun `different durations are reported correctly`() {
        val error1 = ANRError.createMainOnly(1000L)
        val error2 = ANRError.createMainOnly(10000L)

        assertEquals(1000L, error1.duration)
        assertEquals(10000L, error2.duration)
        assertTrue(error1.message!!.contains("1000"))
        assertTrue(error2.message!!.contains("10000"))
    }

    @Test
    fun `error contains ANRInfo`() {
        val error = ANRError.createMainOnly(5000L)

        assertNotNull(error.info)
        assertEquals(5000L, error.info.durationMs)
        assertNotNull(error.info.cause)
        assertNotNull(error.info.mainThread)
    }

    @Test
    fun `error toMap returns valid map`() {
        val error = ANRError.createMainOnly(5000L)

        val map = error.toMap()

        assertTrue(map.containsKey("durationMs"))
        assertTrue(map.containsKey("cause"))
        assertTrue(map.containsKey("mainThread"))
        assertEquals(5000L, map["durationMs"])
    }

    @Test
    fun `error toJsonString returns valid JSON`() {
        val error = ANRError.createMainOnly(5000L)

        val jsonString = error.toJsonString()

        // Should be parseable as JSON
        val parsed = JSONObject(jsonString)
        assertEquals(5000L, parsed.getLong("durationMs"))
        assertNotNull(parsed.getString("cause"))
    }

    @Test
    fun `error toJsonStringPretty returns formatted JSON`() {
        val error = ANRError.createMainOnly(5000L)

        val prettyJson = error.toJsonStringPretty()

        // Pretty JSON should have newlines and indentation
        assertTrue(prettyJson.contains("\n"))
        assertTrue(prettyJson.contains("  "))
    }

    @Test
    fun `error message contains cause information`() {
        val error = ANRError.createMainOnly(5000L)

        assertTrue(error.message!!.contains("CAUSE:"))
    }

    @Test
    fun `create error has correct info with all threads`() {
        val error = ANRError.create(5000L, "", false)

        assertNotNull(error.info)
        assertTrue(error.info.allThreads.isNotEmpty())
        // Main thread should always be present
        assertTrue(error.info.allThreads.any { it.isMainThread })
    }
}
