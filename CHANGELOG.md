# Changelog

## Unreleased

- Expanded individual puzzle sharing so native shares include spoiler-safe
  result details and the share image renders Wordly-style colour rows without
  exposing answers, guesses, clues or groups.
- Fixed the completed Wordly panel so the native Share action is always
  visible in the right-side action rail above Done.
- Added per-puzzle native share and save actions for completed Wordly,
  Spelling B, Crossword, Sudoku and Connections results, backed by the
  existing answer-safe share-card renderer.
- Added UI mapper and visual-test harness coverage so completed puzzle screens
  expose safe share cards without leaking hidden answers.
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
- Completed the Crossword feature with grid parsing, numbering, crossing
  agreement, clue navigation, keyboard entry, autosave, hints, completion and
  share-safe result handling.
- Expanded preview content to 20 human-reviewed 7x7 Crossword fixtures and
  hardened validation for bounds, crossing conflicts, disconnected entries and
  clue answer leakage.
- Added Crossword engine, validator, UI mapper, responsive layout and
  instrumented visual/play-through tests, including short-phone fit checks.
- Completed the Sudoku feature with a reusable 6x6 engine, responsive Compose
  screen, row/column/region highlighting, pencil notes, eraser, undo/redo,
  mistake checking, hints, autosave, completion and share-safe result handling.
- Expanded preview content to 20 human-reviewed 6x6 Sudoku fixtures and
  hardened validation for row, column, region, given/solution and uniqueness
  rules.
- Added Sudoku engine, validator, UI mapper and instrumented visual/play-through
  tests for initial, selected, notes, conflict, hint and completion states.
- Aligned Sudoku and Crossword phone layouts so the square game boards fill the
  same horizontal rail as the status, controls, keyboard and hint/clue panels
  whenever the viewport height allows it without scrolling.
- Completed the Connections feature with a reusable engine, responsive Compose
  screen, 4x4 group selection, shuffle, deselect, mistake limits, progressive
  hints, autosave, completion/failure handling and share-safe result cards.
- Expanded preview content to 20 human-reviewed Connections fixtures and
  hardened validation for duplicate words, duplicate titles, missing hints and
  ambiguous group titles.
- Added Connections engine, validator, UI mapper and instrumented visual/play
  tests covering initial, selected, solved, hinted, failed, completed and
  interactive play states.
- Expanded Daily Home streaks with daily history, category streak tracking,
  comeback messaging and unlockable achievement badges.
- Added secure Daily Five share-card preview, cache rendering, gallery save and
  Android share intent support through FileProvider without answer leakage.
- Added a deterministic content pipeline module that exports the preview season
  pack, SHA-256 checksum, JSON validation summary, CSV puzzle index and HTML
  report.
- Verified the generated preview pack covers 20 days and 100 puzzles with a
  passing validator report.
- Added a Phase 14 Season One candidate export containing 365 days and 1,825
  puzzle records, with per-category counts, checksum, human-review CSV and
  release-blocker audit reports.
- Marked the Season One candidate as structurally valid but not release-ready
  because all generated candidate puzzles still require genuine human review
  and repeated candidate fingerprints require authoring replacement.
- Added a full-season simulator that solves all 1,825 candidate puzzles through
  the reusable engines, verifies save/restore, completion events, Daily Five
  streaks and answer-safe share models, and exports JSON/CSV/HTML reports.
- Added an automated quality audit for offline manifest, accessibility toggles,
  sharing cleanup, generated validation reports and release blockers.
- Added release preparation docs, installation instructions, final release
  report and v0.9.0 prerelease notes.

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

## Phases 11-13

- Status: streak history, achievements, secure sharing and deterministic content
  export/reporting are implemented for the preview season.

## Phase 14

- Status: full-season candidate export and automated audit are implemented;
  final production content release remains blocked by human review and
  replacement of repeated generated candidate content.

## Phases 15-17

- Status: full-season simulation, automated quality audit and release
  preparation artifacts are implemented. Production release remains blocked by
  content review, repeated-content replacement, manual VRT/accessibility review
  and private signing configuration.
