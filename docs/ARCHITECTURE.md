# Architecture

Daily Quest Kids uses modular clean architecture:

- `app` owns Android lifecycle, navigation and feature composition.
- `core-design` owns design tokens and reusable Compose UI primitives.
- `core-model` owns serializable schema models for seasons, puzzles, hints,
  progress and share-card models.
- `core-common` owns UTC date and streak logic.
- `core-data` owns offline pack access.
- `puzzle-engine` owns pure Kotlin puzzle rules and share-safety logic.
- `puzzle-validator` owns pure Kotlin content validation.

Puzzle rules, validators and date/streak logic have no dependency on Compose or
Android UI classes.

The Android app currently wires dependencies through `DailyQuestContainer`.
Settings and preview progress use Android DataStore so first-launch and Daily
Home flows can persist without accounts, internet or generated database code.
Room-backed puzzle history, migrations and backup/restore remain available for
the richer persistence phase.

Wordly follows the intended feature split: `puzzle-engine` owns pure game state,
scoring, validation, save JSON, completion events and share-safe models, while
`app` owns Compose rendering and DataStore autosave.

Dependency injection is currently manual through small provider classes. Hilt is
still allowed later, but manual injection is justified while the module graph
and storage contracts are still stabilising.
