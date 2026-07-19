package com.dailyquestkids.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PuzzlePack(
    val schemaVersion: Int,
    val seasonVersion: String,
    val seasonStartDateUtc: String,
    val checksum: String,
    val days: List<DailyPuzzleSet>,
)

@Serializable
data class DailyPuzzleSet(
    val dayIndex: Int,
    val globalDayNumber: Int,
    val puzzles: List<Puzzle>,
)

@Serializable
sealed interface Puzzle {
    val id: String
    val category: PuzzleCategory
    val difficulty: Difficulty
    val curriculumTags: List<String>
    val hints: List<Hint>
    val review: ReviewMetadata
}

@Serializable
enum class PuzzleCategory(
    val displayName: String,
    val destinationName: String,
) {
    WORDLY("Wordly", "Letter Garden"),
    SPELLING_B("Spelling B", "Honeycomb Library"),
    CROSSWORD("Crossword", "Clue Castle"),
    SUDOKU("Sudoku", "Number Temple"),
    CONNECTIONS("Connections", "Link Laboratory"),
}

@Serializable
enum class Difficulty {
    STARTER,
    EXPLORER,
    THINKER,
    CHALLENGE,
}

@Serializable
enum class PuzzleStatus(
    val label: String,
) {
    NOT_STARTED("Not started"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed"),
    FAILED("Try again tomorrow"),
    REVEALED("Revealed"),
    LOCKED("Locked"),
}

@Serializable
data class Hint(
    val order: Int,
    val text: String,
    val revealsAnswer: Boolean = false,
)

@Serializable
data class ReviewMetadata(
    val automatedReviewPassed: Boolean,
    val humanReviewed: Boolean,
    val reviewer: String? = null,
    val notes: String? = null,
)

@Serializable
@SerialName("wordly")
data class WordlyPuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    override val curriculumTags: List<String>,
    override val hints: List<Hint>,
    override val review: ReviewMetadata,
    val solution: String,
    val validGuesses: List<String>,
    val definition: String,
    val exampleSentence: String,
    val morphologyNote: String? = null,
) : Puzzle {
    override val category: PuzzleCategory = PuzzleCategory.WORDLY
}

@Serializable
@SerialName("spelling_b")
data class SpellingBeePuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    override val curriculumTags: List<String>,
    override val hints: List<Hint>,
    override val review: ReviewMetadata,
    val letters: List<Char>,
    val centreLetter: Char,
    val targetWords: List<SpellingWord>,
) : Puzzle {
    override val category: PuzzleCategory = PuzzleCategory.SPELLING_B
}

@Serializable
data class SpellingWord(
    val word: String,
    val definition: String,
)

@Serializable
@SerialName("crossword")
data class CrosswordPuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    override val curriculumTags: List<String>,
    override val hints: List<Hint>,
    override val review: ReviewMetadata,
    val width: Int,
    val height: Int,
    val entries: List<CrosswordEntry>,
) : Puzzle {
    override val category: PuzzleCategory = PuzzleCategory.CROSSWORD
}

@Serializable
data class CrosswordEntry(
    val answer: String,
    val clue: String,
    val row: Int,
    val column: Int,
    val direction: CrosswordDirection,
)

@Serializable
enum class CrosswordDirection {
    ACROSS,
    DOWN,
}

@Serializable
@SerialName("sudoku")
data class SudokuPuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    override val curriculumTags: List<String>,
    override val hints: List<Hint>,
    override val review: ReviewMetadata,
    val givens: List<Int>,
    val solution: List<Int>,
) : Puzzle {
    override val category: PuzzleCategory = PuzzleCategory.SUDOKU
}

@Serializable
@SerialName("connections")
data class ConnectionsPuzzle(
    override val id: String,
    override val difficulty: Difficulty,
    override val curriculumTags: List<String>,
    override val hints: List<Hint>,
    override val review: ReviewMetadata,
    val groups: List<ConnectionGroup>,
) : Puzzle {
    override val category: PuzzleCategory = PuzzleCategory.CONNECTIONS
}

@Serializable
data class ConnectionGroup(
    val title: String,
    val words: List<String>,
    val explanation: String,
)

@Serializable
data class CategoryProgress(
    val category: PuzzleCategory,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalSolved: Int,
    val lastSolvedDayIndex: Int?,
    val hintsUsed: Int,
    val hintFreeCompletions: Int,
)

@Serializable
data class DailyFiveProgress(
    val currentStreak: Int,
    val bestStreak: Int,
    val perfectDayCount: Int,
    val longestHistoricalStreak: Int,
    val completedDayIndices: Set<Int>,
)

@Serializable
data class ShareCardModel(
    val brand: String,
    val utcDate: String,
    val cardType: ShareCardType,
    val visibleResultPattern: String,
    val hintsUsed: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val forbiddenPayloads: List<String>,
)

@Serializable
enum class ShareCardType {
    INDIVIDUAL_RESULT,
    DAILY_FIVE,
    STREAK,
}
