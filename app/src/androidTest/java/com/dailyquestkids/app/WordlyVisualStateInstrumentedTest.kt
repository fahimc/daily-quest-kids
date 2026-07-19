package com.dailyquestkids.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
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
        compose.setContent {
            DailyQuestTheme {
                WordlyGameScreen(
                    state =
                        WordlyUiMapper.map(
                            puzzle = puzzle,
                            gameState = state,
                            settings = QuestSettings(),
                            homeState = homeState,
                            transientMessage = null,
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

        compose.onNodeWithTag("wordlyScreen").assertIsDisplayed()
        val image = compose.onNodeWithTag("wordlyBoard").captureToImage()
        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
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
