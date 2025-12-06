package com.github.anrtestapp

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button

class MainActivity : Activity() {

    private val mutex = Any()
    private var mode = 0
    private var crash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val application = getApplication() as ANRWatchdogTestApplication

        val minAnrDurationButton = findViewById<Button>(R.id.minAnrDuration)
        minAnrDurationButton.text = "${application.duration} seconds"
        minAnrDurationButton.setOnClickListener {
            application.duration = application.duration % 6 + 2
            minAnrDurationButton.text = "${application.duration} seconds"
        }

        val reportModeButton = findViewById<Button>(R.id.reportMode)
        reportModeButton.text = "All threads"
        reportModeButton.setOnClickListener {
            mode = (mode + 1) % 3
            when (mode) {
                0 -> {
                    reportModeButton.text = "All threads"
                    application.anrWatchDog.setReportAllThreads()
                }
                1 -> {
                    reportModeButton.text = "Main thread only"
                    application.anrWatchDog.setReportMainThreadOnly()
                }
                2 -> {
                    reportModeButton.text = "Filtered"
                    application.anrWatchDog.setReportThreadNamePrefix("APP:")
                }
            }
        }

        val behaviourButton = findViewById<Button>(R.id.behaviour)
        behaviourButton.text = "Crash"
        behaviourButton.setOnClickListener {
            crash = !crash
            if (crash) {
                behaviourButton.text = "Crash"
                application.anrWatchDog.setANRListener(null)
            } else {
                behaviourButton.text = "Silent"
                application.anrWatchDog.setANRListener(application.silentListener)
            }
        }

        findViewById<Button>(R.id.threadSleep).setOnClickListener {
            sleep()
        }

        findViewById<Button>(R.id.infiniteLoop).setOnClickListener {
            infiniteLoop()
        }

        findViewById<Button>(R.id.deadlock).setOnClickListener {
            deadLock()
        }
    }

    private fun sleep() {
        try {
            Thread.sleep(8 * 1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    @Suppress("ControlFlowWithEmptyBody")
    private fun infiniteLoop() {
        var i = 0
        while (true) {
            i++
        }
    }

    private fun deadLock() {
        LockerThread().start()

        Handler(Looper.getMainLooper()).postDelayed({
            synchronized(mutex) {
                Log.e("ANR-Failed", "There should be a dead lock before this message")
            }
        }, 1000)
    }

    private inner class LockerThread : Thread("APP: Locker") {
        override fun run() {
            synchronized(mutex) {
                @Suppress("ControlFlowWithEmptyBody")
                while (true) {
                    sleep()
                }
            }
        }
    }
}
