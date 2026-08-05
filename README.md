# DVRA Treasurer's Report Generator

[![Build & Release](https://github.com/rfreedman/dvra-treasurers-report-generator/actions/workflows/release.yml/badge.svg)](https://github.com/rfreedman/dvra-treasurers-report-generator/actions/workflows/release.yml)

Compose Desktop app that turns a Quicken CSV export into a DVRA treasurer's PDF report.

## Requirements

- **JDK 21** (Temurin / Adoptium recommended)
- Gradle Wrapper (included) — uses Gradle 8.14.4, Kotlin 2.4.10, Compose Multiplatform 1.11.1

For native installers, avoid Homebrew JDKs; Compose packaging rejects them. Prefer Temurin 21.

## Building

### Compile / run

```bash
./gradlew build
./gradlew run
```

### Native distributables

Build on the target platform only:

```bash
# Debug / non-minified package
./gradlew packageDmg      # macOS
./gradlew packageMsi      # Windows
./gradlew packageDeb      # Linux

# Release package (ProGuard) — same tasks CI uses
./gradlew packageReleaseDmg
./gradlew packageReleaseMsi
./gradlew packageReleaseDeb
```

Installers land under `build/compose/binaries/`.

Pushing to `main` runs the [release workflow](.github/workflows/release.yml), which builds macOS, Windows, and Linux installers and publishes a GitHub Release.
