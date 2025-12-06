package com.github.anrtestapp

import android.app.Application
import android.util.Log
import com.github.anrwatchdog.ANRWatchDog
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream

class ANRWatchdogTestApplication : Application() {

    val anrWatchDog = ANRWatchDog(2000)

    var duration = 4

    val silentListener = ANRWatchDog.ANRListener { error ->
        Log.e(TAG, "", error)
    }

    override fun onCreate() {
        super.onCreate()

        anrWatchDog
            .setANRListener { error ->
                Log.e(TAG, "Detected Application Not Responding!")

                // Some tools like ACRA are serializing the exception, so we must make sure the exception serializes correctly
                ObjectOutputStream(ByteArrayOutputStream()).writeObject(error)

                Log.i(TAG, "Error was successfully serialized")

                throw error
            }
            .setANRInterceptor { currentDuration ->
                val ret = duration * 1000L - currentDuration
                if (ret > 0) {
                    Log.w(TAG, "Intercepted ANR that is too short ($currentDuration ms), postponing for $ret ms.")
                }
                ret
            }

        anrWatchDog.start()
    }

    companion object {
        private const val TAG = "ANR-Watchdog-Demo"
    }
}
