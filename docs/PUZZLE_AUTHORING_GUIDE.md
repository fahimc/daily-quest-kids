# Puzzle Authoring Guide

Use British English throughout.

Every puzzle requires:

- Stable ID.
- Category.
- Difficulty.
- Curriculum tags.
- Progressive hints.
- Automated review status.
- Human review status before production release.

Phase 14 candidate workflow:

- Use `./gradlew :content-tools:run --args="--season-one-candidate <repo-root>"`
  to export the current Season One candidate pack and reports.
- Treat every row in `reports/season-one-human-review.csv` with
  `human_reviewed=false` as an authoring task.
- Replace repeated generated candidate content before marking any puzzle as
  production-ready.
- Do not set `humanReviewed=true` unless a real reviewer has checked the puzzle
  for age fit, British English, safety, clue quality, hint leakage and answer
  correctness.

General safety:

- No adult, frightening, branded, political, news-driven, gambling or social
  comparison content.
- No clue or hint may reveal the answer unless it is an explicit late-stage
  reveal after confirmation.
- Share-card models must treat all answers as forbidden payloads.

Difficulty mix:

- 35% starter/lower Key Stage 2.
- 50% core.
- 15% challenge.
- No more than two challenge days consecutively.
