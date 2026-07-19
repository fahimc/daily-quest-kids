# Architecture

Daily Quest Kids uses modular clean architecture:

- `app` owns Android lifecycle, navigation and feature composition.
- `core-design` owns design tokens and reusable Compose UI primitives.
- `core-model` owns serializable schema models for seasons, puzzles, hints,
  progress and share-card models.
- `core-common` owns UTC date and streak logic.
- `core-data` owns offline pack access. Room-backed persistence is scheduled for
  Phase 4.
- `puzzle-engine` owns pure Kotlin puzzle rules and share-safety logic.
- `puzzle-validator` owns pure Kotlin content validation.

Puzzle rules, validators and date/streak logic have no dependency on Compose or
Android UI classes.

Dependency injection is currently manual through small provider classes. Hilt is
still allowed later, but manual injection is justified during early phases
because the repository must compile without adding unnecessary generated-code
complexity before repositories and Room are introduced.
