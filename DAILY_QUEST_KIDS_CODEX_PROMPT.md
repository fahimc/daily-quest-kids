# Daily Quest Kids — Master Codex Build Prompt

You are the lead Android engineer, mobile game UI designer, educational puzzle designer, QA engineer, accessibility specialist and technical project manager for **Daily Quest Kids**.

Build a complete offline Android puzzle game for children aged 7–10. Every calendar day, all players using the same puzzle pack must receive the same five puzzles:

1. Wordly-style five-letter word puzzle
2. Spelling B letter-hive puzzle
3. Mini crossword
4. Child-friendly 6×6 Sudoku
5. Connections-style word grouping puzzle

The finished first season must contain exactly 365 puzzles for each category: **1,825 fully authored, reviewed and validated puzzles in total**.

The game must be built through strict, self-contained phases. Do not attempt the whole product in one large pass. Every phase must end with working software, complete tests, visual regression baselines, updated documentation, a commit and a phase tag. After validating a phase, automatically continue to the next phase without asking the user for permission.

---

## 1. Non-negotiable product requirements

The application must:

- Be a native Android application written in Kotlin and Jetpack Compose.
- Use Material 3 only as a technical base beneath a custom children’s game design system.
- Work completely offline from first launch.
- Request no internet permission.
- Contain no adverts, analytics, tracking, accounts, login, online leaderboard, chat, remote AI or web requests.
- Store puzzle content, progress, settings, streaks and achievements locally.
- Provide exactly one puzzle from each category per UTC calendar day.
- Produce the same daily puzzle set on every installation using the same season pack.
- Include progressive hints for every puzzle.
- Track per-category streaks and an overall Daily Five streak.
- Allow puzzle results, Daily Five results and streak achievements to be shared as locally generated images.
- Never expose puzzle answers or personal information in a shared image.
- Support compact phones, standard phones and tablets.
- Be portrait-first while remaining safe and usable in landscape.
- Be accessible with TalkBack, large text, high contrast, reduced motion and colour-independent feedback.
- Feel like a polished mobile game, not a collection of Material forms or school worksheets.

The final brand name is **Daily Quest Kids**.

Recommended tagline: **Five puzzles. A new quest every day.**

---

## 2. Audience and curriculum research

The audience is children aged 7–10 in the United Kingdom, broadly covering Years 3–5 with carefully selected early Year 6 challenge content.

Before producing the final puzzle pack, research and document the current England Key Stage 2 curriculum, including:

- English programmes of study
- Years 3–4 spelling requirements
- Years 5–6 spelling requirements
- Statutory spelling lists
- Prefixes, suffixes, homophones and word families
- Morphology and age-appropriate etymology
- Reading vocabulary and clue comprehension
- Mathematics fluency, reasoning and problem solving
- Suitable science, geography and general-knowledge topics

Use British English throughout, such as `colour`, `favourite`, `centre`, `travelled` and `organise`.

Create `docs/CURRICULUM_RESEARCH.md` covering:

- Suitable vocabulary by age/year group
- Recommended word lengths and sentence lengths
- Suitable clue complexity
- Words suitable only for challenge content
- Topics to use and topics to avoid
- How each puzzle type supports learning
- How difficulty progresses through the season

Use this approximate difficulty distribution:

- 35% accessible Years 3–4
- 50% core Years 4–5
- 15% challenge Years 5–6

Never place more than two challenge days consecutively.

---

## 3. Required technology and architecture

Use current stable versions available at implementation time:

- Kotlin
- Jetpack Compose
- Material 3
- Navigation Compose
- Coroutines and Flow
- Room
- DataStore
- Kotlin serialization where suitable
- Hilt or a clearly justified alternative
- Android Canvas or Compose graphics for share cards
- FileProvider and Android Sharesheet
- JUnit
- Android instrumented tests
- Compose UI tests
- Screenshot/golden visual regression testing
- Property-based or parameterised tests
- Gradle version catalog
- Android Lint
- Detekt
- Ktlint
- Macrobenchmark where practical

