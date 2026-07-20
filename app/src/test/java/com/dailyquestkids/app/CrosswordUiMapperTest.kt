package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.puzzle.engine.CrosswordGameEngine
import com.dailyquestkids.puzzle.engine.CrosswordMessage
import com.dailyquestkids.puzzle.engine.CrosswordSaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CrosswordUiMapperTest {
    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<CrosswordPuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun initialStateMapsBoardCluesAndTopBadges() {
        val ui =
            CrosswordUiMapper.map(
                puzzle = puzzle,
                gameState = CrosswordGameEngine.initial(puzzle),
                settings = QuestSettings(largePuzzleText = true),
                homeState = homeState,
                transientMessage = null,
            )

        assertEquals(49, ui.cells.size)
        assertEquals(1, ui.cluesAcross.size)
        assertEquals(7, ui.cluesDown.size)
        assertEquals("0/${puzzle.entries.size}", ui.progressBadge)
        assertEquals("4", ui.hintBadge)
        assertEquals("Across", ui.directionLabel)
        assertEquals("1 Across (7)", ui.activeClueLabel)
        assertEquals("Fill the crossword.", ui.statusText)
        assertTrue(ui.largeText)
    }

    @Test
    fun typedAnswerMapsLettersAndImmediateProgress() {
        val result = type("flowers", CrosswordGameEngine.initial(puzzle))
        val ui =
            CrosswordUiMapper.map(
                puzzle = puzzle,
                gameState = result,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = CrosswordMessage.ANSWER_COMPLETE.userText,
            )
        val centreRow = ui.cells.filter { it.row == 3 }.take(7)

        assertEquals("FLOWERS", centreRow.mapNotNull { it.playerLetter }.joinToString(""))
        assertEquals("1/${puzzle.entries.size}", ui.progressBadge)
        assertEquals("Answer complete.", ui.statusText)
        assertTrue(ui.cluesAcross.single().isSolved)
    }

    @Test
    fun routeStateKeepsTypedCellsOptimisticUntilSavedStateCatchesUp() {
        val savedBeforeType = CrosswordGameEngine.initial(puzzle)
        val typed = CrosswordGameEngine.appendLetter(puzzle, savedBeforeType, 'f').state

        val merged = CrosswordRouteStateReducer.mergeSavedState(typed, savedBeforeType)
        val ui =
            CrosswordUiMapper.map(
                puzzle = puzzle,
                gameState = merged,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertEquals(typed.cellValues, merged.cellValues)
        assertEquals('F', ui.cells.single { it.row == 3 && it.column == 0 }.playerLetter)
    }

    @Test
    fun completedStateProvidesSafeSharePattern() {
        val state = completedState()

        val ui =
            CrosswordUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertTrue(ui.isCompleted)
        assertNotNull(ui.sharePattern)
        puzzle.entries.forEach { entry ->
            assertFalse(ui.sharePattern.orEmpty().contains(entry.answer, ignoreCase = true))
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
            val metrics = CrosswordLayoutCalculator.calculate(widthDp = width, heightDp = height)

            assertTrue(
                "$width x $height should not scroll; total height was ${metrics.totalPhoneHeight}",
                metrics.totalPhoneHeight <= height + 0.01f,
            )
            assertTrue("$width x $height content should fit width", metrics.contentWidth <= width)
            assertTrue("$width x $height cell should stay readable", metrics.cellSize >= 25f)
            assertTrue("$width x $height key should stay tappable", metrics.keyHeight >= 34f)
            assertTrue("$width x $height expanded list should grow", metrics.expandedPanelHeight > metrics.cluePanelHeight)
            assertTrue("$width x $height expanded list should fit screen", metrics.expandedPanelHeight <= height)
        }
    }

    @Test
    fun tabletLayoutMetricsUseTwoPaneSizing() {
        val metrics = CrosswordLayoutCalculator.calculate(widthDp = 800f, heightDp = 1024f)

        assertTrue(metrics.tablet)
        assertTrue(metrics.contentWidth <= 800f)
        assertTrue(metrics.boardSize >= 196f)
        assertTrue(metrics.cellSize >= 28f)
    }

    private fun type(
        text: String,
        state: CrosswordSaveState,
    ): CrosswordSaveState =
        text.fold(state) { current, letter ->
            CrosswordGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun completedState(): CrosswordSaveState {
        var state = CrosswordGameEngine.initial(puzzle)
        puzzle.entries.forEachIndexed { index, entry ->
            state = CrosswordGameEngine.selectEntry(puzzle, state, index)
            entry.answer.forEach { letter ->
                state = CrosswordGameEngine.appendLetter(puzzle, state, letter).state
            }
        }
        return state
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
