# Phase Status

## Phase 0 - Repository and Quality Foundation

Status: local non-device gates pass; phase completion is blocked by missing
device/emulator execution for instrumented Compose and VRT checks.

Completed scope:

- Repository cloned locally.
- Native Android project scaffold added.
- Core module graph added.
- Gradle version catalog added.
- CI workflow template added at `docs/CI_WORKFLOW_TEMPLATE.yml`.
- Sample JVM, instrumented Compose and screenshot-smoke tests added.
- Offline Android manifest added with no internet permission.
- Debug APK builds successfully.
- Android test APK compiles successfully.

Verified on 2026-07-19:

- `.\gradlew.bat --no-daemon --no-parallel "-Dorg.gradle.workers.max=1" lintDebug test ktlintCheck detekt assembleDebug compileDebugAndroidTestKotlin`
- Result: passed.

Blocked verification:

- `connectedDebugAndroidTest`
- Result: failed with `No connected devices!`
- Full screenshot/VRT execution still requires an emulator or connected device.

Known limitations:

- VRT baselines are not approved yet.
- Feature puzzle UIs are foundation previews only until their feature phases.
- Live GitHub Actions workflow publication requires a GitHub token with the
  `workflow` scope.

## Phase 1 - Curriculum Research and Content Models

Status: model, validator and engine JVM tests pass; content-inspector VRT is
pending.

Completed scope:

- Initial curriculum research added.
- Versioned puzzle-pack schema documented.
- Kotlin models for puzzle types, hints, progress and share cards added.
- Initial content-safety and structure validators added.

Verified on 2026-07-19:

- Serialization round trip.
- Missing category rejection.
- Duplicate ID rejection.
- Full-season count rejection.
- UTC date and streak behaviour.
- Wordly repeated-letter scoring.
- Share-card answer leak detection.

Pending verification:

- Content-inspector VRT on a device/emulator.

Known limitations:

- Season One content has not been human reviewed.
- Production pack generation is not complete.
