package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.puzzle.engine.ConnectionsGameEngine
import com.dailyquestkids.puzzle.engine.ConnectionsMessage
import com.dailyquestkids.puzzle.engine.ConnectionsSaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConnectionsUiMapperTest {
    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<ConnectionsPuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun initialStateMapsTilesBadgesAndStatus() {
        val ui = map(ConnectionsGameEngine.initial(puzzle))

        assertEquals(16, ui.tiles.size)
        assertEquals("0/4", ui.progressBadge)
        assertEquals("4", ui.hintBadge)
        assertEquals("Find groups of four.", ui.statusText)
        assertEquals(0, ui.selectedCount)
        assertFalse(ui.canSubmit)
    }

    @Test
    fun selectionAndSolvedGroupMapImmediately() {
        val selected = selectWords(ConnectionsGameEngine.initial(puzzle), puzzle.groups.first().words)
        val selectedUi = map(selected)
        val result = ConnectionsGameEngine.submit(puzzle, selected)
        val solvedUi = map(result.state, result.message?.userText)

        assertEquals(4, selectedUi.selectedCount)
        assertTrue(selectedUi.canSubmit)
        assertEquals(ConnectionsMessage.GROUP_SOLVED.userText, solvedUi.statusText)
        assertEquals(1, solvedUi.solvedGroups.size)
        assertEquals(12, solvedUi.tiles.size)
    }

    @Test
    fun failedStateMapsTerminalPanel() {
        val ui = map(failedState(), ConnectionsMessage.FAILED.userText)

        assertTrue(ui.isFailed)
        assertEquals(0, ui.remainingMistakes)
        assertFalse(ui.canSubmit)
        assertFalse(ui.canUseHint)
        assertTrue(ui.panelText.contains("Try again tomorrow"))
    }

    @Test
    fun completedStateProvidesSafeSharePattern() {
        val ui = map(completedState())

        assertTrue(ui.isCompleted)
        assertNotNull(ui.panelText)
        puzzle.groups.forEach { group ->
            group.words.forEach { word ->
                assertFalse(ui.panelText.contains(word, ignoreCase = true))
            }
        }
    }

    @Test
    fun routeStateKeepsSolvedGroupOptimisticUntilSavedStateCatchesUp() {
        val savedBeforeSubmit = ConnectionsGameEngine.initial(puzzle)
        val solved = ConnectionsGameEngine.submit(puzzle, selectWords(savedBeforeSubmit, puzzle.groups.first().words)).state

        val merged = ConnectionsRouteStateReducer.mergeSavedState(solved, savedBeforeSubmit)
        val ui = map(merged)

        assertEquals(solved.solvedGroupTitles, merged.solvedGroupTitles)
        assertEquals("1/4", ui.progressBadge)
        assertEquals(12, ui.tiles.size)
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
            val metrics = ConnectionsLayoutCalculator.calculate(widthDp = width, heightDp = height)

            assertTrue("$width x $height content should fit width", metrics.contentWidth <= width)
            assertTrue("$width x $height tile should stay readable", metrics.tileSize >= 38f)
            assertTrue("$width x $height action buttons should be tappable", metrics.actionHeight >= 34f)
            assertTrue("$width x $height total height should fit", metrics.totalPhoneHeight <= height + 0.01f)
        }
    }

    private fun map(
        state: ConnectionsSaveState,
        transientMessage: String? = null,
    ) = ConnectionsUiMapper.map(
        puzzle = puzzle,
        gameState = state,
        homeState = homeState,
        transientMessage = transientMessage,
    )

    private fun completedState(): ConnectionsSaveState {
        var state = ConnectionsGameEngine.initial(puzzle)
        puzzle.groups.forEach { group ->
            state = ConnectionsGameEngine.submit(puzzle, selectWords(state, group.words)).state
        }
        return state
    }

    private fun failedState(): ConnectionsSaveState {
        var state = ConnectionsGameEngine.initial(puzzle)
        val mixedWords = listOf(puzzle.groups[0].words[0], puzzle.groups[1].words[0], puzzle.groups[2].words[0], puzzle.groups[3].words[0])
        repeat(5) {
            state = ConnectionsGameEngine.submit(puzzle, selectWords(state, mixedWords)).state
        }
        return state
    }

    private fun selectWords(
        state: ConnectionsSaveState,
        words: List<String>,
    ): ConnectionsSaveState {
        var next = state
        words.forEach { word ->
            next = ConnectionsGameEngine.toggleTile(puzzle, next, word)
        }
        return next
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
