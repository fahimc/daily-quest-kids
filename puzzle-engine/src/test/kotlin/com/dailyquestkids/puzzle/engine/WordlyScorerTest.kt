package com.dailyquestkids.puzzle.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun exhaustiveSmallAlphabetScoringNeverOverUsesRepeatedLetters() {
        val words = generateWords(alphabet = listOf('a', 'b', 'c'), length = 5)

        words.forEach { solution ->
            words.forEach { guess ->
                val scores = WordlyScorer.score(solution = solution, guess = guess)

                listOf('a', 'b', 'c').forEach { letter ->
                    val scoredCount =
                        scores.count { score ->
                            score.letter == letter && score.state != TileState.ABSENT
                        }

                    assertTrue(
                        "$solution/$guess over-scored $letter: $scores",
                        scoredCount <= solution.count { it == letter },
                    )
                }

                scores.forEachIndexed { index, score ->
                    if (guess[index] == solution[index]) {
                        assertEquals(TileState.CORRECT, score.state)
                    }
                }
            }
        }
    }

    private fun generateWords(
        alphabet: List<Char>,
        length: Int,
    ): List<String> =
        if (length == 0) {
            listOf("")
        } else {
            generateWords(alphabet, length - 1).flatMap { prefix ->
                alphabet.map { letter -> prefix + letter }
            }
        }
}
