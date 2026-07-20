package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.ReviewMetadata
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory

object SeasonOneCandidateFactory {
    fun candidatePack(): PuzzlePack {
        val source = FixturePackFactory.phasePreviewPack(dayCount = SEASON_LENGTH)
        return source.copy(
            seasonVersion = "season-one-candidate",
            checksum = "season-one-candidate-checksum-pending",
            days =
                source.days.map { day ->
                    day.copy(puzzles = day.puzzles.map { puzzle -> puzzle.asSeasonOneCandidate() })
                },
        )
    }

    private fun Puzzle.asSeasonOneCandidate(): Puzzle =
        when (this) {
            is WordlyPuzzle -> copy(review = candidateReview())
            is SpellingBeePuzzle -> copy(review = candidateReview())
            is CrosswordPuzzle -> copy(review = candidateReview())
            is SudokuPuzzle -> copy(review = candidateReview())
            is ConnectionsPuzzle -> copy(review = candidateReview())
        }

    private fun candidateReview(): ReviewMetadata =
        ReviewMetadata(
            automatedReviewPassed = true,
            humanReviewed = false,
            reviewer = null,
            notes = "Phase 14 generated candidate. Requires genuine human content review before production release.",
        )

    private const val SEASON_LENGTH = 365
}
