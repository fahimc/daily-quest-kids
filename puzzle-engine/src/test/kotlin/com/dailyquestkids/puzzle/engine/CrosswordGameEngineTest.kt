package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrosswordGameEngineTest {
    private val puzzle = firstCrosswordPuzzle()

    @Test
    fun boardParsingNumbersEntriesAndPlayableCells() {
        val clues = CrosswordGameEngine.numberedEntries(puzzle)
        val board = CrosswordGameEngine.boardCells(puzzle, CrosswordGameEngine.initial(puzzle))
        val across = clues.single { it.direction == CrosswordDirection.ACROSS }
        val downNumbers = clues.filter { it.direction == CrosswordDirection.DOWN }.map { it.number }

        assertEquals(49, board.size)
        assertEquals(21, board.count { it.isPlayable })
        assertEquals(8, clues.size)
        assertEquals(1, across.number)
        assertEquals(7, across.answerLength)
        assertEquals((1..7).toList(), downNumbers)
        assertEquals('F', board.single { it.row == 3 && it.column == 0 }.solutionLetter)
        assertEquals('S', board.single { it.row == 3 && it.column == 6 }.solutionLetter)
    }

    @Test
    fun crossingsAgreeBetweenAcrossAndDownEntries() {
        val solution = CrosswordGameEngine.solutionCells(puzzle)
        val across = puzzle.entries.single { it.direction == CrosswordDirection.ACROSS }

        puzzle.entries
            .filter { it.direction == CrosswordDirection.DOWN }
            .forEach { entry ->
                val crossingIndex = entry.row * puzzle.width + entry.column

                assertEquals(entry.answer.first(), solution.getValue(crossingIndex))
                assertEquals(across.answer[entry.column], solution.getValue(crossingIndex))
            }
    }

    @Test
    fun typingAcrossMovesSelectionAndMarksAnswerComplete() {
        var state = CrosswordGameEngine.initial(puzzle)
        var result: CrosswordMoveResult? = null

        "flowers".forEach { letter ->
            val nextResult = CrosswordGameEngine.appendLetter(puzzle, state, letter)
            result = nextResult
            state = nextResult.state
        }

        assertEquals(CrosswordMessage.ANSWER_COMPLETE, result?.message)
        assertEquals(7, state.cellValues.size)
        assertEquals(3, state.activeRow)
        assertEquals(6, state.activeColumn)
        assertTrue(CrosswordGameEngine.clueStates(puzzle, state).single { it.clue.entryIndex == 0 }.isSolved)
    }

    @Test
    fun selectionDirectionSwitchingAndClueNavigationWork() {
        val initial = CrosswordGameEngine.initial(puzzle)
        val selectedDown = CrosswordGameEngine.selectCell(puzzle, initial, row = 3, column = 0)
        val toggledAcross = CrosswordGameEngine.toggleDirection(puzzle, selectedDown)
        val next = CrosswordGameEngine.nextEntry(puzzle, initial)
        val previous = CrosswordGameEngine.previousEntry(puzzle, initial)

        assertEquals(CrosswordDirection.DOWN, selectedDown.activeDirection)
        assertEquals(1, selectedDown.activeEntryIndex)
        assertEquals(CrosswordDirection.ACROSS, toggledAcross.activeDirection)
        assertEquals(0, toggledAcross.activeEntryIndex)
        assertEquals(1, next.activeEntryIndex)
        assertEquals(7, previous.activeEntryIndex)
    }

    @Test
    fun hintsRephraseCheckRevealLettersAndStopWhenExhausted() {
        val first = CrosswordGameEngine.revealHint(puzzle, CrosswordGameEngine.initial(puzzle))
        val second = CrosswordGameEngine.revealHint(puzzle, first.state)
        val locked = CrosswordGameEngine.appendLetter(puzzle, second.state, 'x')
        val third = CrosswordGameEngine.revealHint(puzzle, second.state)
        val fourth = CrosswordGameEngine.revealHint(puzzle, third.state)
        val exhausted = CrosswordGameEngine.revealHint(puzzle, fourth.state)
        val hints = CrosswordGameEngine.revealedHintTexts(puzzle, fourth.state)

        assertEquals(CrosswordMessage.CLUE_REPHRASED, first.message)
        assertEquals(CrosswordMessage.LETTER_REVEALED, second.message)
        assertEquals(CrosswordMessage.REVEALED_CELL_LOCKED, locked.message)
        assertEquals(CrosswordMessage.CHECK_TRY_AGAIN, third.message)
        assertEquals(CrosswordMessage.ANSWER_REVEALED, fourth.message)
        assertEquals(CrosswordMessage.NO_HINTS_LEFT, exhausted.message)
        assertEquals(listOf(1, 2, 3, 4), fourth.state.revealedHintOrders)
        assertTrue(second.state.revealedCellIndices.isNotEmpty())
        assertTrue(CrosswordGameEngine.clueStates(puzzle, fourth.state).single { it.clue.entryIndex == 0 }.isSolved)
        assertEquals(4, hints.size)
    }

    @Test
    fun saveAndRestorePreservesCellsSelectionHintsAndRevealedLetters() {
        val hinted = CrosswordGameEngine.revealHint(puzzle, CrosswordGameEngine.initial(puzzle)).state
        val withLetter = CrosswordGameEngine.appendLetter(puzzle, hinted, 'f').state
        val selected = CrosswordGameEngine.selectEntry(puzzle, withLetter, entryIndex = 3)

        val restored = CrosswordGameEngine.decode(CrosswordGameEngine.encode(selected))

        assertEquals(selected.cellValues, restored.cellValues)
        assertEquals(selected.activeEntryIndex, restored.activeEntryIndex)
        assertEquals(selected.activeDirection, restored.activeDirection)
        assertEquals(listOf(1), restored.revealedHintOrders)
    }

    @Test
    fun completingEveryEntryEmitsCompletionOnce() {
        var state = CrosswordGameEngine.initial(puzzle)
        var finalResult: CrosswordMoveResult? = null
        puzzle.entries.forEachIndexed { index, entry ->
            state = CrosswordGameEngine.selectEntry(puzzle, state, index)
            entry.answer.forEach { letter ->
                val nextResult = CrosswordGameEngine.appendLetter(puzzle, state, letter)
                finalResult = nextResult
                state = nextResult.state
            }
        }

        assertTrue(state.isCompleted)
        assertNotNull(finalResult?.completionEvent)
        assertNotNull(CrosswordGameEngine.pendingCompletion(state))

        val acknowledged = CrosswordGameEngine.acknowledgeCompletion(state)
        val afterFinished = CrosswordGameEngine.appendLetter(puzzle, acknowledged, 'a')

        assertNull(CrosswordGameEngine.pendingCompletion(acknowledged))
        assertNull(afterFinished.completionEvent)
        assertEquals(CrosswordMessage.ALREADY_FINISHED, afterFinished.message)
    }

    @Test
    fun shareModelDoesNotLeakAnswers() {
        val state = type("flowers", CrosswordGameEngine.initial(puzzle))

        val share =
            CrosswordGameEngine.shareCard(
                puzzle = puzzle,
                state = state,
                utcDate = "2026-07-19",
                currentStreak = 1,
                bestStreak = 3,
            )

        assertFalse(ShareSafety.leaksForbiddenPayload(share))
        assertTrue(share.visibleResultPattern.contains("Crossword 001"))
        assertTrue(share.visibleResultPattern.contains("Clues solved"))
        assertTrue(share.visibleResultPattern.contains("Filled"))
        assertTrue(share.visibleResultPattern.contains("Hints 0"))
        assertTrue(share.visibleResultPattern.contains("Streak 1"))
        puzzle.entries.forEach { entry ->
            assertTrue(share.forbiddenPayloads.contains(entry.answer))
            assertFalse(share.visibleResultPattern.contains(entry.answer, ignoreCase = true))
        }
    }

    @Test
    fun phasePreviewIncludesTwentyReviewedCrosswordFixtures() {
        val crosswords =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<CrosswordPuzzle>().single() }

        assertTrue(crosswords.size >= 20)
        assertTrue(crosswords.all { it.review.humanReviewed })
        assertTrue(crosswords.map { it.entries.first().answer }.toSet().size >= 20)
        assertTrue(crosswords.all { it.width == 7 && it.height == 7 })
        assertTrue(crosswords.all { it.entries.size == 8 })
        assertTrue(crosswords.all { puzzle -> puzzle.entries.all { it.answer.length >= 3 } })
    }

    private fun type(
        text: String,
        state: CrosswordSaveState,
    ): CrosswordSaveState =
        text.fold(state) { current, letter ->
            CrosswordGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun firstCrosswordPuzzle(): CrosswordPuzzle =
        FixturePackFactory
            .phasePreviewPack()
            .days
            .first()
            .puzzles
            .filterIsInstance<CrosswordPuzzle>()
            .single()
}
