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
- Added a five-day preview puzzle pack wired into the app through
  `SamplePackRepository`.
- Hardened content validation for sequential day indexes, duplicate day
  categories, curriculum tags, sequential hint orders and production human
  review.
- Implemented splash, onboarding, settings, parent information and local
  DataStore preference/progress persistence.
- Added a state-driven Daily Home with card status, Daily Five progress,
  streak metrics and puzzle preview completion flow.
- Added coordinator unit tests for card state, Daily Five completion and device
  date rollback messaging.
- Verified the phase 1-5 focused non-device gate with the Gradle user home
  redirected to `D:\gradle-user-home` because the C: drive was nearly full.
- Completed the Wordly feature with a reusable engine, responsive Compose
  screen, six-row board, flexible keyboard layout, hints, autosave,
  success/failure handling, learning summary and share-safe result model.
- Expanded preview content to 20 human-reviewed Wordly fixtures.
- Added Wordly tests for repeated-letter scoring properties, invalid guesses,
  attempt exhaustion, hint order, save/restore, terminal-event acknowledgement,
  share leakage, one-to-six-attempt wins, failure resume and UI mapping.
- Added instrumented Wordly screenshot-smoke/VRT coverage targets for empty,
  partial, repeated-letter, hint, success and failure states.

## Phase 0

- Status: local non-device gates pass; device/emulator execution is blocked.

## Phase 1

- Status: preview content pack, stricter validator and JVM tests pass;
  content-inspector VRT and full season production content are still pending.

## Phases 2-5

- Status: app shell, first-launch flows, local persistence and state-driven
  Daily Home are implemented for the preview pack; device/emulator execution is
  still pending.

## Phase 6

- Status: Wordly is implemented and local non-device tests pass; device/emulator
  execution for screenshot/VRT tests is still pending.
