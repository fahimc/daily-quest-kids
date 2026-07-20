package com.dailyquestkids.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.puzzle.engine.ConnectionsGameEngine
import com.dailyquestkids.puzzle.engine.ConnectionsSaveState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ConnectionsVisualStateInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

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
    fun initialStateRenders() {
        assertConnectionsStateRenders(ConnectionsGameEngine.initial(puzzle))
    }

    @Test
    fun selectedStateRenders() {
        val firstPair =
            puzzle
                .groups
                .first()
                .words
                .take(2)
        val state =
            selectWords(
                ConnectionsGameEngine.initial(puzzle),
                firstPair,
            )

        assertConnectionsStateRenders(state)
    }

    @Test
    fun solvedGroupStateRenders() {
        val state =
            ConnectionsGameEngine
                .submit(puzzle, selectWords(ConnectionsGameEngine.initial(puzzle), puzzle.groups.first().words))
                .state

        assertConnectionsStateRenders(state)
        compose.onNodeWithTag("connectionsSolvedGroup-0").assertIsDisplayed()
    }

    @Test
    fun hintStateRenders() {
        val state = ConnectionsGameEngine.revealHint(puzzle, ConnectionsGameEngine.initial(puzzle)).state

        assertConnectionsStateRenders(state)
        compose.onNodeWithText("One group is about sky things.").assertIsDisplayed()
    }

    @Test
    fun failureStateRenders() {
        assertConnectionsStateRenders(failedState())
    }

    @Test
    fun completionStateRenders() {
        setConnectionsContent(completedState())

        compose.onNodeWithTag("connectionsScreen").assertIsDisplayed()
        compose.onNodeWithTag("connectionsShareButton").assertIsDisplayed()
        compose.onNodeWithTag("connectionsDoneButton").assertIsDisplayed()
        compose.onNodeWithText("Connections complete").assertIsDisplayed()
    }

    @Test
    fun topChromeVisualGuardBandsStayClear() {
        setConnectionsContent(ConnectionsGameEngine.initial(puzzle))

        assertNoTextInkTouchesEdges("connectionsProgressBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("connectionsHintsBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("connectionsStatusStrip", includeVerticalEdges = false)
        assertNoTextInkTouchesEdges("connectionsHintPanel", includeVerticalEdges = false)
    }

    @Test
    fun interactiveSelectionSubmitShuffleDeselectAndHintWork() {
        setInteractiveConnectionsContent()

        puzzle.groups.first().words.forEach { word ->
            compose.onNodeWithTag("connectionsTile-$word").performClick()
        }
        compose.onNodeWithTag("connectionsSubmit").performClick()
        compose.onNodeWithText("1/4").assertIsDisplayed()
        compose.onNodeWithTag("connectionsSolvedGroup-0").assertIsDisplayed()

        val firstRemainingWord = puzzle.groups[1].words.first()
        compose.onNodeWithTag("connectionsShuffle").performClick()
        compose.onNodeWithTag("connectionsTile-$firstRemainingWord").performClick()
        compose.onNodeWithTag("connectionsDeselect").performClick()
        compose.onNodeWithText("0/4").assertIsDisplayed()

        compose.onNodeWithTag("connectionsHintButton").performClick()
        compose.onNodeWithText("One group is about school tools.").assertIsDisplayed()
    }

    private fun assertConnectionsStateRenders(
        state: ConnectionsSaveState,
        transientMessage: String? = null,
    ) {
        setConnectionsContent(state, transientMessage)

        compose.onNodeWithTag("connectionsScreen").assertIsDisplayed()
        compose.onNodeWithTag("connectionsTileGrid").assertIsDisplayed()
        compose.onNodeWithTag("connectionsHintPanel").assertIsDisplayed()
        val board = compose.onNodeWithTag("connectionsTileGrid").captureToImage()
        assertTrue(board.width > 0)
        assertTrue(board.height > 0)
    }

    private fun setConnectionsContent(
        state: ConnectionsSaveState,
        transientMessage: String? = null,
    ) {
        compose.setContent {
            DailyQuestTheme {
                val shareActions = ShareActions(LocalContext.current)
                ConnectionsGameScreen(
                    state =
                        ConnectionsUiMapper.map(
                            puzzle = puzzle,
                            gameState = state,
                            homeState = homeState,
                            transientMessage = transientMessage,
                        ),
                    actions = emptyActions(shareActions),
                )
            }
        }
    }

    private fun setInteractiveConnectionsContent() {
        compose.setContent {
            DailyQuestTheme {
                val shareActions = ShareActions(LocalContext.current)
                var gameState by remember { mutableStateOf(ConnectionsGameEngine.initial(puzzle)) }
                var message by remember { mutableStateOf<String?>(null) }
                ConnectionsGameScreen(
                    state =
                        ConnectionsUiMapper.map(
                            puzzle = puzzle,
                            gameState = gameState,
                            homeState = homeState,
                            transientMessage = message,
                        ),
                    actions =
                        ConnectionsGameActions(
                            onBack = {},
                            onTile = { word ->
                                gameState = ConnectionsGameEngine.toggleTile(puzzle, gameState, word)
                                message = null
                            },
                            onSubmit = {
                                val result = ConnectionsGameEngine.submit(puzzle, gameState)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onShuffle = {
                                gameState = ConnectionsGameEngine.shuffle(puzzle, gameState)
                                message = null
                            },
                            onDeselect = {
                                gameState = ConnectionsGameEngine.deselectAll(gameState)
                                message = null
                            },
                            onUseHint = {
                                val confirmReveal = gameState.awaitingRevealConfirmation
                                val result = ConnectionsGameEngine.revealHint(puzzle, gameState, confirmReveal)
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

    private fun emptyActions(shareActions: ShareActions): ConnectionsGameActions =
        ConnectionsGameActions(
            onBack = {},
            onTile = {},
            onSubmit = {},
            onShuffle = {},
            onDeselect = {},
            onUseHint = {},
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

    private fun completedState(): ConnectionsSaveState {
        var state = ConnectionsGameEngine.initial(puzzle)
        puzzle.groups.forEach { group ->
            state = ConnectionsGameEngine.submit(puzzle, selectWords(state, group.words)).state
        }
        return state
    }

    private fun failedState(): ConnectionsSaveState {
        var state = ConnectionsGameEngine.initial(puzzle)
        val mixedWords =
            listOf(
                puzzle.groups[0].words[0],
                puzzle.groups[1].words[0],
                puzzle.groups[2].words[0],
                puzzle.groups[3].words[0],
            )
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
