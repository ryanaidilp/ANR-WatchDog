[![Maven Central](https://img.shields.io/maven-central/v/com.github.anrwatchdog/anrwatchdog.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.anrwatchdog%22)
[![CI](https://github.com/ryanaidilp/ANR-WatchDog/actions/workflows/ci.yml/badge.svg)](https://github.com/ryanaidilp/ANR-WatchDog/actions/workflows/ci.yml)
[![MIT License](https://img.shields.io/github/license/salomonbrys/ANR-WatchDog.svg)](https://github.com/SalomonBrys/ANR-WatchDog/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/SalomonBrys/ANR-WatchDog.svg)](https://github.com/SalomonBrys/ANR-WatchDog/issues)


ANR-WatchDog
============

A simple watchdog that detects Android ANRs (Application Not Responding).


Table of contents
-----------------

  * [ANR-WatchDog](#anr-watchdog)
    * [Table of contents](#table-of-contents)
    * [Why it exists](#why-it-exists)
    * [What it does](#what-it-does)
    * [Can it work with crash reporters?](#can-it-work-with-crash-reporters)
    * [How it works](#how-it-works)
  * [Usage](#usage)
    * [Install](#install)
      * [Requirements](#requirements)
      * [With Gradle / Android Studio](#with-gradle--android-studio)
      * [Migration from 1.x to 2.x](#migration-from-1x-to-2x)
    * [Reading the ANRError exception report](#reading-the-anrerror-exception-report)
    * [Structured ANR Data](#structured-anr-data)
      * [ANR Causes](#anr-causes)
      * [Cross-Platform Integration (Flutter/React Native)](#cross-platform-integration-flutterreact-native)
    * [Configuration](#configuration)
      * [Timeout (minimum hanging time for an ANR)](#timeout-minimum-hanging-time-for-an-anr)
      * [Debugger](#debugger)
      * [On ANR callback](#on-anr-callback)
      * [Filtering reports](#filtering-reports)
      * [Watchdog thread](#watchdog-thread)
  * [Donate](#donate)


Why it exists
-------------

There is currently no way for an android application to catch and report ANR errors.  
If your application is not in the play store (either because you are still developing it or because you are distributing it differently), the only way to investigate an ANR is to pull the file /data/anr/traces.txt.  
Additionally, we found that using the Play Store was not as effective as being able to choose our own bug tracking service.

There is an [issue entry](https://code.google.com/p/android/issues/detail?id=35380) in the android bug tracker describing this lack, feel free to star it ;)


What it does
------------

It sets up a "watchdog" timer that will detect when the UI thread stops responding. When it does, it raises an error with all threads stack traces (main first).


Can it work with crash reporters?
---------------------------------

Yes! I'm glad you asked: That's the reason why it was developed in the first place!  
As this throws an error, a crash handler can intercept it and handle it the way it needs.

Known working crash reporters include:

 * [ACRA](https://github.com/ACRA/acra)
 * [Crashlytics](https://get.fabric.io/crashlytics) ([Only with `setReportMainThreadOnly()`](https://github.com/SalomonBrys/ANR-WatchDog/issues/29))
 * [HockeyApp](https://hockeyapp.net/)
 * [Bugsnag](https://www.bugsnag.com/)

And there is no reason why it should not work with *[insert your favourite crash reporting system here]*.


How it works
------------

The watchdog is a simple thread that does the following in a loop:

1.  Schedules a runnable to be run on the UI thread as soon as possible.
2.  Wait for 5 seconds. (5 seconds is the default, but it can be configured).
3.  See if the runnable has been run. If it has, go back to 1.
4.  If the runnable has not been run, which means that the UI thread has been blocked for at least 5 seconds, it raises an error with all running threads stack traces.


Usage
=====

Install
-------

### Requirements

- **Minimum SDK:** 16 (Android 4.1)
- **Java:** 11 or higher

### With Gradle / Android Studio

#### Option 1: JitPack (Recommended)

1.  Add the JitPack repository to your `settings.gradle.kts`:

    ```kotlin
    dependencyResolutionManagement {
        repositories {
            maven { url = uri("https://jitpack.io") }
        }
    }
    ```

2.  Add the dependency to your `app/build.gradle.kts`:

    ```kotlin
    dependencies {
        implementation("com.github.ryanaidilp:ANR-WatchDog:v2.0.0")
    }
    ```

#### Option 2: Maven Central

1.  In the `app/build.gradle.kts` file, add:

    ```kotlin
    dependencies {
        implementation("com.github.anrwatchdog:anrwatchdog:2.0.0")
    }
    ```

2.  In your application class, in `onCreate`, add:

    ```java
    new ANRWatchDog().start();
    ```

### Migration from 1.x to 2.x

If upgrading from version 1.x:

- **minSdk** increased from 14 to 16. If your app supports Android 4.0 (API 14-15), you'll need to raise your minSdk or stay on version 1.4.0.
- No API changes - existing code should work without modifications.


Reading the ANRError exception report
-------------------------------------

The `ANRError` stack trace is a bit particular, it has the stack traces of all the threads running in your application. So, in the report, **each `caused by` section is not the cause of the precedent exception**, but the stack trace of a different thread.

Here is a dead lock example:

```
FATAL EXCEPTION: |ANR-WatchDog|
    Process: anrwatchdog.github.com.testapp, PID: 26737
    com.github.anrwatchdog.ANRError: Application Not Responding
    Caused by: com.github.anrwatchdog.ANRError$_$_Thread: main (state = WAITING)
        at testapp.MainActivity$1.run(MainActivity.java:46)
        at android.os.Handler.handleCallback(Handler.java:739)
        at android.os.Handler.dispatchMessage(Handler.java:95)
        at android.os.Looper.loop(Looper.java:135)
        at android.app.ActivityThread.main(ActivityThread.java:5221)
    Caused by: com.github.anrwatchdog.ANRError$_$_Thread: APP: Locker (state = TIMED_WAITING)
        at java.lang.Thread.sleep(Native Method)
        at java.lang.Thread.sleep(Thread.java:1031)
        at java.lang.Thread.sleep(Thread.java:985)
        at testapp.MainActivity.SleepAMinute(MainActivity.java:18)
        at testapp.MainActivity.access$100(MainActivity.java:12)
        at testapp.MainActivity$LockerThread.run(MainActivity.java:36)
```

From this report, we can see that the stack traces of two threads. The first (the "main" thread) is stuck at `MainActivity.java:46` while the second thread (named "App: Locker") is locked in a Sleep at `MainActivity.java:18`.  
From there, if we looked at those two lines, we would surely understand the cause of the dead lock!

Note that some crash reporting library (such as Crashlytics) report all thread stack traces at the time of an uncaught exception. In that case, having all threads in the same exception can be cumbersome. In such cases, simply use `setReportMainThreadOnly()`.


Structured ANR Data
-------------------

Starting with version 2.0, ANR-WatchDog provides structured information about the ANR, including the detected cause. This makes it easier to analyze ANRs programmatically and integrate with cross-platform frameworks.

### ANR Causes

The `ANRError` now includes an `info` property of type `ANRInfo` that provides structured data about the ANR:

```kotlin
new ANRWatchDog().setANRListener { error ->
    // Access structured ANR information
    val cause = error.info.cause           // ANRCause enum
    val description = error.info.causeDescription  // Human-readable description
    val mainThread = error.info.mainThread // Main thread stack trace
    val isDeadlock = error.info.isPotentialDeadlock

    Log.e("ANR", "Cause: $cause - $description")
}
```

The `ANRCause` enum identifies the type of ANR:

| Cause | Description |
|-------|-------------|
| `BLOCKED_ON_LOCK` | Main thread is blocked waiting for a lock held by another thread |
| `WAITING` | Main thread is in a waiting state (Object.wait, Thread.join, etc.) |
| `LONG_COMPUTATION` | Main thread appears busy with a long computation |
| `IO_ON_MAIN_THREAD` | File I/O operation detected on main thread |
| `NETWORK_ON_MAIN_THREAD` | Network operation detected on main thread |
| `DATABASE_ON_MAIN_THREAD` | Database operation detected on main thread |
| `DEADLOCK` | Potential deadlock detected between threads |
| `UNKNOWN` | Unable to determine the specific cause |

### Cross-Platform Integration (Flutter/React Native)

For cross-platform frameworks, `ANRError` provides methods to export data in easily parseable formats:

```kotlin
// Get as Map (useful for Flutter MethodChannel)
val anrMap: Map<String, Any?> = error.toMap()

// Get as JSON string
val jsonString: String = error.toJsonString()

// Get as pretty-printed JSON
val prettyJson: String = error.toJsonStringPretty()
```

Example JSON output:

```json
{
  "durationMs": 5000,
  "timestamp": 1699123456789,
  "cause": "NETWORK_ON_MAIN_THREAD",
  "causeDescription": "Network operation detected on main thread at: java.net.Socket.connect",
  "isPotentialDeadlock": false,
  "mainThread": {
    "name": "main",
    "id": 1,
    "state": "RUNNABLE",
    "isMainThread": true,
    "stackTrace": [...]
  },
  "blockingThread": null,
  "allThreads": [...]
}
```

This structured data makes it easy to:

* Filter and categorize ANRs by cause in your analytics
* Create Flutter/React Native plugins that expose ANR data to Dart/JavaScript
* Build custom dashboards and alerting based on ANR types


Configuration
-------------

### Timeout (minimum hanging time for an ANR)

To set a different timeout (5000 millis is the default):

```java
if (BuildConfig.DEBUG == false) {
  new ANRWatchDog(10000 /*timeout*/).start();
}
```


### Debugger

By default, the watchdog will ignore ANRs if the debugger is attached or if the app is waiting for the debugger to attach. This is because it detects execution pauses and breakpoints as ANRs.
To disable this and throw an `ANRError` even if the debugger is connected, you can add `setIgnoreDebugger(true)`:

```java
new ANRWatchDog().setIgnoreDebugger(true).start();
```


### On ANR callback

If you would prefer not to crash the application when an ANR is detected, you can enable a callback instead:

```java
new ANRWatchDog().setANRListener(new ANRWatchDog.ANRListener() {
    @Override
    public void onAppNotResponding(ANRError error) {
        // Handle the error. For example, log it to HockeyApp:
        ExceptionHandler.saveException(error, new CrashManager());
    }
}).start();
```

**This is very important when delivering your app in production.**
When in the hand of the final user, it's *probably better* not to crash after 5 seconds, but simply report the ANR to whatever reporting system you use.
Maybe, after some more seconds, the app will "de-freeze".


### Filtering reports

If you would like to have only your own threads to be reported in the ANRError, and not all threads (including system threads such as the `FinalizerDaemon` thread), you can set a prefix: only the threads whose name starts with this prefix will be reported.

```java
new ANRWatchDog().setReportThreadNamePrefix("APP:").start();
```

Then, when you start a thread, don't forget to set its name to something that starts with this prefix (if you want it to be reported):

```java
public class MyAmazingThread extends Thread {
    @Override
    public void run() {
        setName("APP: Amazing!");
        /* ... do amazing things ... */
    }
}
```

If you want to have only the main thread stack trace and not all the other threads, you can:

```java
new ANRWatchDog().setReportMainThreadOnly().start();
```


### ANR Interceptor

Sometimes, you want to know that the application has froze for a certain duration, but not report the ANR error just yet.
You can define an interceptor that will be called before reporting an error.
The role of the interceptor is to define whether or not, given the given freeze duration, an ANR error should be raised or postponed.

```java
new ANRWatchDog(2000).setANRInterceptor(new ANRWatchDog.ANRInterceptor() {
    @Override
    public long intercept(long duration) {
        long ret = 5000 - duration;
        if (ret > 0) {
            Log.w(TAG, "Intercepted ANR that is too short (" + duration + " ms), postponing for " + ret + " ms.");
        }
        return ret;
    }
})
```

In this example, the ANRWatchDog starts with a timeout of 2000 ms, but the interceptor will postpone the error until at least 5000 ms of freeze has been reached.


### Watchdog thread

ANRWatchDog is a thread, so you can interrupt it at any time.

If you are programming with Android's multi process capability (like starting an activity in a new process), remember that you will need an ANRWatchDog thread per process.


Donate
======

ANR-Watchdog is free to use for both non-profit and commercial use and always will be.

If you wish to show some support or appreciation to my work, you are free to **[donate](https://donorbox.org/donation-salomonbrys)**!

This would be (of course) greatly appreciated but is by no means necessary to receive help or support, which I'll be happy to provide for free :)
