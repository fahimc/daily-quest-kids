# Design System

Visual direction: cheerful daily puzzle quest world, readable before
decorative, suitable for children aged 7-10.

Category identities:

- Wordly: green, Letter Garden.
- Spelling B: amber, Honeycomb Library.
- Crossword: blue, Clue Castle.
- Sudoku: purple, Number Temple.
- Connections: coral, Link Laboratory.

Tokens:

- Background: warm off-white.
- Surface: white.
- Success: green plus textual state.
- Hint: amber plus textual state.
- Warning/error: red plus textual state.
- Locked/completed/current/best streak states are represented by text and shape,
  not colour alone.
- High contrast mode increases light/dark separation and is wired to the
  persisted Settings toggle.

Typography:

- System sans font to avoid unlicensed bundled fonts.
- Large rounded-feeling titles through weight and spacing.
- Puzzle letters and numbers use large, high-contrast roles.

Spacing:

- 4, 8, 12, 16, 20, 24 and 32dp.
- Primary controls target 52dp height where practical.

Current shell:

- Home, Streaks and Settings use bottom navigation.
- Daily Home cards expose text state, hint counts, category labels and action
  labels.
- The hero streak panel has a stable `heroStreakPanel` test tag for
  screenshot-smoke coverage.

Wordly feature:

- The Wordly screen uses a bright Letter Garden direction inspired by the
  supplied reference: green/yellow/dark letter tiles, a compact top status bar,
  six-row board, flexible keyboard and bottom clue panel.
- Board tiles are constraint-sized from available phone width.
- Keyboard rows use weighted flexible keys so labels stay inside the controls on
  narrow phones.
- Empty, partial, repeated-letter, hint, success and failure states have
  instrumented screenshot-smoke targets.