Use a modular clean architecture. Suggested modules:

```text
app
core-model
core-common
core-design
core-data
core-database
core-testing
feature-splash
feature-onboarding
feature-home
feature-wordly
feature-spelling
feature-crossword
feature-sudoku
feature-connections
feature-streaks
feature-achievements
feature-sharing
feature-settings
puzzle-engine
puzzle-validator
content-tools
benchmark
```

Keep UI, view models, domain logic, puzzle engines, persistence, content validation and platform services separated. Puzzle rules and validators must not depend on Compose or Android UI classes.

---

## 4. Repository governance

Create and continuously maintain:

- `README.md`
- `CHANGELOG.md`
- `docs/ARCHITECTURE.md`
- `docs/ROADMAP.md`
- `docs/PHASE_STATUS.md`
- `docs/TESTING.md`
- `docs/ACCESSIBILITY.md`
- `docs/PRIVACY.md`
- `docs/DESIGN_SYSTEM.md`
- `docs/CURRICULUM_RESEARCH.md`
- `docs/PUZZLE_AUTHORING_GUIDE.md`
- `docs/PUZZLE_PACK_SCHEMA.md`
- `docs/CONTENT_VALIDATION.md`
- `docs/RELEASE_CHECKLIST.md`

At the end of every phase:

1. Build the debug application.
2. Run phase unit tests.
3. Run integration and functional tests.
4. Run Compose UI tests.
5. Run visual regression tests.
6. Run Android Lint, Detekt and formatting checks.
7. Fix all phase-related failures.
8. Update documentation and the changelog.
9. Update `docs/PHASE_STATUS.md` with completed scope, test results and known limitations.
10. Capture approved screenshots.
11. Commit the completed phase.
12. Tag the phase.
13. Automatically continue to the next phase.

Use commit messages such as:

```text
phase-01: establish project architecture and quality gates
phase-02: implement game design system and responsive shell
```

Use tags such as:

```text
phase-01-complete
phase-02-complete
```

Do not continue while compilation errors, phase test failures, broken navigation, critical accessibility defects, unresolved VRT differences or phase placeholders remain.

Do not weaken or delete meaningful tests simply to make a build pass. Do not overwrite VRT baselines automatically without reviewing the visual change.

---

## 5. Automatic continuation protocol

After each phase:

1. **Validate:** run every required check.
2. **Repair:** fix failures and rerun until green.
3. **Document:** update status, architecture, testing notes, changelog and screenshots.
4. **Commit:** commit and tag the phase.
5. **Continue:** immediately start the next phase.

Do not ask the user whether to continue. Only stop if all phases are complete or an unavoidable external requirement such as a private release signing key is missing. The offline game itself must not require any unavailable service.

A phase is complete only when its feature works through the actual UI, navigation works, required state persists, loading/empty/success/error states exist, accessibility semantics exist, unit and functional tests pass, VRT baselines exist and no phase placeholder remains.

---

## 6. Full mobile game design direction

### 6.1 Visual concept

Create a cheerful **daily puzzle quest world**. Each puzzle is presented as a destination in a light adventure map or clubhouse rather than a plain utility card.

Suggested configurable zones:

- Wordly: Letter Garden
- Spelling B: Honeycomb Library
- Crossword: Clue Castle
- Sudoku: Number Temple
- Connections: Link Laboratory

The style must be:

- Cheerful but not babyish
- Clever, calm and rewarding
- Colourful without overstimulation
- Modern and original
- Readable before decorative
- Suitable for ages 7–10

Avoid copying proprietary branding, artwork, sound effects, layouts or exact terminology from commercial puzzle games.

### 6.2 Category identity

Use distinct category themes:

- Wordly: green
- Spelling B: amber
- Crossword: blue
- Sudoku: purple
- Connections: coral

Never communicate state through colour alone. Add icons, borders, patterns, shapes and text labels.

Create semantic design tokens for backgrounds, surfaces, elevated surfaces, primary and secondary actions, success, hints, warnings, errors, locked states, completion, current streak and best streak.

