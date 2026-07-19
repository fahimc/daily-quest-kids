package com.dailyquestkids.puzzle.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class WordlyScorerTest {
    @Test
    fun exactMatchMarksEveryLetterCorrect() {
        val result = WordlyScorer.score(solution = "plant", guess = "plant").map { it.state }

        assertEquals(List(5) { TileState.CORRECT }, result)
    }

    @Test
    fun repeatedLettersOnlyScoreAgainstAvailableSolutionLetters() {
        val result = WordlyScorer.score(solution = "civic", guess = "cocoa").map { it.state }

        assertEquals(
            listOf(
                TileState.CORRECT,
                TileState.ABSENT,
                TileState.PRESENT,
                TileState.ABSENT,
                TileState.ABSENT,
            ),
            result,
        )
    }

    @Test
    fun presentLettersAreAssignedAfterCorrectLetters() {
        val result = WordlyScorer.score(solution = "abbey", guess = "bobby").map { it.state }

        assertEquals(
            listOf(
                TileState.PRESENT,
                TileState.ABSENT,
                TileState.CORRECT,
                TileState.ABSENT,
                TileState.CORRECT,
            ),
            result,
        )
    }
}
