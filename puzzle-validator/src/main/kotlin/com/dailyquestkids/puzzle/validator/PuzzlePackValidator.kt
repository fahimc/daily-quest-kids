package com.dailyquestkids.puzzle.validator

import com.dailyquestkids.core.model.ConnectionGroup
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordEntry
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle

class PuzzlePackValidator(
    private val prohibitedWords: Set<String> = DEFAULT_PROHIBITED_WORDS,
) {
    fun validate(
        pack: PuzzlePack,
        requireFullSeason: Boolean = false,
    ): ValidationReport {
        val errors = mutableListOf<String>()
        if (pack.schemaVersion < 1) errors += "schemaVersion must be at least 1"
        if (pack.seasonVersion.isBlank()) errors += "seasonVersion is required"
        if (pack.days.isEmpty()) errors += "pack must contain at least one day"
        if (requireFullSeason && pack.days.size != SEASON_LENGTH) {
            errors += "production pack must contain exactly $SEASON_LENGTH days"
        }

        pack.days.forEachIndexed { expectedIndex, day ->
            if (day.dayIndex != expectedIndex) {
                errors += "day index ${day.dayIndex} should be $expectedIndex"
            }
        }

        val puzzleIds = mutableSetOf<String>()
        pack.days.forEach { day ->
            validateDay(day, requireFullSeason, errors)
            day.puzzles.forEach { puzzle ->
                if (!puzzleIds.add(puzzle.id)) errors += "duplicate puzzle id ${puzzle.id}"
                validatePuzzle(puzzle, requireFullSeason, errors)
            }
        }

        return ValidationReport(errors = errors)
    }

    private fun validateDay(
        day: DailyPuzzleSet,
        requireFullSeason: Boolean,
        errors: MutableList<String>,
    ) {
        if (day.globalDayNumber != day.dayIndex + 1) {
            errors += "day ${day.dayIndex} has mismatched globalDayNumber"
        }
        val categories = day.puzzles.map { it.category }.toSet()
        val missing = PuzzleCategory.entries.filterNot { it in categories }
        if (missing.isNotEmpty()) errors += "day ${day.dayIndex} missing categories $missing"
        if (categories.size != day.puzzles.size) errors += "day ${day.dayIndex} contains duplicate categories"
        if (requireFullSeason && day.puzzles.size != PuzzleCategory.entries.size) {
            errors += "day ${day.dayIndex} must contain exactly five puzzles"
        }
    }

    private fun validatePuzzle(
        puzzle: Puzzle,
        requireProduction: Boolean,
        errors: MutableList<String>,
    ) {
        validatePuzzleMetadata(puzzle, errors)
        validatePuzzleHints(puzzle, errors)
        validatePuzzleReview(puzzle, requireProduction, errors)

        when (puzzle) {
            is WordlyPuzzle -> validateWordly(puzzle, errors)
            is SpellingBeePuzzle -> validateSpelling(puzzle, errors)
            is CrosswordPuzzle -> validateCrossword(puzzle, errors)
            is SudokuPuzzle -> validateSudoku(puzzle, errors)
            is ConnectionsPuzzle -> validateConnections(puzzle, errors)
        }
    }

    private fun validatePuzzleMetadata(
        puzzle: Puzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.id.isBlank()) errors += "blank puzzle id"
        if (puzzle.curriculumTags.isEmpty() || puzzle.curriculumTags.any { it.isBlank() }) {
            errors += "${puzzle.id} must provide curriculum tags"
        }
    }

    private fun validatePuzzleHints(
        puzzle: Puzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.hints.isEmpty()) errors += "${puzzle.id} must provide progressive hints"
        if (puzzle.hints.map { it.order } != (1..puzzle.hints.size).toList()) {
            errors += "${puzzle.id} hint orders must be sequential from 1"
        }
        if (puzzle.hints.any { it.revealsAnswer }) errors += "${puzzle.id} has an answer-leaking hint"
    }

    private fun validatePuzzleReview(
        puzzle: Puzzle,
        requireProduction: Boolean,
        errors: MutableList<String>,
    ) {
        if (!puzzle.review.automatedReviewPassed) errors += "${puzzle.id} has not passed automated review"
        if (requireProduction && !puzzle.review.humanReviewed) {
            errors += "${puzzle.id} requires human review before production release"
        }
    }

    private fun validateWordly(
        puzzle: WordlyPuzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.solution.length != 5) errors += "${puzzle.id} solution must be five letters"
        if (puzzle.solution !in puzzle.validGuesses) errors += "${puzzle.id} solution must be a valid guess"
        checkSafeWords(puzzle.id, listOf(puzzle.solution) + puzzle.validGuesses, errors)
    }

    private fun validateSpelling(
        puzzle: SpellingBeePuzzle,
        errors: MutableList<String>,
    ) {
        val letters = puzzle.letters.map { it.lowercaseChar() }
        val centre = puzzle.centreLetter.lowercaseChar()
        if (letters.size != 7 || letters.toSet().size != 7) errors += "${puzzle.id} must have seven unique letters"
        if (centre !in letters) errors += "${puzzle.id} centre letter must be in letter set"
        if (puzzle.targetWords.size !in SPELLING_TARGET_RANGE) {
            errors += "${puzzle.id} must have 8 to 24 target words"
        }
        val targetTexts = puzzle.targetWords.map { it.word.lowercase() }
        if (targetTexts.toSet().size != targetTexts.size) errors += "${puzzle.id} target words must be unique"
        if (targetTexts.none { word -> letters.all { it in word } }) {
            errors += "${puzzle.id} should include an all-letter target word"
        }
        if (spellingScoreTotal(targetTexts, letters) <= 0) errors += "${puzzle.id} must have a positive score total"
        puzzle.targetWords.forEach { target ->
            val word = target.word.lowercase()
            if (target.definition.isBlank()) errors += "${puzzle.id} target definition is required: $word"
            if (word.length < 3) errors += "${puzzle.id} target word too short: $word"
            if (centre !in word) errors += "${puzzle.id} target word misses centre: $word"
            if (word.any { it !in letters }) errors += "${puzzle.id} target word uses invalid letters: $word"
        }
        checkSafeWords(puzzle.id, puzzle.targetWords.map { it.word }, errors)
    }

    private fun validateCrossword(
        puzzle: CrosswordPuzzle,
        errors: MutableList<String>,
    ) {
        val answers = puzzle.entries.map { it.answer.lowercase() }
        validateCrosswordShapeAndAnswers(puzzle, answers, errors)
        val grid = CrosswordValidationGrid()
        puzzle.entries.forEach { entry ->
            validateCrosswordEntry(puzzle, entry, grid, errors)
        }
        if (grid.entryCells.isNotEmpty() && !crosswordEntriesAreConnected(grid.entryCells)) {
            errors += "${puzzle.id} crossword entries must connect through crossings"
        }
        checkSafeWords(puzzle.id, puzzle.entries.map { it.answer }, errors)
    }

    private fun validateSudoku(
        puzzle: SudokuPuzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.givens.size != SUDOKU_CELL_COUNT) errors += "${puzzle.id} givens must contain $SUDOKU_CELL_COUNT cells"
        if (puzzle.solution.size != SUDOKU_CELL_COUNT) errors += "${puzzle.id} solution must contain $SUDOKU_CELL_COUNT cells"
        if (puzzle.givens.any { it !in 0..SUDOKU_SIZE }) errors += "${puzzle.id} givens values must be 0 through $SUDOKU_SIZE"
        if (puzzle.solution.any { it !in 1..6 }) errors += "${puzzle.id} solution values must be 1 through 6"
        if (!hasValidCompletedSudoku(puzzle.solution)) errors += "${puzzle.id} solution grid is invalid"
        if (!sudokuGivensMatchSolution(puzzle)) errors += "${puzzle.id} givens must match solution"
        if (puzzle.givens.count { it == 0 } == 0) errors += "${puzzle.id} must leave cells for the player"
        if (puzzle.givens.size == SUDOKU_CELL_COUNT && sudokuSolutionCount(puzzle.givens) != 1) {
            errors += "${puzzle.id} must have exactly one solution"
        }
        if (puzzle.givens.size == SUDOKU_CELL_COUNT && !sudokuSolvesWithDirectSingles(puzzle)) {
            errors += "${puzzle.id} must be solvable with starter-level singles"
        }
    }

    private fun validateConnections(
        puzzle: ConnectionsPuzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.groups.size != 4) errors += "${puzzle.id} must have four groups"
        puzzle.groups.forEach { group -> validateConnectionGroup(puzzle.id, group, errors) }
        val visibleWords = puzzle.groups.flatMap { it.words }
        if (visibleWords.size != 16 || visibleWords.toSet().size != 16) {
            errors += "${puzzle.id} must have sixteen unique visible words"
        }
        checkSafeWords(puzzle.id, visibleWords, errors)
    }

    private fun validateConnectionGroup(
        id: String,
        group: ConnectionGroup,
        errors: MutableList<String>,
    ) {
        if (group.title.isBlank()) errors += "$id connection group title is required"
        if (group.words.size != 4) errors += "$id connection group must contain four words"
        if (group.explanation.isBlank()) errors += "$id connection group explanation is required"
    }

    private fun hasValidCompletedSudoku(solution: List<Int>): Boolean {
        if (solution.size != SUDOKU_CELL_COUNT) return false
        return sudokuUnits().all { unit -> unit.map { solution[it] }.toSet() == SUDOKU_DIGITS }
    }

    private fun sudokuGivensMatchSolution(puzzle: SudokuPuzzle): Boolean =
        puzzle.givens.size == SUDOKU_CELL_COUNT &&
            puzzle.solution.size == SUDOKU_CELL_COUNT &&
            puzzle.givens.withIndex().all { (index, value) -> value == 0 || value == puzzle.solution[index] }

    private fun sudokuSolvesWithDirectSingles(puzzle: SudokuPuzzle): Boolean {
        val board = puzzle.givens.toMutableList()
        var changed: Boolean
        do {
            changed = false
            board.indices.filter { board[it] == 0 }.forEach { index ->
                val candidates = sudokuCandidates(board, index)
                if (candidates.size == 1) {
                    board[index] = candidates.single()
                    changed = true
                }
            }
        } while (changed)
        return board == puzzle.solution
    }

    private fun sudokuSolutionCount(
        givens: List<Int>,
        limit: Int = 2,
    ): Int {
        val board = givens.toMutableList()
        return countSudokuSolutions(board, limit)
    }

    private fun countSudokuSolutions(
        board: MutableList<Int>,
        limit: Int,
    ): Int {
        val index =
            board.indices
                .filter { board[it] == 0 }
                .minByOrNull { sudokuCandidates(board, it).size }
        var count = 0
        if (index == null) {
            count = 1
        } else {
            val candidates = sudokuCandidates(board, index)
            for (candidate in candidates) {
                if (count < limit) {
                    board[index] = candidate
                    count += countSudokuSolutions(board, limit - count)
                    board[index] = 0
                }
            }
        }
        return count
    }

    private fun sudokuCandidates(
        board: List<Int>,
        index: Int,
    ): Set<Int> {
        val used = sudokuPeers(index).map { board[it] }.filter { it > 0 }.toSet()
        return SUDOKU_DIGITS - used
    }

    private fun sudokuUnits(): List<List<Int>> = sudokuRows() + sudokuColumns() + sudokuRegions()

    private fun sudokuRows(): List<List<Int>> =
        (0 until SUDOKU_SIZE).map { row ->
            (0 until SUDOKU_SIZE).map { column -> row * SUDOKU_SIZE + column }
        }

    private fun sudokuColumns(): List<List<Int>> =
        (0 until SUDOKU_SIZE).map { column ->
            (0 until SUDOKU_SIZE).map { row -> row * SUDOKU_SIZE + column }
        }

    private fun sudokuRegions(): List<List<Int>> =
        (0 until SUDOKU_SIZE step SUDOKU_REGION_ROWS).flatMap { rowStart ->
            (0 until SUDOKU_SIZE step SUDOKU_REGION_COLUMNS).map { columnStart ->
                (rowStart until rowStart + SUDOKU_REGION_ROWS).flatMap { row ->
                    (columnStart until columnStart + SUDOKU_REGION_COLUMNS).map { column ->
                        row * SUDOKU_SIZE + column
                    }
                }
            }
        }

    private fun sudokuPeers(index: Int): Set<Int> =
        sudokuUnits()
            .filter { index in it }
            .flatten()
            .filterNot { it == index }
            .toSet()

    private fun validateCrosswordShapeAndAnswers(
        puzzle: CrosswordPuzzle,
        answers: List<String>,
        errors: MutableList<String>,
    ) {
        if (puzzle.width != 7 || puzzle.height != 7) errors += "${puzzle.id} must be a 7x7 grid"
        if (puzzle.entries.size !in CROSSWORD_ENTRY_RANGE) errors += "${puzzle.id} must have 6 to 14 crossword entries"
        if (answers.toSet().size != answers.size) errors += "${puzzle.id} crossword answers must be unique"
    }

    private fun validateCrosswordEntry(
        puzzle: CrosswordPuzzle,
        entry: CrosswordEntry,
        grid: CrosswordValidationGrid,
        errors: MutableList<String>,
    ) {
        val answer = entry.answer.lowercase()
        if (answer.length < 3) errors += "${puzzle.id} crossword answer too short: ${entry.answer}"
        if (answer.any { !it.isLetter() }) errors += "${puzzle.id} crossword answer must use letters only: ${entry.answer}"
        if (entry.clue.isBlank()) errors += "${puzzle.id} crossword clue is required: ${entry.answer}"
        if (entry.clue.lowercase().contains(answer)) errors += "${puzzle.id} crossword clue leaks answer: ${entry.answer}"
        if (!crosswordEntryFits(puzzle, entry)) errors += "${puzzle.id} crossword answer out of bounds: ${entry.answer}"
        recordCrosswordCells(puzzle.id, entry, answer, grid, errors)
    }

    private fun crosswordEntryFits(
        puzzle: CrosswordPuzzle,
        entry: CrosswordEntry,
    ): Boolean {
        val rowEnd = entry.row + if (entry.direction == CrosswordDirection.DOWN) entry.answer.lastIndex else 0
        val columnEnd = entry.column + if (entry.direction == CrosswordDirection.ACROSS) entry.answer.lastIndex else 0
        return rowEnd in 0 until puzzle.height && columnEnd in 0 until puzzle.width
    }

    private fun recordCrosswordCells(
        id: String,
        entry: CrosswordEntry,
        answer: String,
        grid: CrosswordValidationGrid,
        errors: MutableList<String>,
    ) {
        val cells = crosswordEntryCells(entry)
        grid.entryCells += cells.toSet()
        cells.forEachIndexed { index, cell ->
            val letter = answer[index]
            val existing = grid.occupied[cell]
            if (existing != null && existing != letter) {
                errors += "$id crossword crossing conflict at $cell"
            }
            grid.occupied[cell] = letter
        }
    }

    private fun crosswordEntryCells(entry: CrosswordEntry): List<Pair<Int, Int>> =
        entry.answer.indices.map { offset ->
            val row = entry.row + if (entry.direction == CrosswordDirection.DOWN) offset else 0
            val column = entry.column + if (entry.direction == CrosswordDirection.ACROSS) offset else 0
            row to column
        }

    private fun crosswordEntriesAreConnected(entryCells: List<Set<Pair<Int, Int>>>): Boolean {
        val seen = mutableSetOf(0)
        val pending = ArrayDeque<Int>()
        pending += 0
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            entryCells.forEachIndexed { index, cells ->
                if (index !in seen && entryCells[current].intersect(cells).isNotEmpty()) {
                    seen += index
                    pending += index
                }
            }
        }
        return seen.size == entryCells.size
    }

    private fun checkSafeWords(
        id: String,
        words: List<String>,
        errors: MutableList<String>,
    ) {
        val unsafe = words.map { it.lowercase() }.filter { it in prohibitedWords }
        if (unsafe.isNotEmpty()) errors += "$id contains prohibited words: $unsafe"
    }

    private fun spellingScoreTotal(
        words: List<String>,
        letters: List<Char>,
    ): Int =
        words.sumOf { word ->
            val base = if (word.length <= 4) 1 else word.length
            val bonus = if (letters.all { it in word }) 7 else 0
            base + bonus
        }

    private data class CrosswordValidationGrid(
        val occupied: MutableMap<Pair<Int, Int>, Char> = mutableMapOf(),
        val entryCells: MutableList<Set<Pair<Int, Int>>> = mutableListOf(),
    )

    private companion object {
        const val SEASON_LENGTH = 365
        const val SUDOKU_SIZE = 6
        const val SUDOKU_CELL_COUNT = 36
        const val SUDOKU_REGION_ROWS = 2
        const val SUDOKU_REGION_COLUMNS = 3
        val SPELLING_TARGET_RANGE = 8..24
        val CROSSWORD_ENTRY_RANGE = 6..14
        val SUDOKU_DIGITS = (1..SUDOKU_SIZE).toSet()

        val DEFAULT_PROHIBITED_WORDS =
            setOf(
                "bet",
                "drug",
                "gun",
                "kill",
                "war",
            )
    }
}

data class ValidationReport(
    val errors: List<String>,
) {
    val passed: Boolean = errors.isEmpty()
}
