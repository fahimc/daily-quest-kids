package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import com.dailyquestkids.core.model.WordlyPuzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WordlyGameEngine {
    const val WORD_LENGTH = 5
    const val MAX_ATTEMPTS = 6

    fun initial(puzzle: WordlyPuzzle): WordlySaveState = WordlySaveState(puzzleId = puzzle.id)

    fun appendLetter(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
        letter: Char,
    ): WordlyMoveResult {
        val normalised = letter.lowercaseChar()
        return when {
            !normalised.isLetter() -> WordlyMoveResult(state, WordlyMessage.LETTERS_ONLY)
            state.isTerminal -> WordlyMoveResult(state, WordlyMessage.ALREADY_FINISHED)
            state.currentInput.length >= WORD_LENGTH -> WordlyMoveResult(state)
            else -> WordlyMoveResult(sanitise(puzzle, state).copy(currentInput = state.currentInput + normalised))
        }
    }

    fun deleteLetter(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlySaveState {
        val safeState = sanitise(puzzle, state)
        if (safeState.isTerminal || safeState.currentInput.isEmpty()) return safeState
        return safeState.copy(currentInput = safeState.currentInput.dropLast(1))
    }

    fun submit(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlyMoveResult {
        val safeState = sanitise(puzzle, state)
        return when {
            safeState.isTerminal -> WordlyMoveResult(safeState, WordlyMessage.ALREADY_FINISHED)
            safeState.currentInput.length < WORD_LENGTH -> WordlyMoveResult(safeState, WordlyMessage.TOO_SHORT)
            !isValidGuess(puzzle, safeState.currentInput) -> WordlyMoveResult(safeState, WordlyMessage.INVALID_GUESS)
            else -> submitValidGuess(puzzle, safeState)
        }
    }

    private fun submitValidGuess(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlyMoveResult {
        val attempts = state.attempts + state.currentInput
        val completed = state.currentInput == puzzle.solution.lowercase()
        val failed = !completed && attempts.size == MAX_ATTEMPTS
        val nextState =
            state.copy(
                attempts = attempts,
                currentInput = "",
                isCompleted = completed,
                isFailed = failed,
                completionAcknowledged = false,
            )

        return WordlyMoveResult(
            state = nextState,
            message =
                when {
                    completed -> WordlyMessage.SOLVED
                    failed -> WordlyMessage.OUT_OF_ATTEMPTS
                    else -> WordlyMessage.GUESS_ACCEPTED
                },
            completionEvent = pendingCompletion(nextState),
        )
    }

    fun revealHint(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlyMoveResult {
        val safeState = sanitise(puzzle, state)
        val nextHint =
            puzzle.hints
                .sortedBy { it.order }
                .firstOrNull { hint -> hint.order !in safeState.revealedHintOrders }
                ?: return WordlyMoveResult(safeState, WordlyMessage.NO_HINTS_LEFT)

        return WordlyMoveResult(
            state = safeState.copy(revealedHintOrders = safeState.revealedHintOrders + nextHint.order),
            message = WordlyMessage.HINT_REVEALED,
        )
    }

    fun acknowledgeCompletion(state: WordlySaveState): WordlySaveState =
        if (state.isTerminal) state.copy(completionAcknowledged = true) else state

    fun pendingCompletion(state: WordlySaveState): WordlyCompletionEvent? =
        when {
            !state.isTerminal || state.completionAcknowledged -> null
            state.isCompleted ->
                WordlyCompletionEvent(
                    outcome = WordlyOutcome.SUCCESS,
                    puzzleId = state.puzzleId,
                    attempts = state.attempts.size,
                    hintsUsed = state.revealedHintOrders.size,
                )
            else ->
                WordlyCompletionEvent(
                    outcome = WordlyOutcome.FAILURE,
                    puzzleId = state.puzzleId,
                    attempts = state.attempts.size,
                    hintsUsed = state.revealedHintOrders.size,
                )
        }

    fun boardRows(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): List<WordlyBoardRow> {
        val rows = mutableListOf<WordlyBoardRow>()
        state.attempts.forEach { guess ->
            rows +=
                WordlyBoardRow(
                    letters = guess.map { it.uppercaseChar() },
                    states = WordlyScorer.score(puzzle.solution, guess).map { it.state },
                    submitted = true,
                )
        }

        if (!state.isTerminal && rows.size < MAX_ATTEMPTS) {
            rows +=
                WordlyBoardRow(
                    letters = state.currentInput.padEnd(WORD_LENGTH).map { it.uppercaseChar() },
                    states = List(WORD_LENGTH) { TileState.EMPTY },
                    submitted = false,
                )
        }

        while (rows.size < MAX_ATTEMPTS) {
            rows +=
                WordlyBoardRow(
                    letters = List(WORD_LENGTH) { ' ' },
                    states = List(WORD_LENGTH) { TileState.EMPTY },
                    submitted = false,
                )
        }

        return rows
    }

    fun keyboardStates(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): Map<Char, TileState> {
        val keys = mutableMapOf<Char, TileState>()
        state.attempts.forEach { guess ->
            WordlyScorer.score(puzzle.solution, guess).forEach { score ->
                keys[score.letter.uppercaseChar()] = bestState(keys[score.letter.uppercaseChar()], score.state)
            }
        }
        return keys
    }

    fun learningSummary(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlyLearningSummary =
        when {
            state.isCompleted ->
                WordlyLearningSummary(
                    title = "Solved in ${state.attempts.size}/$MAX_ATTEMPTS",
                    body = puzzle.definition,
                    example = puzzle.exampleSentence,
                    morphology = puzzle.morphologyNote,
                )
            state.isFailed ->
                WordlyLearningSummary(
                    title = "Good practice",
                    body = "The hidden word was ${puzzle.solution.uppercase()}. ${puzzle.definition}",
                    example = puzzle.exampleSentence,
                    morphology = puzzle.morphologyNote,
                )
            else ->
                WordlyLearningSummary(
                    title = "Keep exploring",
                    body = "Use each row to test five letters.",
                    example = null,
                    morphology = null,
                )
        }

    fun shareCard(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
        utcDate: String,
        currentStreak: Int,
        bestStreak: Int,
    ): ShareCardModel =
        ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = utcDate,
            cardType = ShareCardType.INDIVIDUAL_RESULT,
            visibleResultPattern = sharePattern(puzzle, state),
            hintsUsed = state.revealedHintOrders.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            forbiddenPayloads = listOf(puzzle.solution) + state.attempts,
        )

    fun encode(state: WordlySaveState): String = json.encodeToString(state)

    fun decode(payload: String): WordlySaveState = json.decodeFromString(payload)

    private fun sanitise(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): WordlySaveState {
        if (state.puzzleId != puzzle.id) return initial(puzzle)
        return state.copy(
            attempts =
                state.attempts
                    .map { attempt -> attempt.lowercase().filter { it.isLetter() }.take(WORD_LENGTH) }
                    .filter { it.length == WORD_LENGTH }
                    .take(MAX_ATTEMPTS),
            currentInput =
                state.currentInput
                    .lowercase()
                    .filter { it.isLetter() }
                    .take(WORD_LENGTH),
            revealedHintOrders =
                state.revealedHintOrders
                    .distinct()
                    .filter { order -> puzzle.hints.any { it.order == order } },
        )
    }

    private fun isValidGuess(
        puzzle: WordlyPuzzle,
        guess: String,
    ): Boolean {
        val allowed = puzzle.validGuesses.map { it.lowercase() }.toSet()
        return guess.lowercase() in allowed
    }

    private fun bestState(
        current: TileState?,
        next: TileState,
    ): TileState {
        val currentState = current ?: TileState.EMPTY
        return if (rank(next) > rank(currentState)) next else currentState
    }

    private fun rank(state: TileState): Int =
        when (state) {
            TileState.EMPTY -> 0
            TileState.ABSENT -> 1
            TileState.PRESENT -> 2
            TileState.CORRECT -> 3
        }

    private fun sharePattern(
        puzzle: WordlyPuzzle,
        state: WordlySaveState,
    ): String =
        state.attempts.joinToString(separator = "\n") { guess ->
            WordlyScorer
                .score(puzzle.solution, guess)
                .joinToString(separator = "-") { score -> score.state.shareLabel }
        }

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

@Serializable
data class WordlySaveState(
    val puzzleId: String,
    val attempts: List<String> = emptyList(),
    val currentInput: String = "",
    val revealedHintOrders: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val completionAcknowledged: Boolean = false,
) {
    val isTerminal: Boolean
        get() = isCompleted || isFailed
}

data class WordlyMoveResult(
    val state: WordlySaveState,
    val message: WordlyMessage? = null,
    val completionEvent: WordlyCompletionEvent? = null,
)

data class WordlyCompletionEvent(
    val outcome: WordlyOutcome,
    val puzzleId: String,
    val attempts: Int,
    val hintsUsed: Int,
)

data class WordlyBoardRow(
    val letters: List<Char>,
    val states: List<TileState>,
    val submitted: Boolean,
)

data class WordlyLearningSummary(
    val title: String,
    val body: String,
    val example: String?,
    val morphology: String?,
)

enum class WordlyOutcome {
    SUCCESS,
    FAILURE,
}

enum class WordlyMessage(
    val userText: String,
) {
    LETTERS_ONLY("Use letters A to Z."),
    TOO_SHORT("Type five letters before checking."),
    INVALID_GUESS("That is not in today's word list."),
    ALREADY_FINISHED("This Wordly is finished."),
    GUESS_ACCEPTED("Good thinking. Try the clues from the colours."),
    SOLVED("You found the hidden word."),
    OUT_OF_ATTEMPTS("That was the last row. Review the word and try again tomorrow."),
    HINT_REVEALED("Hint unlocked."),
    NO_HINTS_LEFT("All hints are already open."),
}

val TileState.shareLabel: String
    get() =
        when (this) {
            TileState.CORRECT -> "correct"
            TileState.PRESENT -> "present"
            TileState.ABSENT -> "absent"
            TileState.EMPTY -> "empty"
        }
