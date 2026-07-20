package com.dailyquestkids.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
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
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.puzzle.engine.SpellingBGameEngine
import com.dailyquestkids.puzzle.engine.SpellingBSaveState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SpellingBVisualStateInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

    private val repository = SamplePackRepository()
    private val puzzle =
        repository.pack.days
            .first()
            .puzzles
            .filterIsInstance<SpellingBeePuzzle>()
            .single()
    private val painterPuzzle =
        repository.pack.days[1]
            .puzzles
            .filterIsInstance<SpellingBeePuzzle>()
            .single()
    private val homeState =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        ).homeState(StoredProgress())

    @Test
    fun emptyStateRenders() {
        assertSpellingStateRenders(SpellingBGameEngine.initial(puzzle))
    }

    @Test
    fun partialStateRenders() {
        assertSpellingStateRenders(type("pla"))
    }

    @Test
    fun foundWordStateRenders() {
        assertSpellingStateRenders(submitWord(SpellingBGameEngine.initial(puzzle), "plant").state)
    }

    @Test
    fun hintStateRenders() {
        assertSpellingStateRenders(SpellingBGameEngine.revealHint(puzzle, SpellingBGameEngine.initial(puzzle)).state)
    }

    @Test
    fun completionStateRenders() {
        assertSpellingStateRenders(completedState())
    }

    @Test
    fun topChromeVisualGuardBandsStayClear() {
        setSpellingContent(SpellingBGameEngine.initial(puzzle))

        assertNoTextInkTouchesEdges("spellingScoreBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("spellingHintsBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("spellingCurrentWord", includeVerticalEdges = false)
        val scorePanel = compose.onNodeWithTag("spellingScorePanel").captureToImage()
        assertTrue(scorePanel.width > 0)
        assertTrue(scorePanel.height > 0)
    }

    @Test
    fun expandedFoundListShowsDefinitionsAndCollapses() {
        val state = submitWord(SpellingBGameEngine.initial(puzzle), "plant").state
        setSpellingContent(state)

        compose.onAllNodesWithTag("spellingExpandedFoundPanel").assertCountEquals(0)
        compose.onNodeWithTag("spellingFoundPanel").performClick()
        compose.onNodeWithTag("spellingExpandedFoundPanel").assertIsDisplayed()
        compose.onNodeWithTag("spellingExpandedFoundPanel").assertTextContains("A living thing", substring = true)
        compose.onNodeWithTag("spellingExpandedFoundPanel").performClick()
        compose.onAllNodesWithTag("spellingExpandedFoundPanel").assertCountEquals(0)
    }

    @Test
    fun invalidSubmitFeedbackAppearsInCurrentWordArea() {
        setInteractiveSpellingContent()

        compose.onNodeWithTag("spellingLetter-P").performClick()
        compose.onNodeWithTag("spellingLetter-E").performClick()
        compose.onNodeWithTag("spellingLetter-N").performClick()
        compose.onNodeWithTag("spellingSubmitButton").performClick()

        compose.onNodeWithTag("spellingCurrentWord").assertTextContains("middle letter", substring = true)
        assertNoTextInkTouchesEdges("spellingCurrentWord", includeVerticalEdges = false)
    }

    @Test
    fun pitFoundWordCountUpdatesImmediatelyAfterSubmit() {
        setInteractiveSpellingContent(painterPuzzle)

        compose.onNodeWithTag("spellingLetter-P").performClick()
        compose.onNodeWithTag("spellingCentreLetter").performClick()
        compose.onNodeWithTag("spellingLetter-T").performClick()
        compose.onNodeWithTag("spellingSubmitButton").performClick()

        compose.onNodeWithTag("spellingCurrentWord").assertTextContains("Word found", substring = true)
        compose.onNodeWithTag("spellingScorePanel").assertTextContains("Words", substring = true)
        compose.onNodeWithTag("spellingScorePanel").assertTextContains("1/${painterPuzzle.targetWords.size}", substring = true)
    }

    private fun assertSpellingStateRenders(state: SpellingBSaveState) {
        setSpellingContent(state)

        compose.onNodeWithTag("spellingScreen").assertIsDisplayed()
        val hive = compose.onNodeWithTag("spellingHoneycomb").captureToImage()
        val panel = compose.onNodeWithTag("spellingFoundPanel").captureToImage()
        assertTrue(hive.width > 0)
        assertTrue(hive.height > 0)
        assertTrue(panel.width > 0)
        assertTrue(panel.height > 0)
    }

    private fun setSpellingContent(
        state: SpellingBSaveState,
        transientMessage: String? = null,
    ) {
        compose.setContent {
            DailyQuestTheme {
                val shareActions = ShareActions(LocalContext.current)
                SpellingBGameScreen(
                    state =
                        SpellingBUiMapper.map(
                            puzzle = puzzle,
                            gameState = state,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = transientMessage,
                        ),
                    actions = emptyActions(shareActions),
                )
            }
        }
    }

    private fun setInteractiveSpellingContent(activePuzzle: SpellingBeePuzzle = puzzle) {
        compose.setContent {
            DailyQuestTheme {
                val shareActions = ShareActions(LocalContext.current)
                var gameState by remember { mutableStateOf(SpellingBGameEngine.initial(activePuzzle)) }
                var message by remember { mutableStateOf<String?>(null) }
                SpellingBGameScreen(
                    state =
                        SpellingBUiMapper.map(
                            puzzle = activePuzzle,
                            gameState = gameState,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = message,
                        ),
                    actions =
                        SpellingBGameActions(
                            onBack = {},
                            onUseHint = {
                                val result = SpellingBGameEngine.revealHint(activePuzzle, gameState)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onLetter = { letter ->
                                val result = SpellingBGameEngine.appendLetter(activePuzzle, gameState, letter)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onDelete = {
                                gameState = SpellingBGameEngine.deleteLetter(activePuzzle, gameState)
                                message = null
                            },
                            onClear = {
                                gameState = SpellingBGameEngine.clearInput(activePuzzle, gameState)
                                message = null
                            },
                            onShuffle = {
                                gameState = SpellingBGameEngine.shuffle(activePuzzle, gameState)
                                message = null
                            },
                            onSubmit = {
                                val result = SpellingBGameEngine.submit(activePuzzle, gameState)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onReturnHome = {},
                            shareActions = shareActions,
                        ),
                )
            }
        }
    }

    private fun emptyActions(shareActions: ShareActions): SpellingBGameActions =
        SpellingBGameActions(
            onBack = {},
            onUseHint = {},
            onLetter = {},
            onDelete = {},
            onClear = {},
            onShuffle = {},
            onSubmit = {},
            onReturnHome = {},
            shareActions = shareActions,
        )

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
        state: SpellingBSaveState = SpellingBGameEngine.initial(puzzle),
    ): SpellingBSaveState =
        text.fold(state) { current, letter ->
            SpellingBGameEngine.appendLetter(puzzle, current, letter).state
        }

    private fun submitWord(
        state: SpellingBSaveState,
        word: String,
    ) = SpellingBGameEngine.submit(puzzle, type(word, state))

    private fun completedState(): SpellingBSaveState {
        var state = SpellingBGameEngine.initial(puzzle)
        puzzle.targetWords.forEach { target ->
            state = submitWord(state, target.word).state
        }
        return state
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
