# Puzzle Pack Schema

The pack is represented by `PuzzlePack` in `core-model`.

Required top-level fields:

- `schemaVersion`
- `seasonVersion`
- `seasonStartDateUtc`
- `checksum`
- `days`

Each `DailyPuzzleSet` contains:

- `dayIndex`, where 0 maps to Day 1.
- `globalDayNumber`, equal to `dayIndex + 1`.
- exactly one puzzle per category for production.

Puzzle categories:

- `WORDLY`
- `SPELLING_B`
- `CROSSWORD`
- `SUDOKU`
- `CONNECTIONS`

Production pack rule:

- exactly 365 days.
- exactly 365 puzzles per category.
- exactly 1,825 puzzles total.
- all automated validation passes.
- all human review metadata is complete.
- checksums match exported content.
