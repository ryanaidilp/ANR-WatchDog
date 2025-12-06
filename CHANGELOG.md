# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.0] - Unreleased

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

- GitHub Actions CI/CD pipeline
- Dependabot for automated dependency updates
- ProGuard consumer rules for R8/ProGuard compatibility
- Unit tests for ANRError and ANRWatchDog classes
- CHANGELOG.md file
- CONTRIBUTING.md file

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
