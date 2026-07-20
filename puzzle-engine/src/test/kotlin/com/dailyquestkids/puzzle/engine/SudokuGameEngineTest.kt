package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuGameEngineTest {
    private val puzzle = firstSudokuPuzzle()

    @Test
    fun initialBoardMapsGivensSelectionAndCandidates() {
        val state = SudokuGameEngine.initial(puzzle)
        val cells = SudokuGameEngine.boardCells(puzzle, state)
        val selected = cells.single { it.isSelected }

        assertEquals(36, cells.size)
        assertEquals(30, cells.count { it.givenValue != null })
        assertEquals(6, cells.count { it.givenValue == null })
        assertEquals(0, selected.index)
        assertEquals(setOf(puzzle.solution[0]), SudokuGameEngine.candidatesFor(puzzle, state, 0))
        assertTrue(cells.any { it.isPeer })
    }

    @Test
    fun rowColumnAndRegionConflictsAreReported() {
        val duplicate = puzzle.givens[1]
        val result = SudokuGameEngine.inputNumber(puzzle, SudokuGameEngine.initial(puzzle), duplicate, mistakeChecking = true)
        val cells = SudokuGameEngine.boardCells(puzzle, result.state)

        assertEquals(SudokuMessage.CONFLICT, result.message)
        assertTrue(cells.single { it.index == 0 }.isConflict)
        assertTrue(cells.single { it.index == 1 }.isConflict)
    }

    @Test
    fun mistakeCheckingCanWarnWithoutBlockingEntry() {
        val openPuzzle = puzzle.copy(givens = List(36) { 0 })
        val wrongNumber = (1..6).first { it != openPuzzle.solution[0] }
        val result = SudokuGameEngine.inputNumber(openPuzzle, SudokuGameEngine.initial(openPuzzle), wrongNumber, mistakeChecking = true)

        assertEquals(SudokuMessage.MISTAKE, result.message)
        assertEquals(wrongNumber, result.state.cellValues[0])
        assertTrue(SudokuGameEngine.boardCells(openPuzzle, result.state).single { it.index == 0 }.isMistake)
    }

    @Test
    fun pencilNotesToggleAndEraseClearsCellState() {
        val pencil = SudokuGameEngine.togglePencil(SudokuGameEngine.initial(puzzle))
        val noted = SudokuGameEngine.inputNumber(puzzle, pencil, 1, mistakeChecking = true)
        val removed = SudokuGameEngine.inputNumber(puzzle, noted.state, 1, mistakeChecking = true)
        val notedAgain = SudokuGameEngine.inputNumber(puzzle, removed.state, 2, mistakeChecking = true)
        val erased = SudokuGameEngine.erase(puzzle, notedAgain.state)

        assertEquals(SudokuMessage.NOTE_UPDATED, noted.message)
        assertEquals(listOf(1), noted.state.notes[0])
        assertFalse(removed.state.notes.containsKey(0))
        assertEquals(listOf(2), notedAgain.state.notes[0])
        assertFalse(erased.notes.containsKey(0))
    }

    @Test
    fun undoAndRedoRestoreValuesAndNotes() {
        val enteredResult =
            SudokuGameEngine.inputNumber(
                puzzle = puzzle,
                state = SudokuGameEngine.initial(puzzle),
                number = puzzle.solution[0],
                mistakeChecking = true,
            )
        val entered = enteredResult.state
        val undone = SudokuGameEngine.undo(entered)
        val redone = SudokuGameEngine.redo(undone)

        assertFalse(undone.cellValues.containsKey(0))
        assertEquals(puzzle.solution[0], redone.cellValues[0])
        assertTrue(redone.history.isNotEmpty())
    }

    @Test
    fun hintsRevealInOrderAndPlaceAValidNumber() {
        val first = SudokuGameEngine.revealHint(puzzle, SudokuGameEngine.initial(puzzle))
        val second = SudokuGameEngine.revealHint(puzzle, first.state)
        val third = SudokuGameEngine.revealHint(puzzle, second.state)
        val fourth = SudokuGameEngine.revealHint(puzzle, third.state)
        val exhausted = SudokuGameEngine.revealHint(puzzle, fourth.state)
        val revealedIndex = fourth.state.revealedCellIndices.single()

        assertEquals(SudokuMessage.AREA_HIGHLIGHTED, first.message)
        assertEquals(SudokuMessage.CANDIDATES_EXPLAINED, second.message)
        assertEquals(SudokuMessage.SINGLE_FOUND, third.message)
        assertEquals(SudokuMessage.NUMBER_REVEALED, fourth.message)
        assertEquals(SudokuMessage.NO_HINTS_LEFT, exhausted.message)
        assertEquals(puzzle.solution[revealedIndex], fourth.state.cellValues[revealedIndex])
        assertEquals(4, SudokuGameEngine.revealedHintTexts(puzzle, fourth.state).size)
    }

    @Test
    fun saveAndRestorePreservesValuesNotesSelectionHintsAndHistory() {
        val hinted = SudokuGameEngine.revealHint(puzzle, SudokuGameEngine.initial(puzzle)).state
        val selected = SudokuGameEngine.selectCell(puzzle, hinted, row = 1, column = 1)
        val pencil = SudokuGameEngine.togglePencil(selected)
        val noted = SudokuGameEngine.inputNumber(puzzle, pencil, 3, mistakeChecking = true).state

        val restored = SudokuGameEngine.decode(SudokuGameEngine.encode(noted))

        assertEquals(noted.notes, restored.notes)
        assertEquals(noted.selectedIndex, restored.selectedIndex)
        assertEquals(noted.pencilMode, restored.pencilMode)
        assertEquals(listOf(1), restored.revealedHintOrders)
        assertEquals(noted.history.size, restored.history.size)
    }

    @Test
    fun completingAllBlankCellsEmitsCompletionOnce() {
        var state = SudokuGameEngine.initial(puzzle)
        var finalResult: SudokuMoveResult? = null
        blankIndices().forEach { index ->
            state = SudokuGameEngine.selectCell(puzzle, state, row = index / 6, column = index % 6)
            val result = SudokuGameEngine.inputNumber(puzzle, state, puzzle.solution[index], mistakeChecking = true)
            finalResult = result
            state = result.state
        }

        assertTrue(state.isCompleted)
        assertNotNull(finalResult?.completionEvent)
        assertNotNull(SudokuGameEngine.pendingCompletion(state))

        val acknowledged = SudokuGameEngine.acknowledgeCompletion(state)
        val afterFinished = SudokuGameEngine.inputNumber(puzzle, acknowledged, 1, mistakeChecking = true)

        assertNull(SudokuGameEngine.pendingCompletion(acknowledged))
        assertNull(afterFinished.completionEvent)
        assertEquals(SudokuMessage.ALREADY_FINISHED, afterFinished.message)
    }

    @Test
    fun shareModelDoesNotLeakSolutionRows() {
        val state = SudokuGameEngine.inputNumber(puzzle, SudokuGameEngine.initial(puzzle), puzzle.solution[0], mistakeChecking = true).state

        val share =
            SudokuGameEngine.shareCard(
                puzzle = puzzle,
                state = state,
                utcDate = "2026-07-19",
                currentStreak = 1,
                bestStreak = 3,
            )

        assertFalse(ShareSafety.leaksForbiddenPayload(share))
        assertTrue(share.visibleResultPattern.contains("Sudoku 001"))
        assertTrue(share.visibleResultPattern.contains("Filled"))
        assertTrue(share.visibleResultPattern.contains("Mistakes"))
        assertTrue(share.visibleResultPattern.contains("Hints 0"))
        assertTrue(share.visibleResultPattern.contains("Streak 1"))
        puzzle.solution.chunked(6).forEach { row ->
            assertFalse(share.visibleResultPattern.contains(row.joinToString("")))
        }
    }

    @Test
    fun phasePreviewIncludesTwentyReviewedSudokuFixtures() {
        val sudokus =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<SudokuPuzzle>().single() }

        assertTrue(sudokus.size >= 20)
        assertTrue(sudokus.all { it.review.humanReviewed })
        assertTrue(sudokus.map { it.solution.joinToString("") }.toSet().size >= 20)
        assertTrue(sudokus.all { it.givens.size == 36 && it.solution.size == 36 })
        assertTrue(sudokus.all { it.givens.count { value -> value == 0 } == 6 })
    }

    private fun blankIndices(): List<Int> {
        val blanks = mutableListOf<Int>()
        val givens = puzzle.givens
        givens.forEachIndexed { index, value ->
            if (value == 0) {
                blanks += index
            }
        }
        return blanks
    }

    private fun firstSudokuPuzzle(): SudokuPuzzle =
        FixturePackFactory
            .phasePreviewPack()
            .days
            .first()
            .puzzles
            .filterIsInstance<SudokuPuzzle>()
            .single()
}
