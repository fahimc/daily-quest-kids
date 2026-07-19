# Content Validation

Validators currently check:

- Schema version presence.
- Season version presence.
- Day numbering.
- Missing categories.
- Duplicate puzzle IDs.
- Progressive hints.
- Answer-leaking hint flags.
- Automated review flags.
- Wordly length, dictionary inclusion and prohibited vocabulary.
- Spelling B unique letters, centre-letter inclusion and target word legality.
- Crossword grid dimensions and answer bounds.
- Sudoku cell count, value range and row validity.
- Connections group and visible-word counts.

Production validation must still add:

- full 365-day count enforcement in pipeline export.
- stronger Sudoku uniqueness and technique validation.
- crossword crossing consistency.
- semantic ambiguity detection for Connections.
- near-duplicate and hint-leak reports.
- checksum generation and verification.
- human-review report export.