### 6.3 Typography

Use a highly legible, properly licensed Android-compatible rounded font or system font. Do not bundle unlicensed fonts.

Create typography roles for:

- Game logo/display title
- Screen title
- Section title
- Puzzle clue
- Tile letter/number
- Body text
- Supporting text
- Buttons
- Status labels

Requirements:

- Large puzzle letters and numbers
- Clear distinction between `I`, `l`, `1`, `O` and `0`
- No long all-caps paragraphs
- No clipping at 200% text scale
- Comfortable line spacing for children

### 6.4 Shapes, spacing and touch

Use rounded panels, squircle puzzle cards, large circular icon buttons and soft elevation. Crossword and Sudoku cells should remain precise and grid-aligned rather than overly rounded.

Use a consistent spacing scale: 4, 8, 12, 16, 20, 24 and 32dp.

All touch targets must be at least 48dp. Primary buttons should normally be 52–60dp high.

### 6.5 Motion, sound and haptics

Use subtle card presses, tile movement, completion bounces, small particles, smooth progress updates and group reveal transitions.

Avoid full-screen flashing, aggressive shaking, rapid particle effects, constant background motion and casino-like celebrations.

Provide optional tile, success, group and Daily Five sounds plus light haptics. All must be disableable. Respect reduced-motion settings.

### 6.6 Responsive layouts

Support:

- Compact phone: 320–379dp width
- Standard phone: 380–599dp width
- Tablet/expanded: 600dp and above

Compact phone:

- Single column
- 16dp horizontal margins
- Compact top bar
- Core puzzle area receives most vertical space
- Reachable bottom controls
- Avoid scrolling during primary puzzle play where practical

Standard phone:

- 20dp margins
- Larger boards
- Expanded progress summaries
- Optional inline hint panels

Tablet:

- Centred maximum-width game panel
- Two-pane layouts where useful
- Board beside clues/hints for crossword and similar features
- Do not simply stretch phone content across the full width

Use window size classes and test all key screens at each size.

### 6.7 Navigation

Main destinations:

- Home
- Streaks
- Achievements
- Settings

Use bottom navigation on suitable screens. Puzzle screens should become immersive, hiding the bottom navigation when space is required, while retaining a clear back action.

---

## 7. Screen-by-screen design requirements

### Splash

- Calm full-screen background or light illustration
- Central Daily Quest Kids logo
- Small puzzle-piece or star animation
- Hide initial local setup
- Transition quickly
- No unnecessary controls

### Welcome

- Friendly abstract puzzle mascots
- Large game title and tagline
- One short explanation
- Primary action: `Start Playing`
- Secondary action: `How It Works`
- Quiet parent information link
- Do not request a child’s name, age or date of birth

### Onboarding

Use no more than three short pages:

1. Five new puzzles each day
2. Solve puzzles to build streaks; hints do not break streaks
3. Share answer-safe result pictures; ask a grown-up before sharing

Include Skip, Back, Continue and a page indicator.

### Daily home screen

This is the central screen.

Top area:

- Friendly greeting
- Current date
- Global day number
- Settings action

Hero streak panel:

- Current Daily Five streak
- Best streak
- Progress such as 3/5
- Progress ring or stepped bar
- Calm streak icon/animation

Daily puzzle area:

- Five large quest cards
- Category icon and title
- Short description
- Difficulty
- Status
- Category streak
- Hint indicator
- `Play`, `Continue` or `View Result` action

Support not started, in progress, completed, failed, revealed and locked card states. Completed cards remain tappable.

Bottom area:

- Share today’s result
- Streak calendar shortcut
- Calm message such as `New puzzles arrive tomorrow`

Do not use pressure-inducing countdowns.

### Wordly

- Top bar with back, title, attempt count, hints and menu
- Six rows of five large tiles
- Strong active-row indicator
- Correct, present and absent states using colour plus shape/icon/border
- Three-row keyboard with child-sized keys
- Enter and delete clearly labelled
- No scrolling on normal phones where possible
- Friendly invalid-word message near the grid
- Accurate repeated-letter handling
- Completion panel with definition, example sentence, attempts, hints used, streak, share and return actions

