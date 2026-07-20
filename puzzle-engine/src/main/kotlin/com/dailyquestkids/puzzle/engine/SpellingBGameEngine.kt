package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SpellingWord
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SpellingBGameEngine {
    const val MIN_WORD_LENGTH = 3
    const val PANGRAM_BONUS = 7
    const val MAX_HINTS = 4

    fun initial(puzzle: SpellingBeePuzzle): SpellingBSaveState =
        SpellingBSaveState(
            puzzleId = puzzle.id,
            shuffledOuterLetters = defaultOuterLetters(puzzle),
        )

    fun appendLetter(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
        letter: Char,
    ): SpellingBMoveResult {
        val safeState = sanitise(puzzle, state)
        val normalised = letter.lowercaseChar()
        return when {
            safeState.isCompleted -> SpellingBMoveResult(safeState, SpellingBMessage.ALREADY_FINISHED)
            !normalised.isLetter() -> SpellingBMoveResult(safeState, SpellingBMessage.LETTERS_ONLY)
            normalised !in letterSet(puzzle) -> SpellingBMoveResult(safeState, SpellingBMessage.INVALID_LETTER)
            safeState.currentInput.length >= maxInputLength(puzzle) -> SpellingBMoveResult(safeState)
            else -> SpellingBMoveResult(safeState.copy(currentInput = safeState.currentInput + normalised))
        }
    }

    fun deleteLetter(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBSaveState {
        val safeState = sanitise(puzzle, state)
        if (safeState.isCompleted || safeState.currentInput.isEmpty()) return safeState
        return safeState.copy(currentInput = safeState.currentInput.dropLast(1))
    }

    fun clearInput(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBSaveState = sanitise(puzzle, state).copy(currentInput = "")

    fun shuffle(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBSaveState {
        val safeState = sanitise(puzzle, state)
        val current = outerLetters(puzzle, safeState)
        val shuffled =
            if (current.size <= 1) {
                current
            } else {
                current.drop(1) + current.take(1)
            }
        return safeState.copy(shuffledOuterLetters = shuffled)
    }

    fun submit(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBMoveResult {
        val safeState = sanitise(puzzle, state)
        val word = safeState.currentInput.lowercase()
        val targetWords = targetWordSet(puzzle)
        val centre = puzzle.centreLetter.lowercaseChar()
        val letters = letterSet(puzzle)

        return when {
            safeState.isCompleted -> SpellingBMoveResult(safeState, SpellingBMessage.ALREADY_FINISHED)
            word.length < MIN_WORD_LENGTH -> SpellingBMoveResult(safeState, SpellingBMessage.TOO_SHORT)
            word.any { !it.isLetter() } -> SpellingBMoveResult(safeState, SpellingBMessage.LETTERS_ONLY)
            word.any { it !in letters } -> SpellingBMoveResult(safeState, SpellingBMessage.INVALID_LETTER)
            centre !in word -> SpellingBMoveResult(safeState, SpellingBMessage.MISSING_CENTRE)
            word in safeState.foundWords -> SpellingBMoveResult(safeState, SpellingBMessage.ALREADY_FOUND)
            word !in targetWords -> SpellingBMoveResult(safeState, SpellingBMessage.NOT_IN_LIST)
            else -> recordFoundWord(puzzle, safeState, word)
        }
    }

    fun revealHint(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBMoveResult {
        val safeState = sanitise(puzzle, state)
        val nextOrder =
            hintOrders(puzzle)
                .firstOrNull { it !in safeState.revealedHintOrders }
                ?: return SpellingBMoveResult(safeState, SpellingBMessage.NO_HINTS_LEFT)

        return SpellingBMoveResult(
            state = safeState.copy(revealedHintOrders = safeState.revealedHintOrders + nextOrder),
            message = SpellingBMessage.HINT_REVEALED,
        )
    }

    fun acknowledgeCompletion(state: SpellingBSaveState): SpellingBSaveState =
        if (state.isCompleted) state.copy(completionAcknowledged = true) else state

    fun pendingCompletion(state: SpellingBSaveState): SpellingBCompletionEvent? =
        if (state.isCompleted && !state.completionAcknowledged) {
            SpellingBCompletionEvent(
                puzzleId = state.puzzleId,
                wordsFound = state.foundWords.size,
                hintsUsed = state.revealedHintOrders.size,
            )
        } else {
            null
        }

    fun score(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): Int = sanitise(puzzle, state).foundWords.sumOf { word -> scoreForWord(puzzle, word) }

    fun totalScore(puzzle: SpellingBeePuzzle): Int =
        puzzle.targetWords
            .map { it.word.lowercase() }
            .distinct()
            .sumOf { word -> scoreForWord(puzzle, word) }

    fun scoreForWord(
        puzzle: SpellingBeePuzzle,
        word: String,
    ): Int {
        val normalised = word.lowercase()
        val base = if (normalised.length <= 4) 1 else normalised.length
        val bonus = if (usesEveryLetter(puzzle, normalised)) PANGRAM_BONUS else 0
        return base + bonus
    }

    fun achievement(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBAchievement {
        val total = totalScore(puzzle).coerceAtLeast(1)
        val fraction = score(puzzle, state).toFloat() / total.toFloat()
        val title =
            when {
                fraction >= 1f -> "Queen Bee"
                fraction >= 0.75f -> "Amazing"
                fraction >= 0.55f -> "Hive Hero"
                fraction >= 0.35f -> "Word Builder"
                fraction >= 0.15f -> "Explorer"
                else -> "Starter"
            }
        return SpellingBAchievement(title = title, progress = fraction.coerceIn(0f, 1f))
    }

    fun outerLetters(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): List<Char> {
        val default = defaultOuterLetters(puzzle)
        val candidate = state.shuffledOuterLetters.map { it.lowercaseChar() }
        return if (candidate.size == default.size && candidate.toSet() == default.toSet()) candidate else default
    }

    fun foundWordRows(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): List<SpellingBFoundWord> {
        val wordsByText = puzzle.targetWords.associateBy { it.word.lowercase() }
        return sanitise(puzzle, state).foundWords.mapNotNull { word ->
            wordsByText[word]?.let {
                SpellingBFoundWord(
                    word = word,
                    definition = it.definition,
                    isPangram = usesEveryLetter(puzzle, word),
                )
            }
        }
    }

    fun revealedHintTexts(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): List<String> =
        sanitise(puzzle, state).revealedHintOrders.map { order ->
            hintText(puzzle, state, order)
        }

    fun shareCard(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
        utcDate: String,
        currentStreak: Int,
        bestStreak: Int,
    ): ShareCardModel {
        val safeState = sanitise(puzzle, state)
        return ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = utcDate,
            cardType = ShareCardType.INDIVIDUAL_RESULT,
            visibleResultPattern =
                listOf(
                    "Spelling B",
                    "Points ${score(puzzle, safeState)}/${totalScore(puzzle)}",
                    "Found ${safeState.foundWords.size}/${puzzle.targetWords.size}",
                    "Rank ${achievement(puzzle, safeState).title}",
                    "Full-letter words ${safeState.foundWords.count { usesEveryLetter(puzzle, it) }}",
                ).joinToString(separator = "\n"),
            hintsUsed = safeState.revealedHintOrders.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            forbiddenPayloads = puzzle.targetWords.map { it.word },
        )
    }

    fun encode(state: SpellingBSaveState): String = json.encodeToString(state)

    fun decode(payload: String): SpellingBSaveState = json.decodeFromString(payload)

    private fun recordFoundWord(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
        word: String,
    ): SpellingBMoveResult {
        val foundWords = state.foundWords + word
        val completed = targetWordSet(puzzle).all { it in foundWords }
        val nextState =
            state.copy(
                foundWords = foundWords,
                currentInput = "",
                isCompleted = completed,
                completionAcknowledged = false,
            )
        val pangram = usesEveryLetter(puzzle, word)
        return SpellingBMoveResult(
            state = nextState,
            message =
                when {
                    completed -> SpellingBMessage.PUZZLE_COMPLETE
                    pangram -> SpellingBMessage.PANGRAM_FOUND
                    else -> SpellingBMessage.WORD_FOUND
                },
            completionEvent = pendingCompletion(nextState),
        )
    }

    private fun sanitise(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): SpellingBSaveState {
        if (state.puzzleId != puzzle.id) return initial(puzzle)

        val targets = targetWordSet(puzzle)
        val letters = letterSet(puzzle)
        val safeFound =
            state.foundWords
                .map { it.lowercase().filter(Char::isLetter) }
                .filter { it in targets }
                .distinct()
        val safeInput =
            state.currentInput
                .lowercase()
                .filter { it.isLetter() && it in letters }
                .take(maxInputLength(puzzle))
        val safeHintOrders =
            state.revealedHintOrders
                .distinct()
                .filter { it in hintOrders(puzzle) }
        val defaultOuter = defaultOuterLetters(puzzle)
        val candidateOuter = state.shuffledOuterLetters.map { it.lowercaseChar() }
        val safeOuter =
            if (candidateOuter.size == defaultOuter.size && candidateOuter.toSet() == defaultOuter.toSet()) {
                candidateOuter
            } else {
                defaultOuter
            }

        return state.copy(
            foundWords = safeFound,
            currentInput = safeInput,
            revealedHintOrders = safeHintOrders,
            shuffledOuterLetters = safeOuter,
            isCompleted = targets.isNotEmpty() && targets.all { it in safeFound },
            completionAcknowledged = if (targets.all { it in safeFound }) state.completionAcknowledged else false,
        )
    }

    private fun hintText(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
        order: Int,
    ): String {
        val remaining = remainingTargets(puzzle, state)
        val nextWord = remaining.firstOrNull()
        return when (order) {
            1 -> startingLetterCounts(puzzle)
            2 ->
                nextWord
                    ?.let { "An undiscovered word has ${it.word.length} letters." }
                    ?: "Every word is found."
            3 ->
                nextWord
                    ?.let { "Definition: ${it.definition}" }
                    ?: "Every definition is open."
            4 ->
                nextWord
                    ?.let { "One undiscovered word starts with ${it.word.first().uppercaseChar()}." }
                    ?: "Every first letter is known."
            else ->
                puzzle.hints
                    .firstOrNull { it.order == order }
                    ?.text
                    .orEmpty()
        }
    }

    private fun startingLetterCounts(puzzle: SpellingBeePuzzle): String =
        puzzle.targetWords
            .map { it.word.lowercase() }
            .distinct()
            .groupingBy { it.first().uppercaseChar() }
            .eachCount()
            .toSortedMap()
            .entries
            .joinToString(prefix = "Starts: ", separator = ", ") { (letter, count) -> "$letter $count" }

    private fun remainingTargets(
        puzzle: SpellingBeePuzzle,
        state: SpellingBSaveState,
    ): List<SpellingWord> {
        val found = sanitise(puzzle, state).foundWords.toSet()
        return puzzle.targetWords
            .filter { it.word.lowercase() !in found }
            .sortedWith(compareBy<SpellingWord> { it.word.length }.thenBy { it.word })
    }

    private fun maxInputLength(puzzle: SpellingBeePuzzle): Int =
        puzzle.targetWords.maxOfOrNull { it.word.length }?.coerceAtLeast(MIN_WORD_LENGTH) ?: 18

    private fun usesEveryLetter(
        puzzle: SpellingBeePuzzle,
        word: String,
    ): Boolean = letterSet(puzzle).all { it in word.lowercase() }

    private fun targetWordSet(puzzle: SpellingBeePuzzle): Set<String> = puzzle.targetWords.map { it.word.lowercase() }.toSet()

    private fun letterSet(puzzle: SpellingBeePuzzle): Set<Char> = puzzle.letters.map { it.lowercaseChar() }.toSet()

    private fun defaultOuterLetters(puzzle: SpellingBeePuzzle): List<Char> {
        val centre = puzzle.centreLetter.lowercaseChar()
        return puzzle.letters
            .map { it.lowercaseChar() }
            .filterNot { it == centre }
    }

    private fun hintOrders(puzzle: SpellingBeePuzzle): List<Int> =
        puzzle.hints
            .map { it.order }
            .sorted()
            .take(MAX_HINTS)

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

@Serializable
data class SpellingBSaveState(
    val puzzleId: String,
    val foundWords: List<String> = emptyList(),
    val currentInput: String = "",
    val revealedHintOrders: List<Int> = emptyList(),
    val shuffledOuterLetters: List<Char> = emptyList(),
    val isCompleted: Boolean = false,
    val completionAcknowledged: Boolean = false,
)

data class SpellingBMoveResult(
    val state: SpellingBSaveState,
    val message: SpellingBMessage? = null,
    val completionEvent: SpellingBCompletionEvent? = null,
)

data class SpellingBCompletionEvent(
    val puzzleId: String,
    val wordsFound: Int,
    val hintsUsed: Int,
)

data class SpellingBAchievement(
    val title: String,
    val progress: Float,
)

data class SpellingBFoundWord(
    val word: String,
    val definition: String,
    val isPangram: Boolean,
)

enum class SpellingBMessage(
    val userText: String,
) {
    LETTERS_ONLY("Use letters A to Z."),
    INVALID_LETTER("That letter is not in this hive."),
    TOO_SHORT("Make a word with at least three letters."),
    MISSING_CENTRE("Every word must use the middle letter."),
    NOT_IN_LIST("Not in today's word list."),
    ALREADY_FOUND("You already found that word."),
    WORD_FOUND("Word found."),
    PANGRAM_FOUND("Great find: it uses every hive letter."),
    PUZZLE_COMPLETE("Hive complete."),
    HINT_REVEALED("Hint unlocked."),
    NO_HINTS_LEFT("All hints are already open."),
    ALREADY_FINISHED("This Spelling B is finished."),
}
