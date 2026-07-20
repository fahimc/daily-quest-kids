package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordlyGameEngineTest {
    private val puzzle = firstWordlyPuzzle()

    @Test
    fun invalidGuessesReturnMessagesWithoutRecordingAttempts() {
        val tooShort = WordlyGameEngine.submit(puzzle, type("abc"))
        val notInList = WordlyGameEngine.submit(puzzle, type("zzzzz"))

        assertEquals(WordlyMessage.TOO_SHORT, tooShort.message)
        assertEquals(emptyList<String>(), tooShort.state.attempts)
        assertEquals(WordlyMessage.INVALID_GUESS, notInList.message)
        assertEquals(emptyList<String>(), notInList.state.attempts)
    }

    @Test
    fun commonValidGuessesAreAcceptedAndRecorded() {
        val result = WordlyGameEngine.submit(puzzle, type("known"))

        assertEquals(WordlyMessage.GUESS_ACCEPTED, result.message)
        assertEquals(listOf("known"), result.state.attempts)
    }

    @Test
    fun attemptExhaustionFailsAndLocksFurtherInput() {
        var state = WordlyGameEngine.initial(puzzle)
        wrongGuesses(6).forEach { guess ->
            state = submitGuess(state, guess).state
        }

        assertTrue(state.isFailed)
        assertFalse(state.isCompleted)
        assertEquals(6, state.attempts.size)

        val afterFinished = WordlyGameEngine.appendLetter(puzzle, state, 'a')

        assertEquals(WordlyMessage.ALREADY_FINISHED, afterFinished.message)
        assertEquals(state, afterFinished.state)
    }

    @Test
    fun hintsRevealInOrderAndStopWhenAllAreOpen() {
        val first = WordlyGameEngine.revealHint(puzzle, WordlyGameEngine.initial(puzzle))
        val second = WordlyGameEngine.revealHint(puzzle, first.state)
        val third = WordlyGameEngine.revealHint(puzzle, second.state)
        val exhausted = WordlyGameEngine.revealHint(puzzle, third.state)

        assertEquals(listOf(1), first.state.revealedHintOrders)
        assertEquals(listOf(1, 2), second.state.revealedHintOrders)
        assertEquals(listOf(1, 2, 3), third.state.revealedHintOrders)
        assertEquals(WordlyMessage.NO_HINTS_LEFT, exhausted.message)
    }

    @Test
    fun saveAndRestorePreservesAttemptsInputHintsAndTerminalFlags() {
        val hinted = WordlyGameEngine.revealHint(puzzle, WordlyGameEngine.initial(puzzle)).state
        val attempted = submitGuess(hinted, wrongGuesses(1).single()).state
        val partial = type("fl", attempted)

        val restored = WordlyGameEngine.decode(WordlyGameEngine.encode(partial))

        assertEquals(attempted.attempts, restored.attempts)
        assertEquals("fl", restored.currentInput)
        assertEquals(listOf(1), restored.revealedHintOrders)
        assertFalse(restored.isCompleted)
        assertFalse(restored.isFailed)
    }

    @Test
    fun completionEventIsAcknowledgedOnce() {
        val solved = submitGuess(WordlyGameEngine.initial(puzzle), puzzle.solution)

        assertEquals(WordlyOutcome.SUCCESS, solved.completionEvent?.outcome)
        assertNotNull(WordlyGameEngine.pendingCompletion(solved.state))

        val acknowledged = WordlyGameEngine.acknowledgeCompletion(solved.state)
        val resubmitted = WordlyGameEngine.submit(puzzle, acknowledged)

        assertNull(WordlyGameEngine.pendingCompletion(acknowledged))
        assertNull(resubmitted.completionEvent)
        assertEquals(WordlyMessage.ALREADY_FINISHED, resubmitted.message)
    }

    @Test
    fun shareModelDoesNotLeakSolutionOrGuesses() {
        var state = submitGuess(WordlyGameEngine.initial(puzzle), wrongGuesses(1).single()).state
        state = submitGuess(state, puzzle.solution).state

        val share =
            WordlyGameEngine.shareCard(
                puzzle = puzzle,
                state = state,
                utcDate = "2026-07-19",
                currentStreak = 2,
                bestStreak = 4,
            )

        assertFalse(ShareSafety.leaksForbiddenPayload(share))
        assertTrue(share.visibleResultPattern.contains("Wordly 001 2/6"))
        assertTrue(share.visibleResultPattern.contains("Hints 0"))
        assertTrue(share.visibleResultPattern.contains("Streak 2"))
        assertTrue(share.visibleResultPattern.contains("correct"))
        assertTrue(share.forbiddenPayloads.contains(puzzle.solution))
        state.attempts.forEach { guess ->
            assertFalse(share.visibleResultPattern.contains(guess, ignoreCase = true))
        }
    }

    @Test
    fun solvingCanHappenOnAnyAttemptFromOneThroughSix() {
        (1..6).forEach { targetAttempt ->
            var state = WordlyGameEngine.initial(puzzle)
            wrongGuesses(targetAttempt - 1).forEach { guess ->
                state = submitGuess(state, guess).state
            }

            val result = submitGuess(state, puzzle.solution)

            assertTrue("attempt $targetAttempt should complete", result.state.isCompleted)
            assertEquals(targetAttempt, result.state.attempts.size)
            assertEquals(WordlyOutcome.SUCCESS, result.completionEvent?.outcome)
        }
    }

    @Test
    fun failureCanResumeFromSavedState() {
        var state = WordlyGameEngine.initial(puzzle)
        wrongGuesses(3).forEach { guess ->
            state = submitGuess(state, guess).state
        }

        state = WordlyGameEngine.decode(WordlyGameEngine.encode(state))

        wrongGuesses(6).drop(3).forEach { guess ->
            state = submitGuess(state, guess).state
        }

        assertTrue(state.isFailed)
        assertEquals(WordlyOutcome.FAILURE, WordlyGameEngine.pendingCompletion(state)?.outcome)
    }

    @Test
    fun keyboardStateKeepsBestKnownLetterResult() {
        var state = submitGuess(WordlyGameEngine.initial(puzzle), "plane").state
        state = submitGuess(state, puzzle.solution).state

        val keys = WordlyGameEngine.keyboardStates(puzzle, state)

        puzzle.solution.uppercase().forEach { letter ->
            assertEquals(TileState.CORRECT, keys[letter])
        }
        assertEquals(TileState.ABSENT, keys['P'])
    }

    private fun type(
        text: String,
        state: WordlySaveState = WordlyGameEngine.initial(puzzle),
    ): WordlySaveState =
        text.fold(state) { current, letter ->
            WordlyGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun submitGuess(
        state: WordlySaveState,
        guess: String,
    ): WordlyMoveResult = WordlyGameEngine.submit(puzzle, type(guess, state))

    private fun wrongGuesses(count: Int): List<String> = puzzle.validGuesses.filterNot { it == puzzle.solution }.take(count)

    private fun firstWordlyPuzzle(): WordlyPuzzle =
        FixturePackFactory
            .phasePreviewPack()
            .days
            .first()
            .puzzles
            .filterIsInstance<WordlyPuzzle>()
            .single()
}
