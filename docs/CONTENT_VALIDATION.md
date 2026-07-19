# Content Validation

Validators currently check:

- Schema version presence.
- Season version presence.
- Sequential day numbering.
- Missing categories.
- Duplicate categories inside a day.
- Duplicate puzzle IDs.
- Nonblank curriculum tags.
- Progressive hints with sequential orders from 1.
- Answer-leaking hint flags.
- Automated review flags.
- Production human-review flags.
- Wordly length, dictionary inclusion and prohibited vocabulary.
- Spelling B unique letters, centre-letter inclusion and target word legality.
- Crossword grid dimensions and answer bounds.
- Sudoku cell count, value range and row validity.
- Connections group and visible-word counts.

Production validation must still add:

- pipeline export wiring for the full 365-day count enforcement.
- stronger Sudoku uniqueness and technique validation.
- crossword crossing consistency.
- semantic ambiguity detection for Connections.
- near-duplicate and hint-leak reports.
- checksum generation and verification.
- human-review report export.
