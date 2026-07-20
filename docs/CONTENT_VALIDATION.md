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

Production validation now includes:

- pipeline export wiring for the full 365-day count enforcement.
- crossword crossing consistency.
- semantic ambiguity detection for Connections.
- checksum generation and verification.
- human-review report export.

Production validation must still add:

- stronger independent Sudoku uniqueness and technique validation outside the
  current validator.
- stronger near-duplicate reports beyond exact content fingerprints.
- independent semantic review for vocabulary, clues and Connections group
  ambiguity.

Current preview content:

- Phase 6 includes 20 human-reviewed Wordly fixtures.
- Later puzzle categories still use automated preview fixtures until their
  feature/content phases.

Phase 14 Season One candidate content:

- `puzzle-pack/season-one-candidate.json` contains 365 days and 1,825 puzzle
  records.
- `reports/season-one-validation.json` records automated structural pass,
  per-category counts of 365 puzzles each and release blockers.
- The candidate is not release-ready. The report intentionally marks all 1,825
  candidate puzzles as requiring genuine human review before production release.
- The report also flags repeated content fingerprints that must be replaced
  through real content authoring before release.
