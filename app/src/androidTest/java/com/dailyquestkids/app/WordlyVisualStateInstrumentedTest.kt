package com.dailyquestkids.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
import com.dailyquestkids.puzzle.engine.WordlyMessage
import com.dailyquestkids.puzzle.engine.WordlySaveState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class WordlyVisualStateInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

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
    fun emptyStateRenders() {
        assertWordlyStateRenders(WordlyGameEngine.initial(puzzle))
    }

    @Test
    fun partialStateRenders() {
        assertWordlyStateRenders(type("fl", WordlyGameEngine.initial(puzzle)))
    }

    @Test
    fun repeatedLetterStateRenders() {
        assertWordlyStateRenders(submitGuess(WordlyGameEngine.initial(puzzle), "bloom").state)
    }

    @Test
    fun hintStateRenders() {
        assertWordlyStateRenders(WordlyGameEngine.revealHint(puzzle, WordlyGameEngine.initial(puzzle)).state)
    }

    @Test
    fun topChromeVisualGuardBandsStayClear() {
        setWordlyContent(WordlyGameEngine.initial(puzzle))

        assertNoTextInkTouchesEdges("wordlyAttemptsBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("wordlyPromptPill", includeVerticalEdges = false)
        assertNoTextInkTouchesEdges("wordlyStarsPill", includeVerticalEdges = false)
    }

    @Test
    fun invalidGuessFeedbackAppearsInTopPromptPill() {
        val result = WordlyGameEngine.submit(puzzle, type("zzzzz", WordlyGameEngine.initial(puzzle)))

        setWordlyContent(result.state, result.message?.userText)

        compose.onNodeWithTag("wordlyPromptPill").assertTextContains(WordlyMessage.INVALID_GUESS.userText)
        assertNoTextInkTouchesEdges("wordlyPromptPill", includeVerticalEdges = false)
    }

    @Test
    fun cluePanelExpandsAndCollapses() {
        setWordlyContent(WordlyGameEngine.initial(puzzle))

        compose.onAllNodesWithTag("wordlyExpandedCluePanel").assertCountEquals(0)
        compose.onNodeWithTag("wordlyCluePanel").performClick()
        compose.onNodeWithTag("wordlyExpandedCluePanel").assertIsDisplayed()
        compose.onNodeWithTag("wordlyExpandedCluePanel").performClick()
        compose.onAllNodesWithTag("wordlyExpandedCluePanel").assertCountEquals(0)

        compose.onNodeWithTag("wordlyCluePanel").performClick()
        compose.onNodeWithTag("wordlyClueDismissLayer").performClick()
        compose.onAllNodesWithTag("wordlyExpandedCluePanel").assertCountEquals(0)
    }

    @Test
    fun successStateRenders() {
        assertWordlyStateRenders(submitGuess(WordlyGameEngine.initial(puzzle), puzzle.solution).state)
    }

    @Test
    fun failureStateRenders() {
        var state = WordlyGameEngine.initial(puzzle)
        puzzle.validGuesses
            .filterNot { it == puzzle.solution }
            .take(6)
            .forEach { guess ->
                state = submitGuess(state, guess).state
            }

        assertWordlyStateRenders(state)
    }

    private fun assertWordlyStateRenders(state: WordlySaveState) {
        setWordlyContent(state)

        compose.onNodeWithTag("wordlyScreen").assertIsDisplayed()
        val image = compose.onNodeWithTag("wordlyBoard").captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    private fun setWordlyContent(
        state: WordlySaveState,
        transientMessage: String? = null,
    ) {
        compose.setContent {
            DailyQuestTheme {
                WordlyGameScreen(
                    state =
                        WordlyUiMapper.map(
                            puzzle = puzzle,
                            gameState = state,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = transientMessage,
                        ),
                    actions =
                        WordlyGameActions(
                            onBack = {},
                            onUseHint = {},
                            onLetter = {},
                            onDelete = {},
                            onSubmit = {},
                            onReturnHome = {},
                        ),
                )
            }
        }
    }

    private fun assertNoTextInkTouchesEdges(
        tag: String,
        includeVerticalEdges: Boolean,
    ) {
        val image = compose.onNodeWithTag(tag).captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
        assertTrue("$tag has clipped text on a horizontal edge", textInkOnHorizontalEdges(image) == 0)
        if (includeVerticalEdges) {
            assertTrue("$tag has clipped text on a vertical edge", textInkOnVerticalEdges(image) == 0)
        }
    }

    private fun textInkOnHorizontalEdges(image: ImageBitmap): Int {
        val pixels = image.toPixelMap()
        val guard = edgeGuardPixels(image)
        var inkCount = 0
        for (y in guard until image.height - guard) {
            for (x in 0 until guard) {
                if (pixels[x, y].isTextInk()) inkCount++
            }
            for (x in image.width - guard until image.width) {
                if (pixels[x, y].isTextInk()) inkCount++
            }
        }
        return inkCount
    }

    private fun textInkOnVerticalEdges(image: ImageBitmap): Int {
        val pixels = image.toPixelMap()
        val guard = edgeGuardPixels(image)
        var inkCount = 0
        for (y in 0 until guard) {
            for (x in guard until image.width - guard) {
                if (pixels[x, y].isTextInk()) inkCount++
            }
        }
        for (y in image.height - guard until image.height) {
            for (x in guard until image.width - guard) {
                if (pixels[x, y].isTextInk()) inkCount++
            }
        }
        return inkCount
    }

    private fun edgeGuardPixels(image: ImageBitmap): Int = (minOf(image.width, image.height) / 8).coerceIn(3, 10)

    private fun Color.isTextInk(): Boolean = alpha > 0.8f && red < 0.32f && green < 0.35f && blue < 0.45f

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