### Spelling B

- Top bar with score and hints
- Large seven-letter honeycomb
- Centre letter visibly distinct
- Current word displayed prominently
- Shuffle, clear and submit controls
- Score progress, achievement level and word count
- Expandable found-word list with definitions
- Never expose undiscovered words before completion

### Crossword

- Large centred 7×7 grid
- Active cell and active answer highlight
- Clear numbers and direction
- Fixed clue panel above the keyboard on phones
- Previous/next clue actions
- Expandable Across/Down clue list
- Responsive cell sizing
- Tablet two-pane layout with grid left and clue lists right

### Sudoku

- 6×6 board with strong 2×3 region boundaries
- Givens visually distinct from player entries
- Selected cell, row, column, region and matching-number highlights
- Number buttons 1–6
- Pencil mode, eraser, undo and redo
- Optional mistake checking
- Hint explanation bottom sheet written in simple language

### Connections

- 4×4 word-tile grid
- Large readable text and equal tile sizes
- Selected state uses border, icon and background
- Shuffle, deselect and submit actions
- Submit enabled after exactly four selections
- Solved groups collapse into full-width group bars showing title, words and a short explanation
- Calm incorrect-attempt feedback

### Puzzle completion

- Category-specific completion illustration or badge
- Result summary
- Current and best category streaks
- Hints used
- Positive improvement message
- Share result
- Return to Daily Five
- Next unsolved puzzle

### Daily Five celebration

- Trigger after all five puzzles are solved
- Five completed icons
- Current and best Daily Five streaks
- Perfect-day count
- Share action
- View streak calendar
- Respect reduced motion

### Streak dashboard

- Current and best Daily Five streaks
- Perfect-day count
- Total puzzles solved
- Category current and best streaks
- Share streak action
- Friendly comeback messaging

### Streak calendar

Month grid states:

- No activity
- Partial completion
- All five completed
- Today
- Future locked date
- Outside season

Past results may be viewed but must not retroactively alter streaks.

### Achievements

Badge grid covering first steps, puzzle mastery, streaks, hint-free successes, season progress and comeback milestones. Locked badges must not use manipulative pressure.

### Share preview

- Large local card preview
- Share, Save and Cancel
- Reminder: `Ask a grown-up before sharing outside the app.`
- Never include child name, age, location, device data or hidden answers

### Settings

- Sound
- Haptics
- Reduced motion
- High contrast
- Large puzzle text
- Optional timer
- Mistake checking
- Instructions
- Privacy
- Parent information
- Reset progress with confirmation

### Parent information

Explain the educational purpose, offline operation, lack of ads/tracking/accounts, local data storage, sharing and reset behaviour.

### Season complete

Show season badge, total solved, perfect days, best streak, category statistics and a final answer-safe share card. Architecture must support future seasons.

---

## 8. Daily date and puzzle-pack system

Use a configurable `seasonStartDateUtc`.

```text
dayIndex = UTC calendar days between seasonStartDateUtc and current UTC date
```

Rules:

- Index 0 maps to Day 1; index 364 maps to Day 365.
- Runtime randomness must never select the daily puzzle.
- Every installation using the same pack and UTC date loads the same puzzle IDs.
- Installing late opens the current global day.
- Future days remain locked.
- Puzzle state is saved by stable puzzle ID.
- The pack has a schema version, season version and checksums.
- Debug builds support a developer date override.
- Release builds must not expose the override.
- After Day 365, show Season Complete.
- Support adding future versioned seasons without rewriting the engines.

Clock manipulation is impossible to prevent fully offline. Use a non-punitive approach:

- Store the greatest UTC day observed.
- Never award the same streak day twice.
- Never erase legitimate completion when the clock moves backwards.
- Pause new streak advancement until a valid later date is reached.
- Show a friendly device-date message.
- Never accuse the child of cheating.

