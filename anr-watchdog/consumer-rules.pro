# ANR-WatchDog consumer ProGuard rules
# Keep ANRError class for proper stack trace reporting
-keep class com.github.anrwatchdog.ANRError { *; }
-keep class com.github.anrwatchdog.ANRError$* { *; }

# Keep ANRWatchDog public API
-keep class com.github.anrwatchdog.ANRWatchDog { *; }
-keep interface com.github.anrwatchdog.ANRWatchDog$* { *; }
