package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingBGameEngineTest {
    private val puzzle = firstSpellingPuzzle()

    @Test
    fun invalidInputsReturnMessagesWithoutRecordingWords() {
        val invalidLetter = SpellingBGameEngine.appendLetter(puzzle, SpellingBGameEngine.initial(puzzle), 'z')
        val tooShort = SpellingBGameEngine.submit(puzzle, type("an"))
        val missingCentre = SpellingBGameEngine.submit(puzzle, type("pen"))
        val notInList = SpellingBGameEngine.submit(puzzle, type("trap"))

        assertEquals(SpellingBMessage.INVALID_LETTER, invalidLetter.message)
        assertEquals(SpellingBMessage.TOO_SHORT, tooShort.message)
        assertEquals(SpellingBMessage.MISSING_CENTRE, missingCentre.message)
        assertEquals(SpellingBMessage.NOT_IN_LIST, notInList.message)
        assertEquals(emptyList<String>(), invalidLetter.state.foundWords)
        assertEquals(emptyList<String>(), tooShort.state.foundWords)
        assertEquals(emptyList<String>(), missingCentre.state.foundWords)
        assertEquals(emptyList<String>(), notInList.state.foundWords)
    }

    @Test
    fun validWordsScoreAndDuplicatesAreRejected() {
        val first = submitWord(SpellingBGameEngine.initial(puzzle), "plant")
        val duplicate = submitWord(first.state, "plant")

        assertEquals(SpellingBMessage.WORD_FOUND, first.message)
        assertEquals(listOf("plant"), first.state.foundWords)
        assertEquals(5, SpellingBGameEngine.score(puzzle, first.state))
        assertEquals(SpellingBMessage.ALREADY_FOUND, duplicate.message)
        assertEquals(listOf("plant"), duplicate.state.foundWords)
    }

    @Test
    fun allLetterWordsReceiveBonus() {
        val result = submitWord(SpellingBGameEngine.initial(puzzle), "planter")

        assertEquals(SpellingBMessage.PANGRAM_FOUND, result.message)
        assertEquals(14, SpellingBGameEngine.scoreForWord(puzzle, "planter"))
        assertEquals(14, SpellingBGameEngine.score(puzzle, result.state))
        assertTrue(SpellingBGameEngine.foundWordRows(puzzle, result.state).single().isPangram)
    }

    @Test
    fun hintsRevealInOrderAndStopWhenAllAreOpen() {
        val first = SpellingBGameEngine.revealHint(puzzle, SpellingBGameEngine.initial(puzzle))
        val second = SpellingBGameEngine.revealHint(puzzle, first.state)
        val third = SpellingBGameEngine.revealHint(puzzle, second.state)
        val fourth = SpellingBGameEngine.revealHint(puzzle, third.state)
        val exhausted = SpellingBGameEngine.revealHint(puzzle, fourth.state)
        val hints = SpellingBGameEngine.revealedHintTexts(puzzle, fourth.state)

        assertEquals(listOf(1), first.state.revealedHintOrders)
        assertEquals(listOf(1, 2), second.state.revealedHintOrders)
        assertEquals(listOf(1, 2, 3), third.state.revealedHintOrders)
        assertEquals(listOf(1, 2, 3, 4), fourth.state.revealedHintOrders)
        assertEquals(SpellingBMessage.NO_HINTS_LEFT, exhausted.message)
        assertEquals(4, hints.size)
        assertTrue(hints.first().startsWith("Starts:"))
    }

    @Test
    fun saveAndRestorePreservesFoundWordsInputHintsAndShuffle() {
        val hinted = SpellingBGameEngine.revealHint(puzzle, SpellingBGameEngine.initial(puzzle)).state
        val found = submitWord(hinted, "plant").state
        val partial = type("pla", found)
        val shuffled = SpellingBGameEngine.shuffle(puzzle, partial)

        val restored = SpellingBGameEngine.decode(SpellingBGameEngine.encode(shuffled))

        assertEquals(listOf("plant"), restored.foundWords)
        assertEquals("pla", restored.currentInput)
        assertEquals(listOf(1), restored.revealedHintOrders)
        assertNotEquals(SpellingBGameEngine.initial(puzzle).shuffledOuterLetters, restored.shuffledOuterLetters)
    }

    @Test
    fun completingEveryTargetEmitsCompletionOnce() {
        var state = SpellingBGameEngine.initial(puzzle)
        var finalResult: SpellingBMoveResult? = null
        puzzle.targetWords.forEach { target ->
            val result = submitWord(state, target.word)
            finalResult = result
            state = result.state
        }

        assertTrue(state.isCompleted)
        assertEquals(puzzle.targetWords.size, state.foundWords.size)
        assertNotNull(finalResult?.completionEvent)
        assertNotNull(SpellingBGameEngine.pendingCompletion(state))

        val acknowledged = SpellingBGameEngine.acknowledgeCompletion(state)
        val afterFinished = submitWord(acknowledged, puzzle.targetWords.first().word)

        assertNull(SpellingBGameEngine.pendingCompletion(acknowledged))
        assertNull(afterFinished.completionEvent)
        assertEquals(SpellingBMessage.ALREADY_FINISHED, afterFinished.message)
    }

    @Test
    fun shareModelDoesNotLeakAnswers() {
        val state = submitWord(SpellingBGameEngine.initial(puzzle), "planter").state

        val share =
            SpellingBGameEngine.shareCard(
                puzzle = puzzle,
                state = state,
                utcDate = "2026-07-19",
                currentStreak = 1,
                bestStreak = 3,
            )

        assertFalse(ShareSafety.leaksForbiddenPayload(share))
        assertTrue(share.visibleResultPattern.contains("Spelling B 001"))
        assertTrue(share.visibleResultPattern.contains("Points"))
        assertTrue(share.visibleResultPattern.contains("Found"))
        assertTrue(share.visibleResultPattern.contains("Hints 0"))
        assertTrue(share.visibleResultPattern.contains("Streak 1"))
        assertTrue(share.forbiddenPayloads.contains("planter"))
        puzzle.targetWords.forEach { target ->
            assertFalse(share.visibleResultPattern.contains(target.word, ignoreCase = true))
        }
    }

    @Test
    fun phasePreviewIncludesTwentySpellingFixturesWithValidTargets() {
        val spellingPuzzles =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<SpellingBeePuzzle>().single() }

        assertTrue(spellingPuzzles.size >= 20)
        assertTrue(spellingPuzzles.all { it.review.humanReviewed })
        assertTrue(spellingPuzzles.map { it.letters.joinToString("") }.toSet().size >= 20)
        assertTrue(spellingPuzzles.all { it.targetWords.size in 8..24 })
        assertTrue(spellingPuzzles.all { puzzle -> puzzle.targetWords.any { word -> puzzle.letters.all { it in word.word } } })
    }

    private fun type(
        text: String,
        state: SpellingBSaveState = SpellingBGameEngine.initial(puzzle),
    ): SpellingBSaveState =
        text.fold(state) { current, letter ->
            SpellingBGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun submitWord(
        state: SpellingBSaveState,
        word: String,
    ): SpellingBMoveResult = SpellingBGameEngine.submit(puzzle, type(word, state))

    private fun firstSpellingPuzzle(): SpellingBeePuzzle =
        FixturePackFactory
            .phasePreviewPack()
            .days
            .first()
            .puzzles
            .filterIsInstance<SpellingBeePuzzle>()
            .single()
}
