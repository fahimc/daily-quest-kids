# Changelog

## Unreleased

- Cloned the starter repository and replaced the placeholder README with a
  native Android project baseline.
- Added Gradle version catalog, Android app module, core domain modules,
  offline manifest and initial Compose shell.
- Added deterministic UTC season-day logic, streak updates, share-safety
  models, puzzle-pack models and first-pass validators.
- Added required governance documentation and a CI workflow template.
- Updated the Android build stack to Gradle 9.6.1, AGP 9.3.0,
  Kotlin 2.4.10, Compose BOM 2026.06.01 and compile/target SDK 37.
- Verified local lint, JVM tests, Ktlint, Detekt, debug APK assembly and
  Android test APK compilation.
- Documented that publishing the live GitHub Actions workflow requires a token
  with the `workflow` scope.

## Phase 0

- Status: local non-device gates pass; device/emulator execution is blocked.

## Phase 1

- Status: model, validator and engine JVM tests pass; content-inspector VRT is
  still pending.
