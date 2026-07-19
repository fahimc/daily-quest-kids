# Daily Quest Kids

Daily Quest Kids is a native Android puzzle game for children aged 7-10.
The product direction is: **Five puzzles. A new quest every day.**

This repository is being built phase by phase from
[`DAILY_QUEST_KIDS_CODEX_PROMPT.md`](DAILY_QUEST_KIDS_CODEX_PROMPT.md).

## Current Status

Phase 0 and Phase 1 foundations are in progress:

- Kotlin and Jetpack Compose Android scaffold
- Modular core model, engine and validator packages
- Offline-only Android manifest with no internet permission
- Deterministic UTC season-day engine
- Initial puzzle-pack schema and validators
- Initial curriculum research and governance documentation
- CI workflow template at [`docs/CI_WORKFLOW_TEMPLATE.yml`](docs/CI_WORKFLOW_TEMPLATE.yml)

The full Season One content target remains exactly 365 days and 1,825 puzzles.
Human content review is a release blocker and is not represented as complete.

## Build

Use Android Studio JBR or any JDK 17 runtime.

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

## Test

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat ktlintCheck
.\gradlew.bat detekt
```

Instrumented, Compose UI and screenshot tests require an emulator or connected
Android device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

The live GitHub Actions workflow is currently provided as a template because
the available GitHub token does not have the `workflow` scope required to push
files under `.github/workflows`.

## Repository Layout

```text
app/                Android application and Compose shell
core-common/        Date, streak and shared domain utilities
core-model/         Serializable puzzle-pack and progress models
core-design/        Compose design tokens and reusable UI components
core-data/          Offline sample pack provider
core-testing/       Test fixtures
puzzle-engine/      Pure Kotlin puzzle rules and engines
puzzle-validator/   Pure Kotlin content validators
docs/               Architecture, testing, privacy, content and release docs
```

## Privacy

The application is designed to work fully offline, request no internet
permission, use no adverts or analytics, and store progress locally.