---

## 9. Streaks

### Category streaks

Track per category:

- Current streak
- Best streak
- Total solved
- Last solved UTC day
- Hints used
- Hint-free completions

A category streak increases when that category is solved on consecutive global puzzle days. Hints do not break a streak.

### Daily Five streak

Increase only when all five puzzles for a global day are solved.

Track:

- Current streak
- Best streak
- Perfect-day count
- Longest historical streak
- Today’s progress
- Completion calendar

Use friendly messages such as:

- `A new streak can begin today.`
- `You reached a best streak of 8 days.`
- `Ready for another puzzle quest?`

Avoid guilt, pressure and loss-focused language.

---

## 10. Sharing

Render all cards locally at about 1080×1350 pixels. Use Android Canvas or Compose rendering, FileProvider and the system Sharesheet.

### Individual result card

Include:

- Daily Quest Kids branding
- UTC date
- Category
- Answer-safe result pattern
- Attempts/errors
- Hints used
- Current and best category streaks

### Daily Five card

Include:

- Date
- Five category icons and completion states
- Completed count out of five
- Current and best Daily Five streaks

### Streak card

Include:

- Large current streak
- Best streak
- Perfect days
- Category streak summary
- Achievement badge
- Generic text such as `I’m on a 12-day puzzle streak!`

Never include answers, hidden words, child identity, location, device information, advertising IDs or location EXIF metadata. Do not request broad storage permission.

Add automated leak tests for every card model and renderer.

---

## 11. Puzzle rules

### 11.1 Wordly-style puzzle

- Five-letter British English solution
- Six attempts
- Curated child-safe guess dictionary
- Proper duplicate-letter scoring
- No proper nouns, abbreviations, profanity, slang or obscure inflections

Hints:

1. Broad category or meaning
2. Child-friendly definition or sentence with a blank
3. Reveal one correctly positioned letter

After completion show definition, example sentence and optional morphology note.

Validate all 365 puzzles for length, dictionary membership, uniqueness, safety, British spelling, hint leakage, example leakage and correct scoring.

### 11.2 Spelling B

- Seven unique letters
- One required centre letter
- 8–24 curated target words
- Three-letter minimum
- Every word includes the centre letter
- Words use only supplied letters; letters may repeat
- At least one all-seven-letter word where practical

Hints:

1. Counts by starting letter
2. Length of an undiscovered word
3. Definition
4. First letter

Validate letter set, centre inclusion, target uniqueness, score totals, safety and hint correctness.

### 11.3 Mini crossword

- Primarily 7×7
- 6–14 entries
- Three-letter minimum
- British English clues and answers
- Clear Across/Down numbering
- No celebrity trivia, brands, current news, politics, adult topics or obscure abbreviations

Hints:

1. Rephrase clue
2. Reveal one selected letter
3. Check selected answer
4. Reveal selected answer after confirmation

Validate grid dimensions, crossings, numbering, entry reachability, clue presence, answer fit, clue leakage and final grid.

### 11.4 Sudoku

- 6×6
- Digits 1–6
- 2×3 regions
- Exactly one solution
- No guessing required
- Child-level logical techniques only

Difficulty:

- Starter: direct singles
- Explorer: row/column/region elimination
- Thinker: modest chained elimination without advanced expert techniques

Hints:

1. Highlight useful area
2. Explain why candidates cannot fit
3. Highlight a single-candidate cell
4. Place one number after confirmation

Use an independent solver to validate givens, uniqueness, stored solution, technique level and hint validity.

### 11.5 Connections

- 16 unique visible words
- Four intended groups
- Four words per group
- One intended complete partition
- Up to five incorrect group submissions
- Child-friendly themes and fair red herrings

Hints:

1. Broadly describe one group
2. Highlight two words from the same group
3. Reveal one group title
4. Complete one group after confirmation

Validate counts, uniqueness, intended partition, group titles, hint pairs and plausible alternative full partitions. Flag semantic ambiguity for human review.

---

