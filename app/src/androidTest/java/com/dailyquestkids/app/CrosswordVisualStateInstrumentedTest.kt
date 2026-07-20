package com.dailyquestkids.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.puzzle.engine.CrosswordGameEngine
import com.dailyquestkids.puzzle.engine.CrosswordSaveState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CrosswordVisualStateInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

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
    fun emptyStateRenders() {
        assertCrosswordStateRenders(CrosswordGameEngine.initial(puzzle))
    }

    @Test
    fun partialStateRenders() {
        assertCrosswordStateRenders(type("flo"))
    }

    @Test
    fun hintStateRenders() {
        assertCrosswordStateRenders(CrosswordGameEngine.revealHint(puzzle, CrosswordGameEngine.initial(puzzle)).state)
    }

    @Test
    fun completionStateRenders() {
        assertCrosswordStateRenders(completedState())
    }

    @Test
    fun topChromeVisualGuardBandsStayClear() {
        setCrosswordContent(CrosswordGameEngine.initial(puzzle))

        assertNoTextInkTouchesEdges("crosswordProgressBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("crosswordHintsBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("crosswordStatusStrip", includeVerticalEdges = false)
        assertNoTextInkTouchesEdges("crosswordCluePanel", includeVerticalEdges = false)
    }

    @Test
    fun expandedClueListShowsAcrossDownAndCollapses() {
        setCrosswordContent(CrosswordGameEngine.initial(puzzle))

        compose.onAllNodesWithTag("crosswordExpandedClueList").assertCountEquals(0)
        compose.onNodeWithTag("crosswordCluePanel").performClick()
        compose.onNodeWithTag("crosswordExpandedClueList").assertIsDisplayed()
        compose.onNodeWithTag("crosswordExpandedClueList").assertTextContains("Across", substring = true)
        compose.onNodeWithTag("crosswordExpandedClueList").assertTextContains("Down", substring = true)
        compose.onNodeWithTag("crosswordExpandedClueList").performClick()
        compose.onAllNodesWithTag("crosswordExpandedClueList").assertCountEquals(0)
    }

    @Test
    fun interactiveAcrossAnswerUpdatesProgressImmediately() {
        setInteractiveCrosswordContent()

        "FLOWERS".forEach { letter ->
            compose.onNodeWithTag("crosswordKey-$letter").performClick()
        }

        compose.onNodeWithTag("crosswordStatusStrip").assertTextContains("Answer complete", substring = true)
        compose.onNodeWithTag("crosswordProgressBadge").assertTextContains("1/${puzzle.entries.size}", substring = true)
    }

    private fun assertCrosswordStateRenders(state: CrosswordSaveState) {
        setCrosswordContent(state)

        compose.onNodeWithTag("crosswordScreen").assertIsDisplayed()
        compose.onNodeWithTag("crosswordBoard").assertIsDisplayed()
        compose.onNodeWithTag("crosswordCluePanel").assertIsDisplayed()
        compose.onNodeWithTag("crosswordKeyboard").assertIsDisplayed()
        val board = compose.onNodeWithTag("crosswordBoard").captureToImage()
        val cluePanel = compose.onNodeWithTag("crosswordCluePanel").captureToImage()
        assertTrue(board.width > 0)
        assertTrue(board.height > 0)
        assertTrue(cluePanel.width > 0)
        assertTrue(cluePanel.height > 0)
    }

    private fun setCrosswordContent(
        state: CrosswordSaveState,
        transientMessage: String? = null,
    ) {
        compose.setContent {
            DailyQuestTheme {
                CrosswordGameScreen(
                    state =
                        CrosswordUiMapper.map(
                            puzzle = puzzle,
                            gameState = state,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = transientMessage,
                        ),
                    actions = emptyActions(),
                )
            }
        }
    }

    private fun setInteractiveCrosswordContent() {
        compose.setContent {
            DailyQuestTheme {
                var gameState by remember { mutableStateOf(CrosswordGameEngine.initial(puzzle)) }
                var message by remember { mutableStateOf<String?>(null) }
                CrosswordGameScreen(
                    state =
                        CrosswordUiMapper.map(
                            puzzle = puzzle,
                            gameState = gameState,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = message,
                        ),
                    actions =
                        CrosswordGameActions(
                            onBack = {},
                            onCell = { row, column ->
                                gameState = CrosswordGameEngine.selectCell(puzzle, gameState, row, column)
                                message = null
                            },
                            onEntry = { entryIndex ->
                                gameState = CrosswordGameEngine.selectEntry(puzzle, gameState, entryIndex)
                                message = null
                            },
                            onToggleDirection = {
                                gameState = CrosswordGameEngine.toggleDirection(puzzle, gameState)
                                message = null
                            },
                            onPrevious = {
                                gameState = CrosswordGameEngine.previousEntry(puzzle, gameState)
                                message = null
                            },
                            onNext = {
                                gameState = CrosswordGameEngine.nextEntry(puzzle, gameState)
                                message = null
                            },
                            onUseHint = {
                                val result = CrosswordGameEngine.revealHint(puzzle, gameState)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onLetter = { letter ->
                                val result = CrosswordGameEngine.appendLetter(puzzle, gameState, letter)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onDelete = {
                                gameState = CrosswordGameEngine.deleteLetter(puzzle, gameState)
                                message = null
                            },
                            onReturnHome = {},
                        ),
                )
            }
        }
    }

    private fun emptyActions(): CrosswordGameActions =
        CrosswordGameActions(
            onBack = {},
            onCell = { _, _ -> },
            onEntry = {},
            onToggleDirection = {},
            onPrevious = {},
            onNext = {},
            onUseHint = {},
            onLetter = {},
            onDelete = {},
            onReturnHome = {},
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
        state: CrosswordSaveState = CrosswordGameEngine.initial(puzzle),
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
