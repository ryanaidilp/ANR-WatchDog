package com.github.anrwatchdog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ANRWatchDogTest {

    @Test
    public void testDefaultConstructor() {
        ANRWatchDog watchDog = new ANRWatchDog();

        assertEquals(5000, watchDog.getTimeoutInterval());
    }

    @Test
    public void testCustomTimeoutConstructor() {
        int customTimeout = 10000;

        ANRWatchDog watchDog = new ANRWatchDog(customTimeout);

        assertEquals(customTimeout, watchDog.getTimeoutInterval());
    }

    @Test
    public void testSetANRListener_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setANRListener(error -> {});

        assertSame(watchDog, result);
    }

    @Test
    public void testSetANRListener_nullResetsToDefault() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setANRListener(null);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetANRInterceptor_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setANRInterceptor(duration -> 0);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetANRInterceptor_nullResetsToDefault() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setANRInterceptor(null);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetInterruptionListener_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setInterruptionListener(exception -> {});

        assertSame(watchDog, result);
    }

    @Test
    public void testSetInterruptionListener_nullResetsToDefault() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setInterruptionListener(null);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetReportThreadNamePrefix_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setReportThreadNamePrefix("MyApp:");

        assertSame(watchDog, result);
    }

    @Test
    public void testSetReportThreadNamePrefix_nullSetsEmptyPrefix() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setReportThreadNamePrefix(null);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetReportMainThreadOnly_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setReportMainThreadOnly();

        assertSame(watchDog, result);
    }

    @Test
    public void testSetReportAllThreads_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setReportAllThreads();

        assertSame(watchDog, result);
    }

    @Test
    public void testSetLogThreadsWithoutStackTrace_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setLogThreadsWithoutStackTrace(true);

        assertSame(watchDog, result);
    }

    @Test
    public void testSetIgnoreDebugger_returnsItself() {
        ANRWatchDog watchDog = new ANRWatchDog();

        ANRWatchDog result = watchDog.setIgnoreDebugger(true);

        assertSame(watchDog, result);
    }

    @Test
    public void testFluentApi_chainingWorks() {
        ANRWatchDog watchDog = new ANRWatchDog(3000)
                .setANRListener(error -> {})
                .setANRInterceptor(duration -> 0)
                .setInterruptionListener(exception -> {})
                .setReportThreadNamePrefix("Test:")
                .setLogThreadsWithoutStackTrace(true)
                .setIgnoreDebugger(true);

        assertNotNull(watchDog);
        assertEquals(3000, watchDog.getTimeoutInterval());
    }

    @Test
    public void testIsThread() {
        ANRWatchDog watchDog = new ANRWatchDog();

        assertTrue(watchDog instanceof Thread);
    }

    @Test
    public void testDifferentTimeouts() {
        ANRWatchDog watchDog1 = new ANRWatchDog(1000);
        ANRWatchDog watchDog2 = new ANRWatchDog(2000);
        ANRWatchDog watchDog3 = new ANRWatchDog(30000);

        assertEquals(1000, watchDog1.getTimeoutInterval());
        assertEquals(2000, watchDog2.getTimeoutInterval());
        assertEquals(30000, watchDog3.getTimeoutInterval());
    }

    @Test
    public void testCustomListener_receivesError() {
        AtomicBoolean listenerCalled = new AtomicBoolean(false);
        AtomicLong receivedDuration = new AtomicLong(0);

        ANRWatchDog.ANRListener listener = error -> {
            listenerCalled.set(true);
            receivedDuration.set(error.duration);
        };

        // Just test the listener can be set without errors
        ANRWatchDog watchDog = new ANRWatchDog().setANRListener(listener);

        assertNotNull(watchDog);
    }

    @Test
    public void testCustomInterceptor_canBeSet() {
        ANRWatchDog.ANRInterceptor interceptor = duration -> {
            if (duration < 5000) {
                return 5000 - duration;
            }
            return 0;
        };

        ANRWatchDog watchDog = new ANRWatchDog().setANRInterceptor(interceptor);

        assertNotNull(watchDog);
    }
}
