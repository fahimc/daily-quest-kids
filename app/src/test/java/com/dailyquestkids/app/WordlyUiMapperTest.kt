package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.puzzle.engine.TileState
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
import com.dailyquestkids.puzzle.engine.WordlySaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class WordlyUiMapperTest {
    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<WordlyPuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun partialInputAndHintMapToBoardAndClueState() {
        val hinted = WordlyGameEngine.revealHint(puzzle, WordlyGameEngine.initial(puzzle)).state
        val state = type("fl", hinted)

        val ui =
            WordlyUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(largePuzzleText = true),
                homeState = homeState,
                transientMessage = "Hint unlocked.",
            )

        assertEquals("0/6", ui.attemptsLabel)
        assertEquals('F', ui.rows.first().letters[0])
        assertEquals('L', ui.rows.first().letters[1])
        assertEquals(TileState.EMPTY, ui.rows.first().states[0])
        assertEquals(puzzle.hints.first().text, ui.openHintText)
        assertTrue(ui.canUseHint)
        assertTrue(ui.largeText)
        assertEquals(3, ui.keyboardRows.size)
    }

    @Test
    fun terminalStateProvidesLearningSummaryAndSafeSharePattern() {
        val wrong = puzzle.validGuesses.first { it != puzzle.solution }
        var state = submitGuess(WordlyGameEngine.initial(puzzle), wrong).state
        state = submitGuess(state, puzzle.solution).state

        val ui =
            WordlyUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertTrue(ui.isTerminal)
        assertNotNull(ui.summary)
        assertNotNull(ui.sharePattern)
        assertFalse(ui.sharePattern.orEmpty().contains(puzzle.solution, ignoreCase = true))
        state.attempts.forEach { guess ->
            assertFalse(ui.sharePattern.orEmpty().contains(guess, ignoreCase = true))
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
            val metrics = WordlyLayoutCalculator.calculate(widthDp = width, heightDp = height)

            assertTrue(
                "$width x $height should not scroll; total height was ${metrics.totalHeight}",
                metrics.totalHeight <= height + 0.01f,
            )
            assertTrue("$width x $height content should fit width", metrics.contentWidth <= width)
            assertTrue("$width x $height board tile should stay visible", metrics.tileSize >= 24f)
            assertTrue("$width x $height keyboard key should stay tappable", metrics.keyHeight >= 34f)
        }
    }

    private fun type(
        text: String,
        state: WordlySaveState,
    ): WordlySaveState =
        text.fold(state) { current, letter ->
            WordlyGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun submitGuess(
        state: WordlySaveState,
        guess: String,
    ) = WordlyGameEngine.submit(puzzle, type(guess, state))

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
