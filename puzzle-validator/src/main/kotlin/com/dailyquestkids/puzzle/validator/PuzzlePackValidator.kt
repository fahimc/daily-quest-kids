package com.dailyquestkids.puzzle.validator

import com.dailyquestkids.core.model.ConnectionGroup
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordDirection
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
        puzzle.targetWords.forEach { target ->
            val word = target.word.lowercase()
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
        if (puzzle.width != 7 || puzzle.height != 7) errors += "${puzzle.id} must be a 7x7 grid"
        puzzle.entries.forEach { entry ->
            if (entry.answer.length < 3) errors += "${puzzle.id} crossword answer too short: ${entry.answer}"
            val rowEnd = entry.row + if (entry.direction == CrosswordDirection.DOWN) entry.answer.lastIndex else 0
            val columnEnd = entry.column + if (entry.direction == CrosswordDirection.ACROSS) entry.answer.lastIndex else 0
            if (rowEnd !in 0 until puzzle.height || columnEnd !in 0 until puzzle.width) {
                errors += "${puzzle.id} crossword answer out of bounds: ${entry.answer}"
            }
        }
        checkSafeWords(puzzle.id, puzzle.entries.map { it.answer }, errors)
    }

    private fun validateSudoku(
        puzzle: SudokuPuzzle,
        errors: MutableList<String>,
    ) {
        if (puzzle.givens.size != SUDOKU_CELL_COUNT) errors += "${puzzle.id} givens must contain $SUDOKU_CELL_COUNT cells"
        if (puzzle.solution.size != SUDOKU_CELL_COUNT) errors += "${puzzle.id} solution must contain $SUDOKU_CELL_COUNT cells"
        if (puzzle.solution.any { it !in 1..6 }) errors += "${puzzle.id} solution values must be 1 through 6"
        if (!hasValidCompletedSudokuRows(puzzle.solution)) errors += "${puzzle.id} solution rows are invalid"
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

    private fun hasValidCompletedSudokuRows(solution: List<Int>): Boolean {
        if (solution.size != SUDOKU_CELL_COUNT) return false
        val expected = (1..6).toSet()
        return solution.chunked(6).all { row -> row.toSet() == expected }
    }

    private fun checkSafeWords(
        id: String,
        words: List<String>,
        errors: MutableList<String>,
    ) {
        val unsafe = words.map { it.lowercase() }.filter { it in prohibitedWords }
        if (unsafe.isNotEmpty()) errors += "$id contains prohibited words: $unsafe"
    }

    private companion object {
        const val SEASON_LENGTH = 365
        const val SUDOKU_CELL_COUNT = 36

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
