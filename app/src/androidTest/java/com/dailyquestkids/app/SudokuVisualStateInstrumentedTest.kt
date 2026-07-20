package com.dailyquestkids.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.puzzle.engine.SudokuGameEngine
import com.dailyquestkids.puzzle.engine.SudokuSaveState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SudokuVisualStateInstrumentedTest {
    @get:Rule
    val compose = createComposeRule()

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
    fun emptyStateRenders() {
        assertSudokuStateRenders(SudokuGameEngine.initial(puzzle))
    }

    @Test
    fun partialStateRenders() {
        val state = SudokuGameEngine.inputNumber(puzzle, SudokuGameEngine.initial(puzzle), puzzle.solution[0], mistakeChecking = true).state
        assertSudokuStateRenders(state)
    }

    @Test
    fun notesStateRenders() {
        val pencil = SudokuGameEngine.togglePencil(SudokuGameEngine.initial(puzzle))
        val state = SudokuGameEngine.inputNumber(puzzle, pencil, 1, mistakeChecking = true).state
        assertSudokuStateRenders(state)
    }

    @Test
    fun conflictStateRenders() {
        val result = SudokuGameEngine.inputNumber(puzzle, SudokuGameEngine.initial(puzzle), puzzle.givens[1], mistakeChecking = true)
        assertSudokuStateRenders(result.state, result.message?.userText)
    }

    @Test
    fun hintStateRenders() {
        assertSudokuStateRenders(SudokuGameEngine.revealHint(puzzle, SudokuGameEngine.initial(puzzle)).state)
    }

    @Test
    fun completionStateRenders() {
        assertSudokuStateRenders(completedState())
    }

    @Test
    fun topChromeVisualGuardBandsStayClear() {
        setSudokuContent(SudokuGameEngine.initial(puzzle))

        assertNoTextInkTouchesEdges("sudokuProgressBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("sudokuHintsBadge", includeVerticalEdges = true)
        assertNoTextInkTouchesEdges("sudokuStatusStrip", includeVerticalEdges = false)
        assertNoTextInkTouchesEdges("sudokuHintPanel", includeVerticalEdges = false)
    }

    @Test
    fun interactiveNumberEntryUpdatesProgressImmediately() {
        setInteractiveSudokuContent()

        compose.onNodeWithTag("sudokuNumber-${puzzle.solution[0]}").performClick()

        compose.onNodeWithTag("sudokuProgressBadge").assertTextContains("31/36", substring = true)
        compose.onNodeWithTag("sudokuCell-0-0").assertTextContains(puzzle.solution[0].toString(), substring = true)
    }

    @Test
    fun interactiveNotesConflictHintAndUndoWork() {
        setInteractiveSudokuContent()

        compose.onNodeWithTag("sudokuPencilToggle").performClick()
        compose.onNodeWithTag("sudokuNumber-1").performClick()
        compose.onNodeWithTag("sudokuCell-0-0").assertTextContains("1", substring = true)
        compose.onNodeWithTag("sudokuPencilToggle").performClick()
        compose.onNodeWithTag("sudokuNumber-${puzzle.givens[1]}").performClick()
        compose.onNodeWithTag("sudokuStatusStrip").assertTextContains("repeats", substring = true)
        compose.onNodeWithTag("sudokuUndo").performClick()
        compose.onNodeWithTag("sudokuHintButton").performClick()
        compose.onNodeWithTag("sudokuHintPanel").assertTextContains("Look closely", substring = true)
    }

    private fun assertSudokuStateRenders(
        state: SudokuSaveState,
        transientMessage: String? = null,
    ) {
        setSudokuContent(state, transientMessage)

        compose.onNodeWithTag("sudokuScreen").assertIsDisplayed()
        compose.onNodeWithTag("sudokuBoard").assertIsDisplayed()
        compose.onNodeWithTag("sudokuNumberPad").assertIsDisplayed()
        val board = compose.onNodeWithTag("sudokuBoard").captureToImage()
        assertTrue(board.width > 0)
        assertTrue(board.height > 0)
    }

    private fun setSudokuContent(
        state: SudokuSaveState,
        transientMessage: String? = null,
    ) {
        compose.setContent {
            DailyQuestTheme {
                SudokuGameScreen(
                    state =
                        SudokuUiMapper.map(
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

    private fun setInteractiveSudokuContent() {
        compose.setContent {
            DailyQuestTheme {
                var gameState by remember { mutableStateOf(SudokuGameEngine.initial(puzzle)) }
                var message by remember { mutableStateOf<String?>(null) }
                SudokuGameScreen(
                    state =
                        SudokuUiMapper.map(
                            puzzle = puzzle,
                            gameState = gameState,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = message,
                        ),
                    actions =
                        SudokuGameActions(
                            onBack = {},
                            onCell = { row, column ->
                                gameState = SudokuGameEngine.selectCell(puzzle, gameState, row, column)
                                message = null
                            },
                            onNumber = { number ->
                                val result = SudokuGameEngine.inputNumber(puzzle, gameState, number, mistakeChecking = true)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onTogglePencil = {
                                gameState = SudokuGameEngine.togglePencil(gameState)
                                message = null
                            },
                            onErase = {
                                gameState = SudokuGameEngine.erase(puzzle, gameState)
                                message = null
                            },
                            onUndo = {
                                gameState = SudokuGameEngine.undo(gameState)
                                message = null
                            },
                            onRedo = {
                                gameState = SudokuGameEngine.redo(gameState)
                                message = null
                            },
                            onUseHint = {
                                val result = SudokuGameEngine.revealHint(puzzle, gameState)
                                gameState = result.state
                                message = result.message?.userText
                            },
                            onReturnHome = {},
                        ),
                )
            }
        }
    }

    private fun emptyActions(): SudokuGameActions =
        SudokuGameActions(
            onBack = {},
            onCell = { _, _ -> },
            onNumber = {},
            onTogglePencil = {},
            onErase = {},
            onUndo = {},
            onRedo = {},
            onUseHint = {},
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
