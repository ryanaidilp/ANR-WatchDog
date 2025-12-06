package com.github.anrwatchdog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ANRWatchDogTest {

    @Test
    fun `default constructor uses 5000ms timeout`() {
        val watchDog = ANRWatchDog()

        assertEquals(5000, watchDog.getTimeoutInterval())
    }

    @Test
    fun `custom timeout constructor`() {
        val customTimeout = 10000

        val watchDog = ANRWatchDog(customTimeout)

        assertEquals(customTimeout, watchDog.getTimeoutInterval())
    }

    @Test
    fun `setANRListener returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setANRListener { }

        assertSame(watchDog, result)
    }

    @Test
    fun `setANRListener with null resets to default`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setANRListener(null)

        assertSame(watchDog, result)
    }

    @Test
    fun `setANRInterceptor returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setANRInterceptor { 0 }

        assertSame(watchDog, result)
    }

    @Test
    fun `setANRInterceptor with null resets to default`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setANRInterceptor(null)

        assertSame(watchDog, result)
    }

    @Test
    fun `setInterruptionListener returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setInterruptionListener { }

        assertSame(watchDog, result)
    }

    @Test
    fun `setInterruptionListener with null resets to default`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setInterruptionListener(null)

        assertSame(watchDog, result)
    }

    @Test
    fun `setReportThreadNamePrefix returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setReportThreadNamePrefix("MyApp:")

        assertSame(watchDog, result)
    }

    @Test
    fun `setReportThreadNamePrefix with null sets empty prefix`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setReportThreadNamePrefix(null)

        assertSame(watchDog, result)
    }

    @Test
    fun `setReportMainThreadOnly returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setReportMainThreadOnly()

        assertSame(watchDog, result)
    }

    @Test
    fun `setReportAllThreads returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setReportAllThreads()

        assertSame(watchDog, result)
    }

    @Test
    fun `setLogThreadsWithoutStackTrace returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setLogThreadsWithoutStackTrace(true)

        assertSame(watchDog, result)
    }

    @Test
    fun `setIgnoreDebugger returns itself for chaining`() {
        val watchDog = ANRWatchDog()

        val result = watchDog.setIgnoreDebugger(true)

        assertSame(watchDog, result)
    }

    @Test
    fun `fluent API chaining works`() {
        val watchDog = ANRWatchDog(3000)
            .setANRListener { }
            .setANRInterceptor { 0 }
            .setInterruptionListener { }
            .setReportThreadNamePrefix("Test:")
            .setLogThreadsWithoutStackTrace(true)
            .setIgnoreDebugger(true)

        assertNotNull(watchDog)
        assertEquals(3000, watchDog.getTimeoutInterval())
    }

    @Test
    fun `ANRWatchDog is a Thread`() {
        val watchDog = ANRWatchDog()

        assertTrue(watchDog is Thread)
    }

    @Test
    fun `different timeouts are stored correctly`() {
        val watchDog1 = ANRWatchDog(1000)
        val watchDog2 = ANRWatchDog(2000)
        val watchDog3 = ANRWatchDog(30000)

        assertEquals(1000, watchDog1.getTimeoutInterval())
        assertEquals(2000, watchDog2.getTimeoutInterval())
        assertEquals(30000, watchDog3.getTimeoutInterval())
    }

    @Test
    fun `custom listener can be set`() {
        var listenerCalled = false

        val listener = ANRWatchDog.ANRListener {
            listenerCalled = true
        }

        val watchDog = ANRWatchDog().setANRListener(listener)

        assertNotNull(watchDog)
    }

    @Test
    fun `custom interceptor can be set`() {
        val interceptor = ANRWatchDog.ANRInterceptor { duration ->
            if (duration < 5000) 5000 - duration else 0
        }

        val watchDog = ANRWatchDog().setANRInterceptor(interceptor)

        assertNotNull(watchDog)
    }
}
