package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.puzzle.engine.SpellingBGameEngine
import com.dailyquestkids.puzzle.engine.SpellingBMessage
import com.dailyquestkids.puzzle.engine.SpellingBSaveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SpellingBUiMapperTest {
    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<SpellingBeePuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun partialInputAndHintMapToHiveState() {
        val hinted = SpellingBGameEngine.revealHint(puzzle, SpellingBGameEngine.initial(puzzle)).state
        val state = type("pla", hinted)

        val ui =
            SpellingBUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(largePuzzleText = true),
                homeState = homeState,
                transientMessage = "Hint unlocked.",
            )

        assertEquals("PLA", ui.currentWord)
        assertEquals('A', ui.centreLetter)
        assertEquals(6, ui.outerLetters.size)
        assertEquals(1, ui.hintTexts.size)
        assertTrue(ui.canUseHint)
        assertTrue(ui.largeText)
        assertEquals("Hint unlocked.", ui.message)
    }

    @Test
    fun foundWordMapsDefinitionAndScoreProgress() {
        val state = submitWord(SpellingBGameEngine.initial(puzzle), "plant").state

        val ui =
            SpellingBUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertEquals("5/${SpellingBGameEngine.totalScore(puzzle)}", ui.scoreLabel)
        assertEquals("PLANT", ui.foundRows.single().word)
        assertTrue(ui.foundSummary.contains("living thing", ignoreCase = true))
        assertFalse(ui.isCompleted)
        assertTrue(ui.progress > 0f)
    }

    @Test
    fun invalidSubmitMessageMapsToPromptArea() {
        val result = SpellingBGameEngine.submit(puzzle, type("pen"))

        val ui =
            SpellingBUiMapper.map(
                puzzle = puzzle,
                gameState = result.state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = result.message?.userText,
            )

        assertEquals(SpellingBMessage.MISSING_CENTRE, result.message)
        assertEquals("Every word must use the middle letter.", ui.message)
        assertEquals(emptyList<String>(), result.state.foundWords)
    }

    @Test
    fun completedStateProvidesSafeSharePattern() {
        var state = SpellingBGameEngine.initial(puzzle)
        puzzle.targetWords.forEach { target ->
            state = submitWord(state, target.word).state
        }

        val ui =
            SpellingBUiMapper.map(
                puzzle = puzzle,
                gameState = state,
                settings = QuestSettings(),
                homeState = homeState,
                transientMessage = null,
            )

        assertTrue(ui.isCompleted)
        assertNotNull(ui.sharePattern)
        puzzle.targetWords.forEach { target ->
            assertFalse(ui.sharePattern.orEmpty().contains(target.word, ignoreCase = true))
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
            val metrics = SpellingBLayoutCalculator.calculate(widthDp = width, heightDp = height)

            assertTrue(
                "$width x $height should not scroll; total height was ${metrics.totalHeight}",
                metrics.totalHeight <= height + 0.01f,
            )
            assertTrue("$width x $height content should fit width", metrics.contentWidth <= width)
            assertTrue("$width x $height hive should stay visible", metrics.hiveSize >= 146f)
            assertTrue("$width x $height tile should be usable", metrics.hiveTileSize >= 42f)
            assertTrue("$width x $height expanded list should grow", metrics.expandedPanelHeight > metrics.bottomPanelHeight)
            assertTrue("$width x $height expanded list should fit screen", metrics.expandedPanelHeight <= height)
        }
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
    ) = SpellingBGameEngine.submit(puzzle, type(word, state))

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
