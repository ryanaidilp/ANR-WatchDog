# Contributing to ANR-WatchDog

Thank you for your interest in contributing to ANR-WatchDog! This document provides guidelines and information for contributors.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Create a feature branch from `master`

## Development Setup

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 17 or higher
- Android SDK with API 34

### Building the Project

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

## Making Changes

### Code Style

- Follow existing code conventions in the project
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Keep methods focused and concise

### Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/) style:

- `feat:` - New features
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `test:` - Adding or updating tests
- `build:` - Build system or dependency changes
- `ci:` - CI/CD changes
- `refactor:` - Code refactoring
- `chore:` - Other changes

Examples:
```
feat: add ANR duration threshold configuration
fix: prevent false positives during orientation change
docs: update README with new configuration options
```

## Pull Request Process

1. Ensure all tests pass locally
2. Update documentation if needed
3. Add tests for new functionality
4. Create a pull request with a clear description
5. Wait for review and address any feedback

### PR Title Format

Use the same conventional commit format for PR titles:
```
feat: add new feature description
```

## Reporting Issues

When reporting bugs, please include:

- Android version and device
- ANR-WatchDog version
- Minimal reproduction steps
- Expected vs actual behavior
- Stack traces if applicable

## Questions?

Feel free to open an issue for questions or discussions about the project.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
