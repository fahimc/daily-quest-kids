package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle

object ContentReleaseAuditor {
    fun audit(pack: PuzzlePack): ContentReleaseAudit {
        val puzzles = pack.days.flatMap { it.puzzles }
        val categoryCounts =
            PuzzleCategory.entries.associate { category ->
                category.name to puzzles.count { puzzle -> puzzle.category == category }
            }
        val duplicateContentFingerprintCount =
            puzzles
                .groupingBy { puzzle -> puzzle.contentFingerprint() }
                .eachCount()
                .values
                .sumOf { count -> (count - 1).coerceAtLeast(0) }
        val humanReviewRequiredCount = puzzles.count { !it.review.humanReviewed }
        val blockers =
            buildList {
                if (pack.days.size != SEASON_LENGTH) add("Season must contain exactly $SEASON_LENGTH days.")
                val expectedPuzzleCount = SEASON_LENGTH * PuzzleCategory.entries.size
                if (puzzles.size != expectedPuzzleCount) {
                    add("Season must contain exactly $expectedPuzzleCount puzzles.")
                }
                categoryCounts
                    .filterValues { count -> count != SEASON_LENGTH }
                    .forEach { (category, count) -> add("$category must contain $SEASON_LENGTH puzzles; found $count.") }
                if (humanReviewRequiredCount > 0) {
                    add("$humanReviewRequiredCount puzzles require genuine human review before production release.")
                }
                if (duplicateContentFingerprintCount > 0) {
                    add("$duplicateContentFingerprintCount repeated content fingerprints require authoring replacement.")
                }
            }
        return ContentReleaseAudit(
            categoryCounts = categoryCounts,
            humanReviewRequiredCount = humanReviewRequiredCount,
            duplicateContentFingerprintCount = duplicateContentFingerprintCount,
            blockers = blockers,
        )
    }

    private fun Puzzle.contentFingerprint(): String =
        when (this) {
            is WordlyPuzzle -> "wordly:${solution.lowercase()}"
            is SpellingBeePuzzle ->
                "spelling:${letters.joinToString(separator = "").lowercase()}:$centreLetter:" +
                    targetWords.joinToString(separator = "|") { it.word.lowercase() }

            is CrosswordPuzzle ->
                "crossword:" +
                    entries
                        .sortedWith(compareBy({ it.row }, { it.column }, { it.direction.name }, { it.answer }))
                        .joinToString(separator = "|") { "${it.answer.lowercase()}@${it.row},${it.column},${it.direction}" }

            is SudokuPuzzle -> "sudoku:${givens.joinToString(separator = "")}:${solution.joinToString(separator = "")}"
            is ConnectionsPuzzle ->
                "connections:" +
                    groups
                        .sortedBy { it.title.lowercase() }
                        .joinToString(separator = "|") { group ->
                            "${group.title.lowercase()}=${group.words.sorted().joinToString(separator = "/") { it.lowercase() }}"
                        }
        }

    private const val SEASON_LENGTH = 365
}

data class ContentReleaseAudit(
    val categoryCounts: Map<String, Int>,
    val humanReviewRequiredCount: Int,
    val duplicateContentFingerprintCount: Int,
    val blockers: List<String>,
) {
    val releaseReady: Boolean = blockers.isEmpty()
}
