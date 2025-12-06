package com.github.anrwatchdog;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ANRErrorTest {

    @Test
    public void testNewMainOnly_createErrorWithCorrectDuration() {
        long duration = 5000L;

        ANRError error = ANRError.NewMainOnly(duration);

        assertNotNull(error);
        assertEquals(duration, error.duration);
    }

    @Test
    public void testNewMainOnly_messageContainsDuration() {
        long duration = 3000L;

        ANRError error = ANRError.NewMainOnly(duration);

        assertTrue(error.getMessage().contains("3000"));
        assertTrue(error.getMessage().contains("Application Not Responding"));
    }

    @Test
    public void testNew_createErrorWithAllThreads() {
        long duration = 5000L;

        ANRError error = ANRError.New(duration, "", false);

        assertNotNull(error);
        assertEquals(duration, error.duration);
    }

    @Test
    public void testNew_createErrorWithPrefixFilter() {
        long duration = 5000L;
        String prefix = "MyApp:";

        ANRError error = ANRError.New(duration, prefix, false);

        assertNotNull(error);
        assertEquals(duration, error.duration);
    }

    @Test
    public void testNew_withLogThreadsWithoutStackTrace() {
        long duration = 5000L;

        ANRError error = ANRError.New(duration, "", true);

        assertNotNull(error);
        assertEquals(duration, error.duration);
    }

    @Test
    public void testErrorHasCause() {
        ANRError error = ANRError.NewMainOnly(5000L);

        // The cause should be the thread stack trace
        assertNotNull(error.getCause());
    }

    @Test
    public void testErrorStackTraceIsEmpty() {
        ANRError error = ANRError.NewMainOnly(5000L);

        // ANRError itself has empty stack trace (the cause has the actual trace)
        assertEquals(0, error.getStackTrace().length);
    }

    @Test
    public void testSerialization() throws Exception {
        ANRError error = ANRError.NewMainOnly(5000L);

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(error);
        oos.close();

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        ANRError deserializedError = (ANRError) ois.readObject();
        ois.close();

        assertNotNull(deserializedError);
        assertEquals(error.duration, deserializedError.duration);
        assertEquals(error.getMessage(), deserializedError.getMessage());
    }

    @Test
    public void testDifferentDurations() {
        ANRError error1 = ANRError.NewMainOnly(1000L);
        ANRError error2 = ANRError.NewMainOnly(10000L);

        assertEquals(1000L, error1.duration);
        assertEquals(10000L, error2.duration);
        assertTrue(error1.getMessage().contains("1000"));
        assertTrue(error2.getMessage().contains("10000"));
    }
}
