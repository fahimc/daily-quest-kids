package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.puzzle.engine.ShareSafety
import com.dailyquestkids.puzzle.engine.SudokuGameEngine
import com.dailyquestkids.puzzle.engine.SudokuMessage
import com.dailyquestkids.puzzle.engine.SudokuSaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SudokuUiMapperTest {
    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<SudokuPuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun initialStateMapsBoardBadgesAndSelection() {
        val ui =
            SudokuUiMapper.map(
                puzzle = puzzle,
                gameState = SudokuGameEngine.initial(puzzle),
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertEquals(36, ui.cells.size)
        assertEquals("30/36", ui.progressBadge)
        assertEquals("4", ui.hintBadge)
        assertEquals("R1 C1", ui.selectedLabel)
        assertEquals("Fill the 6x6 grid.", ui.statusText)
        assertEquals(30, ui.cells.count { it.isGiven })
        assertFalse(ui.pencilMode)
    }

    @Test
    fun notesConflictAndMistakeStateMapToCells() {
        val pencil = SudokuGameEngine.togglePencil(SudokuGameEngine.initial(puzzle))
        val noted = SudokuGameEngine.inputNumber(puzzle, pencil, 1, mistakeChecking = true).state
        val valueState = SudokuGameEngine.setPencilMode(noted, false)
        val duplicate = SudokuGameEngine.inputNumber(puzzle, valueState, puzzle.givens[1], mistakeChecking = true)

        val ui =
            SudokuUiMapper.map(
                puzzle = puzzle,
                gameState = duplicate.state,
                settings = QuestSettings(mistakeChecking = true),
                homeState = homeState,
                transientMessage = duplicate.message?.userText,
            )

        assertEquals(SudokuMessage.CONFLICT, duplicate.message)
        assertEquals("That number repeats in a row, column or box.", ui.statusText)
        assertTrue(ui.cells.single { it.row == 0 && it.column == 0 }.isConflict)
    }

    @Test
    fun routeStateKeepsEnteredNumberOptimisticUntilSavedStateCatchesUp() {
        val savedBeforeEntry = SudokuGameEngine.initial(puzzle)
        val entered = SudokuGameEngine.inputNumber(puzzle, savedBeforeEntry, puzzle.solution[0], mistakeChecking = true).state

        val merged = SudokuRouteStateReducer.mergeSavedState(entered, savedBeforeEntry)
        val ui =
            SudokuUiMapper.map(
                puzzle = puzzle,
                gameState = merged,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertEquals(entered.cellValues, merged.cellValues)
        assertEquals(puzzle.solution[0], ui.cells.single { it.row == 0 && it.column == 0 }.playerValue)
        assertEquals("31/36", ui.progressBadge)
    }

    @Test
    fun completedStateProvidesSafeSharePattern() {
        val state = completedState()

        val ui =
            SudokuUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertTrue(ui.isCompleted)
        assertNotNull(ui.sharePattern)
        assertNotNull(ui.shareCard)
        assertFalse(ShareSafety.leaksForbiddenPayload(ui.shareCard!!))
        puzzle.solution.chunked(6).forEach { row ->
            assertFalse(ui.sharePattern.orEmpty().contains(row.joinToString("")))
        }
    }

    @Test
    fun layoutMetricsFitCommonPhoneViewports() {
        val viewports =
            listOf(
                320f to 480f,
                320f to 568f,
                360f to 640f,
                393f to 851f,
                412f to 915f,
                480f to 853f,
            )

        viewports.forEach { (width, height) ->
            val metrics = SudokuLayoutCalculator.calculate(widthDp = width, heightDp = height)

            assertTrue(
                "$width x $height should not scroll; total height was ${metrics.totalPhoneHeight}",
                metrics.totalPhoneHeight <= height + 0.01f,
            )
            assertTrue("$width x $height content should fit width", metrics.contentWidth <= width)
            assertTrue("$width x $height cell should stay readable", metrics.cellSize >= 25f)
            assertTrue("$width x $height number buttons should be tappable", metrics.numberHeight >= 36f)
            if (height >= 640f) {
                assertTrue(
                    "$width x $height board should align to the game rail",
                    metrics.contentWidth - metrics.boardSize <= 0.01f,
                )
            }
        }
    }

    @Test
    fun tabletLayoutMetricsUseTwoPaneSizing() {
        val metrics = SudokuLayoutCalculator.calculate(widthDp = 800f, heightDp = 1024f)

        assertTrue(metrics.tablet)
        assertTrue(metrics.contentWidth <= 800f)
        assertTrue(metrics.boardSize >= 232f)
        assertTrue(metrics.cellSize >= 34f)
    }

    private fun completedState(): SudokuSaveState {
        var state = SudokuGameEngine.initial(puzzle)
        puzzle.givens.withIndex().filter { it.value == 0 }.forEach { (index, _) ->
            state = SudokuGameEngine.selectCell(puzzle, state, row = index / 6, column = index % 6)
            state = SudokuGameEngine.inputNumber(puzzle, state, puzzle.solution[index], mistakeChecking = true).state
        }
        return state
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
