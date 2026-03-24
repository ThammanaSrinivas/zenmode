# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ZenMode is an Android minimalist launcher app designed to reduce doomscrolling. Built with Kotlin and Jetpack Compose. Licensed under GPLv3.

## Build Commands

```bash
# Debug build
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run a single test class
./gradlew :app:testDebugUnitTest --tests "com.zenlauncher.zenmode.SomeTestClass"

# Run instrumentation tests (requires device/emulator)
./gradlew :app:connectedDebugAndroidTest

# Check code coverage (minimum 80% enforced)
./gradlew :app:koverVerifyDebug

# Compile Kotlin only
./gradlew :app:compileDebugKotlin
```

## Architecture

### Composite Build / Open Core Pattern

The project uses Gradle composite builds to separate open-source code from proprietary backends:

- **`app`** — Main Android app: UI screens (Jetpack Compose), ViewModels, Activities, Services
- **`core-api`** — Public interface layer: contracts for auth, analytics, Firestore, and domain models
- **`core-mock`** — Mock implementation for open-source builds (instant auth, local analytics logging)
- **`core-private`** *(not in this repo)* — Real Firebase/PostHog integrations, lives in sibling `../zenmode_core_private/`

`settings.gradle.kts` auto-detects whether `../zenmode_core_private` exists. If absent, falls back to `core-mock`. No config changes needed.

### Service Discovery

Backend services are loaded at startup via Java `ServiceLoader` SPI. `ZenModeApp.onCreate()` discovers the `AppInitializer` implementation (from core-mock or core-private), which populates `ServiceLocator` — a singleton registry holding:
- `analyticsManager` / `analyticsTracker` — analytics abstractions
- `authProvider` — authentication
- `firestoreDataSource` — database operations

All app code accesses backends through `ServiceLocator`, never directly.

### Key App Components

- **Activities**: `MainActivity` (home launcher, singleTask), `OnboardingActivity`, `SettingsActivity`, `DelayedUnlockActivity`
- **Services**: `ZenAccessibilityService` (interaction monitoring), `DoomScrollingMonitorService` (foreground service for scroll detection)
- **ViewModels**: `MainViewModel` (home screen state, unlocks, buddy stats), `GoogleSignInViewModel`
- **UI Screens** (Compose): `HomeScreen`, `ResistenceScreen`, `WelcomeScreen`, `SettingsScreen`, `ZenBuddyConnectScreen`, plus permission setup screens

### Contributing Rule

When adding new backend functionality: define the interface in `core-api`, provide a mock in `core-mock`, so open-source builds don't break.

## Design System

Defined in `design_system.md`. Key points:

- **Brand color**: ZenMode green `#00C700`, glow `#24FF24`, dark `#007700`
- **Fonts**: Cabinet Grotesque (primary UI), Reddit Mono (numeric data — prevents layout shift), Silkscreen (secondary). Font definitions in `ui/theme/Type.kt`.
- **Layout**: 20dp global margins and gutters. Corner radius: 16dp cards, 8dp buttons, 4dp inputs.
- **Theme**: Light/dark mode via semantic tokens in `ui/theme/Theme.kt` and `ui/theme/Color.kt`
- **Font files**: Cabinet Grotesque must be downloaded from Fontshare and placed in `app/src/main/assets/fonts/`

## SDK & Tooling

- Compile/Target SDK 35, Min SDK 28, Java 17, Kotlin 2.0.0
- Compose BOM 2024.06.00, Material 3
- Kover for coverage (excludes Activities, Fragments, Adapters from 80% minimum)
- LeakCanary in debug builds
- ProGuard enabled for release (minification + resource shrinking)
