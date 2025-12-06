# Changelog

## [2.0.1] - 2025-12-06

### Changes

#### Bug Fixes
- fix: make signing conditional for JitPack compatibility

## [2.0.0] - 2025-12-7

### Changed

- **BREAKING:** Minimum SDK raised from 14 to 16
- Updated target SDK from 28 to 34 (Android 14)
- Updated compile SDK from 29 to 34
- Updated Java source compatibility from 1.6 to 11
- Updated AndroidX Annotation from 1.1.0 to 1.7.1
- Migrated from deprecated `maven` plugin to `maven-publish`
- Modernized Gradle wrapper from 4.10.1 to 8.4
- Updated Android Gradle Plugin from 3.3.2 to 8.2.2

### Added

- **Structured ANR information** via new `ANRInfo` class
  - `ANRCause` enum to identify the actual cause of ANR (BLOCKED_ON_LOCK, NETWORK_ON_MAIN_THREAD, IO_ON_MAIN_THREAD, etc.)
  - `toMap()` method for easy data extraction (useful for Flutter/React Native plugins)
  - `toJsonString()` and `toJsonStringPretty()` methods for JSON serialization
  - Detailed thread stack trace information with `ThreadStackInfo` class
  - Potential deadlock detection
- GitHub Actions CI/CD pipeline
- Dependabot for automated dependency updates
- ProGuard consumer rules for R8/ProGuard compatibility
- Unit tests for ANRError, ANRWatchDog, and ANRInfo classes
- CHANGELOG.md file
- CONTRIBUTING.md file
- Kotlin support - library fully migrated to Kotlin while maintaining Java compatibility

### Removed

- Removed deprecated `jcenter()` repository (shut down)

### Fixed

- Added `android:exported` attribute to test app activity (Android 12+ requirement)
- Added namespace to build.gradle (AGP 8.x requirement)

## [1.4.0] - 2020

### Added

- ANR duration field in ANRError
- ANRInterceptor for custom ANR handling logic

### Changed

- Migrated to AndroidX

## [1.3.0] - 2017

### Added

- Thread name prefix filtering
- Option to log threads without stack traces

## [1.2.0] - 2016

### Added

- Debugger detection to avoid false positives during debugging
- InterruptionListener interface

## [1.1.0] - 2016

### Added

- Custom ANRListener support
- Fluent API for configuration

## [1.0.0] - 2016

### Added

- Initial release
- Basic ANR detection via UI thread monitoring
- Stack trace collection for all threads