## 12. Testing philosophy

Testing must be built alongside each phase.

Every phase requires, where relevant:

- Unit tests
- Integration tests
- Functional tests
- Compose UI tests
- Visual regression tests (VRT)
- Accessibility checks
- Persistence and process-death tests
- Loading, empty, success and error-state tests
- Responsive-layout tests

Use deterministic VRT conditions:

- Fixed device sizes
- Fixed font scales
- Fixed locale
- Fixed clock/date
- Fixed data
- Disabled or controlled animations
- Stable system bars and theme

Store approved baselines in version control. Never overwrite them blindly to hide regressions.

Coverage goals:

- At least 90% for core domain logic
- 100% branch coverage where practical for puzzle validators
- 100% structural validation of all 1,825 puzzles
- Complete coverage of date, streak and share-leak logic

Do not write meaningless tests merely to inflate coverage.

---

# 13. Phased implementation plan

## Phase 0 — Repository and quality foundation

Deliver:

- New Git repository
- Compiling modular Android project
- Gradle version catalog
- CI workflow
- Lint, Detekt and Ktlint
- Unit, instrumented, Compose and VRT test frameworks
- Initial documentation

Tests:

- Sample unit test
- Sample instrumented test
- Sample Compose test
- Sample screenshot test
- Debug APK build

Exit only when the clean build and all sample checks pass. Commit, tag and auto-continue.

## Phase 1 — Curriculum research and content models

Deliver:

- Curriculum research
- Versioned puzzle-pack schema
- Kotlin models for five puzzle types, days, hints, difficulty, curriculum tags, progress and validation
- Content-safety rules and prohibited vocabulary mechanism

Tests:

- Serialization round trips
- Invalid schema rejection
- Missing category detection
- Duplicate ID detection
- Difficulty and hint validation
- Unsafe-word filtering
- Schema compatibility
- VRT for a developer content-inspector screen

Commit, tag and auto-continue.

## Phase 2 — Design system and responsive shell

Deliver the production-ready colour, typography, spacing, shape, motion and category systems plus navigation, top bars, bottom navigation, buttons, puzzle cards, status badges, progress components, sheets, dialogs and standard UI states.

Tests:

- Navigation and back stack
- Process recreation
- TalkBack semantics
- Touch targets
- Large text
- Reduced motion
- Compact, standard and tablet VRT
- VRT for every shared component state

Commit, tag and auto-continue.

## Phase 3 — Splash, onboarding, settings and parent information

Deliver all first-launch and preference flows using DataStore.

Tests:

- First/returning launch
- Skip/complete onboarding
- Preference persistence
- Reset behaviour
- Process recreation
- Accessibility
- VRT for every screen, size and major state

Commit, tag and auto-continue.

## Phase 4 — UTC date engine, puzzle pack and persistence

Deliver:

- Season configuration
- Day-index engine
- Debug date override with release protection
- Room database and migrations
- Puzzle-pack loading and checksums
- Progress entities and repositories
- Friendly invalid-date and season-complete handling

Unit tests must cover season boundaries, leap years, leap day, month/year boundaries, UTC boundaries, daylight saving, timezone changes, clock rollback/advance, duplicate awards, deterministic cross-install mapping, migrations and checksum rejection.

Functional tests must cover install-on-current-day, restart, process death, future lock and season completion.

Commit, tag and auto-continue.

## Phase 5 — Production Daily Home

Deliver the fully state-driven dashboard with all card states, Daily Five progress, current/best streaks, difficulty, hints, date, day number, responsive tablet layout and working navigation.

Tests:

- Every card state
- Mixed completion states
- 0/5 through 5/5 VRT
- Date change while open
- Process recreation
- Navigation to each feature
- Accessibility and responsive layouts

Commit, tag and auto-continue.

## Phase 6 — Wordly engine and feature

Deliver the reusable engine, keyboard, six-row board, duplicate scoring, hints, autosave, success/failure, learning summary, streak integration and share-safe model. Include at least 20 reviewed fixtures.

