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

Status: preview content pack, stricter validator and engine JVM tests pass;
content-inspector VRT is pending.

Completed scope:

- Initial curriculum research added.
- Versioned puzzle-pack schema documented.
- Kotlin models for puzzle types, hints, progress and share cards added.
- Initial content-safety and structure validators added.
- Preview pack added for app-level phase development.
- Validator now rejects non-sequential days, duplicate categories, blank
  curriculum tags, non-sequential hint orders and missing production human
  review.

Verified on 2026-07-19:

- Serialization round trip.
- Missing category rejection.
- Duplicate ID rejection.
- Full-season count rejection.
- Sequential day-index rejection.
- Non-sequential hint rejection.
- Production human-review rejection.
- UTC date and streak behaviour.
- Wordly repeated-letter scoring.
- Share-card answer leak detection.

Pending verification:

- Content-inspector VRT on a device/emulator.

Known limitations:

- Season One content has not been human reviewed.
- Production pack generation is not complete.

## Phase 2 - Design System and Responsive Shell

Status: preview shell implemented; screenshot/VRT approval is pending a
device/emulator.

Completed scope:

- Bottom navigation shell for Home, Streaks and Settings.
- Reusable panels, progress dots, puzzle cards and category styling.
- High-contrast theme mode connected to settings.
- Instrumented screenshot-smoke target updated for the Daily Home hero panel.

Verified on 2026-07-19:

- `:app:compileDebugKotlin`
- `:app:compileDebugAndroidTestKotlin`
- Result: passed.

Known limitations:

- Responsive VRT baselines are not captured yet.
- Feature puzzle UIs are preview screens, not full gameplay.

## Phase 3 - First-Launch Flows

Status: implemented for local preview.

Completed scope:

- Splash screen.
- Three-step onboarding flow.
- Welcome-to-home transition.
- Settings screen with sound, haptics, motion, contrast, text size, timer and
  mistake-checking toggles.
- Parent information screen.
- Reset progress and reset onboarding actions.
- Preference persistence through Android DataStore.

Verified on 2026-07-19:

- `:app:testDebugUnitTest`
- `:app:compileDebugAndroidTestKotlin`
- Result: passed.

Known limitations:

- Instrumented onboarding navigation has not executed on a device/emulator.

## Phase 4 - Date and Persistence Layer

Status: lightweight local persistence implemented; Room-backed history remains
planned.

Completed scope:

- UTC season-day state feeds the app home coordinator.
- Progress persistence through Android DataStore.
- Started/completed puzzle IDs persist locally.
- Current/best Daily Five streak metrics are derived from stored completion
  days.
- Greatest observed day is stored to detect device date rollback.
- User-facing rollback message preserves progress confidence when the date
  moves earlier than the last observed day.

Verified on 2026-07-19:

- Coordinator unit tests for card state, Daily Five completion and rollback
  messaging.
- Result: passed.

Known limitations:

- Room schema, migrations, backup/restore and richer history queries are still
  pending.

## Phase 5 - State-Driven Daily Home

Status: implemented against the preview pack.

Completed scope:

- Daily Home derives all visible card state from puzzle pack data, date state
  and stored progress.
- Current, solved, started and locked card states.
- Daily Five progress, current streak, best streak, perfect days and total
  solved metrics.
- Category streak counts and hint counts shown per card.
- Puzzle preview screen marks a puzzle solved and returns home.
- Season-complete and device-date rollback messaging.

Verified on 2026-07-19:

- `:puzzle-validator:test :app:testDebugUnitTest ktlintCheck detekt :app:compileDebugAndroidTestKotlin`
- Result: passed.

Known limitations:

- Interactive gameplay for the five puzzle types starts in later phases.
- Android UI tests have compiled but not run on physical/emulated hardware.

## Phase 6 - Wordly Engine and Feature

Status: implemented; device/emulator execution for visual tests is pending.

Completed scope:

- Reusable Wordly engine added to `puzzle-engine`.
- Six-row board state, duplicate-letter scoring, keyboard state, guess
  validation, hints, save/restore, success/failure and terminal-event
  acknowledgement.
- DataStore-backed Wordly autosave keyed by puzzle id.
- Failure state is now persisted separately from completion state.
- Wordly route replaces the generic puzzle preview for Wordly puzzles.
- Responsive Compose Wordly screen inspired by the supplied design, using
  constraint-aware sizing plus flexible weighted rows for the board, controls
  and keyboard.
- Learning summary and share-safe result pattern generated without answer or
  guess leakage.
- Preview pack now includes 20 human-reviewed Wordly fixtures.

Verified on 2026-07-19:

- `:puzzle-engine:test`
- `:puzzle-validator:test`
- `:app:testDebugUnitTest`
- `:app:compileDebugAndroidTestKotlin`
- `ktlintCheck`
- `detekt`
- Result: passed.

Wordly test coverage:

- Exhaustive repeated-letter scoring properties over a small alphabet.
- Invalid guesses and too-short guesses.
- Attempt exhaustion and locked finished state.
- Hint order and hint exhaustion.
- Save/restore of attempts, current input, hints and terminal flags.
- Terminal completion acknowledgement emits once.
- Share-safe model answer/guess leak protection.
- Success paths on attempts one through six.
- Failure after process-style save/restore.
- Keyboard state promotion.
- UI mapper coverage for partial, hinted and terminal share states.
- Instrumented screenshot-smoke targets for empty, partial, repeated-letter,
  hint, success and failure visual states.

Known limitations:

- The instrumented visual tests compile but have not executed because no
  emulator or device is attached.
- Real Android screenshot baselines still need capture and approval.

## Phase 14 - Season One Candidate Content

Status: full-season candidate export and automated audit are implemented;
production release remains blocked by genuine human content review and
replacement of repeated generated candidate fingerprints.

Completed scope:

- `SeasonOneCandidateFactory` exports 365 days and 1,825 puzzle records.
- Candidate puzzle IDs are stable across all five categories for days 1-365.
- Candidate review metadata is explicitly set to `humanReviewed=false`.
- Content pipeline supports `--season-one-candidate` mode.
- Generated artifacts include:
  - `puzzle-pack/season-one-candidate.json`
  - `puzzle-pack/season-one-candidate.sha256`
  - `reports/season-one-validation.json`
  - `reports/season-one-puzzles.csv`
  - `reports/season-one-human-review.csv`
  - `reports/season-one-validation.html`
- Release audit reports per-category counts, human-review blockers and repeated
  content fingerprints.

Verified on 2026-07-20:

- `:content-tools:ktlintFormat`
- `:content-tools:ktlintCheck`
- `:content-tools:detekt`
- `:content-tools:test`
- Result: passed.

Generated report summary:

- Days: 365.
- Puzzles: 1,825.
- Category counts: 365 Wordly, 365 Spelling B, 365 Crossword, 365 Sudoku and
  365 Connections.
- Automated structural validation: passed.
- Release readiness: blocked.
- Human review required: 1,825 puzzles.
- Repeated candidate content fingerprints: 1,715.

Known limitations:

- The generated candidate pack is a release-blocking content scaffold, not a
  finished human-authored production season.
- Genuine reviewer sign-off must not be inferred from the generated candidate
  rows.