Tests:

- Exhaustive/property scoring including repeats
- Invalid guesses
- Attempt exhaustion
- Hint order
- save/restore
- completion emitted once
- share answer leakage
- full one-to-six-attempt flows
- failure and resume
- VRT for empty, partial, repeat, hint, success and failure states

Commit, tag and auto-continue.

## Phase 7 — Spelling B engine and feature

Deliver hive input, centre-letter enforcement, scoring, bonus, shuffle, found words, definitions, hints, persistence, completion, streaks and share-safe model. Include at least 20 fixtures.

Test invalid letters, centre omission, duplicates, short words, scoring, bonus, hints, save/restore and all UI flows. Add VRT for every major state and device class.

Commit, tag and auto-continue.

## Phase 8 — Crossword engine and feature

Deliver grid parsing, numbering, crossings, cell and direction selection, clue navigation, keyboard, hints, autosave, completion, streaks, sharing and phone/tablet layouts. Include at least 20 fixtures.

Test parsing, numbering, crossing agreement, movement, direction switching, completion, persistence and invalid-grid rejection. Add full UI and VRT coverage.

Commit, tag and auto-continue.

## Phase 9 — Sudoku engine and feature

Deliver board logic, conflicts, candidates, pencil notes, eraser, undo/redo, highlights, mistake checking, child-friendly hints, autosave, completion, streaks and sharing. Include at least 20 unique fixtures.

Test row/column/region rules, candidate generation, history, uniqueness, hint validity, completion and process restoration. Add VRT for initial, selected, notes, conflict, hint and completion states.

Commit, tag and auto-continue.

## Phase 10 — Connections engine and feature

Deliver 4×4 tiles, selection, submission, mistakes, shuffle, deselection, group reveals, explanations, hints, persistence, completion/failure, streaks and sharing. Include at least 20 fixtures.

Test exact and incorrect groups, duplicate/overlap rejection, mistake limits, hints, shuffle preservation and save/restore. Add VRT for initial through completion/failure.

Commit, tag and auto-continue.

## Phase 11 — Streaks, history and achievements

Deliver category streaks, Daily Five streak, best streaks, perfect days, completion calendar, dashboard, celebration, achievements and comeback messaging.

Test first completion, consecutive days, missed/partial/perfect days, per-category behaviour, hints, failure, clock reversal, timezone travel and duplicate events. Add functional and VRT coverage.

Commit, tag and auto-continue.

## Phase 12 — Secure image sharing

Deliver individual, Daily Five and streak cards; preview; save; FileProvider; Sharesheet; metadata stripping and adult reminder.

Test that every answer type and personal field is absent, filenames are safe, files are cleaned up and Sharesheet intents work. Add production-resolution VRT for every card type and edge case.

Commit, tag and auto-continue.

## Phase 13 — Deterministic content pipeline

Create:

```text
content-tools/
content-source/
puzzle-pack/
reports/
```

The pipeline must load curated dictionaries and curriculum vocabulary, generate/import candidates, validate structure and safety, estimate difficulty, detect duplicates/near-duplicates, test hint leakage, generate human-review reports, export the versioned production pack and checksums, and fail with a non-zero exit code on invalid content.

Produce HTML, CSV and machine-readable reports plus puzzle, difficulty, vocabulary, duplicate and hint-leak statistics.

Test generator determinism and every validator failure mode.

Commit, tag and auto-continue.

## Phase 14 — Produce all 1,825 puzzles

Create exactly 365 production puzzles for each category, five per day, with hints, definitions/explanations, difficulty, curriculum tags, review metadata and checksums.

Release validation must fail if counts differ, a day is incomplete, IDs duplicate, a puzzle is structurally invalid, a solution is non-unique where required, content is unsafe, hints leak answers, sharing leaks answers, review status is incomplete or checksums fail.

Produce human-review reports for all puzzles. Do not falsely claim a human reviewed content if that did not occur. Mark genuine human sign-off as a release blocker while completing every possible automated and model-assisted review.

Commit, tag and auto-continue.

## Phase 15 — Full season integration simulation

Automatically simulate all 365 days through real repositories and engines.

For every day verify:

- All five correct puzzles load
- Hints load
- Saves restore
- Completion updates category streaks
- Five completions update Daily Five streak
- Share models are answer-safe
- No crash occurs

Also simulate perfect season, missed days, partial days, hint-heavy use, date rollback, timezone changes, an app update and migration midway through the season.

Commit, tag and auto-continue.

## Phase 16 — Accessibility, performance and polish

Complete TalkBack, traversal, touch targets, high contrast, 200% font scaling, reduced motion, sound-independent feedback, clear instructions and manual accessibility review.

Benchmark cold start, home load, puzzle opening, database queries, share rendering, memory, recompositions and APK size. Test process death, low-memory recreation, rapid navigation, repeated sharing and airplane-mode first launch.

Approve final VRT baselines for all major screens and device classes.

Commit, tag and auto-continue.

## Phase 17 — Release preparation

Deliver:

- Release APK
- Release AAB
- Version information
- Adaptive icon and splash assets
- Store screenshots
- Privacy and parent documents
- Release notes
- Installation instructions
- Final test, coverage, VRT and content-validation reports
- Completed release checklist

Do not commit private signing keys. Document local/environment-based signing.

Run the complete clean release gate: all unit, integration, functional, Compose, content, full-season, screenshot, lint, static-analysis, accessibility, benchmark, installation, airplane-mode, upgrade and migration tests.

Final commit:

```text
release: complete Daily Quest Kids season one
```

Create the final release tag.

---

## 14. Minimum VRT matrix

Capture compact phone, standard phone and tablet baselines for:

- Splash
- Welcome
- Onboarding
- Home at 0/5, partial and 5/5
- Settings
- Parent information
- Invalid date
- Season complete
- Every major state of each puzzle
- Puzzle completion
- Daily Five celebration
- Streak dashboard and calendar
- Achievements
- All three share card types

Capture critical screens at 200% text scale and in high-contrast mode.

---

## 15. Error handling

Handle corrupt packs, migration failure, missing puzzle records, invalid dates, unsupported pack versions, failed image generation, insufficient storage, process death and unexpected restart.

Show child-friendly UI messages. Never expose stack traces to players. Log technical details only locally in debug builds.

---

## 16. Child-safety rules

Do not include profanity, sexual content, drugs, gambling, graphic violence, frightening imagery, current politics, celebrity gossip, current news, brands as answers, public usernames, chat, friend requests, online sharing within the app, manipulative notifications, purchases, loot boxes or advertising.

Avoid pressure messages such as `You lost your streak!`, late-night countdowns or social comparison.

Prefer calm messages such as:

- `A new streak can begin today.`
- `You solved three puzzles today.`
- `Great thinking.`
- `Try a hint when you need one.`

---

## 17. Final definition of done

The project is complete only when:

- The app works completely offline and requests no internet permission.
- The same UTC date produces the same five puzzles on every installation using the same pack.
- All five engines and screens are complete.
- Exactly 365 puzzles exist per category and 1,825 total.
- Every puzzle has valid hints and completed automated validation.
- Progress autosaves and survives process death.
- Category and Daily Five streaks work and are visible.
- Streak history and achievements work.
- Puzzle results and streaks can be shared as answer-safe images.
- Accessibility and responsive phone/tablet requirements are met.
- Unit, integration, functional, Compose UI and VRT tests pass.
- Full-season simulation passes.
- Release APK and AAB build.
- Documentation is complete.
- No placeholders or release-blocking TODOs remain.
- Outstanding genuine human content-review requirements are explicitly reported rather than misrepresented.

Continue automatically through every phase until this definition of done is satisfied.

At completion, provide a final report containing:

- Repository structure
- Build instructions
- Test commands and results
- Coverage results
- VRT results
- Content totals and validation totals
- APK and AAB paths
- Known limitations
- Human-review items still requiring sign-off
- Release-readiness status
