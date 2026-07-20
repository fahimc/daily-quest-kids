package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ConnectionGroup
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ConnectionsGameEngine {
    fun initial(puzzle: ConnectionsPuzzle): ConnectionsSaveState =
        ConnectionsSaveState(
            puzzleId = puzzle.id,
            tileOrder = shuffledOrder(puzzle.id, puzzle.groups.flatMap { it.words }),
        )

    fun tiles(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): List<ConnectionsTile> {
        val safeState = sanitise(puzzle, state)
        val solvedWords = solvedWords(puzzle, safeState)
        return safeState.tileOrder
            .filterNot { word -> normalise(word) in solvedWords }
            .map { word ->
                ConnectionsTile(
                    word = word,
                    isSelected = normalise(word) in safeState.selectedWords,
                )
            }
    }

    fun solvedGroups(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): List<ConnectionsSolvedGroup> {
        val safeState = sanitise(puzzle, state)
        return safeState.solvedGroupTitles.mapNotNull { title ->
            val group = puzzle.groups.firstOrNull { it.title == title }
            group?.let {
                ConnectionsSolvedGroup(
                    title = it.title,
                    words = it.words,
                    explanation = it.explanation,
                )
            }
        }
    }

    fun toggleTile(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
        word: String,
    ): ConnectionsSaveState {
        val safeState = sanitise(puzzle, state)
        val key = normalise(word)
        val activeWords = activeWords(puzzle, safeState)
        if (safeState.isCompleted || safeState.isFailed || key !in activeWords) return safeState
        val nextSelected =
            when {
                key in safeState.selectedWords -> safeState.selectedWords - key
                safeState.selectedWords.size >= GROUP_SIZE -> safeState.selectedWords
                else -> safeState.selectedWords + key
            }
        return safeState.copy(selectedWords = nextSelected, lastMistakeWords = emptyList())
    }

    fun deselectAll(state: ConnectionsSaveState): ConnectionsSaveState =
        state.copy(selectedWords = emptyList(), lastMistakeWords = emptyList())

    fun shuffle(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): ConnectionsSaveState {
        val safeState = sanitise(puzzle, state)
        val remaining = safeState.tileOrder.filter { word -> normalise(word) in activeWords(puzzle, safeState) }
        val shuffled = rotated(remaining, safeState.shuffleCount + 1)
        val solved = puzzle.groups.filter { it.title in safeState.solvedGroupTitles }.flatMap { it.words }
        return safeState.copy(
            tileOrder = solved + shuffled,
            shuffleCount = safeState.shuffleCount + 1,
            lastMistakeWords = emptyList(),
        )
    }

    fun submit(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): ConnectionsMoveResult {
        val safeState = sanitise(puzzle, state)
        return when {
            safeState.isCompleted -> ConnectionsMoveResult(safeState, ConnectionsMessage.ALREADY_COMPLETE)
            safeState.isFailed -> ConnectionsMoveResult(safeState, ConnectionsMessage.FAILED)
            safeState.selectedWords.size != GROUP_SIZE -> ConnectionsMoveResult(safeState, ConnectionsMessage.SELECT_FOUR)
            else -> {
                val selected = safeState.selectedWords.toSet()
                val group = puzzle.groups.firstOrNull { group -> group.words.map(::normalise).toSet() == selected }
                if (group == null) {
                    incorrectSubmission(safeState, selected)
                } else {
                    correctSubmission(puzzle, safeState, group)
                }
            }
        }
    }

    fun revealHint(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
        confirmReveal: Boolean = false,
    ): ConnectionsMoveResult {
        val safeState = sanitise(puzzle, state)
        val nextHint = hintOrders(puzzle).firstOrNull { it !in safeState.revealedHintOrders }
        val target = nextUnsolvedGroup(puzzle, safeState)
        return when {
            nextHint == null -> ConnectionsMoveResult(safeState, ConnectionsMessage.NO_HINTS_LEFT)
            target == null -> ConnectionsMoveResult(safeState, ConnectionsMessage.ALREADY_COMPLETE)
            shouldPromptForGroupReveal(nextHint, confirmReveal, safeState) ->
                ConnectionsMoveResult(
                    safeState.copy(awaitingRevealConfirmation = true),
                    ConnectionsMessage.CONFIRM_REVEAL,
                )
            else -> applyHint(puzzle, safeState, target, nextHint)
        }
    }

    private fun shouldPromptForGroupReveal(
        nextHint: Int,
        confirmReveal: Boolean,
        state: ConnectionsSaveState,
    ): Boolean = nextHint == 4 && !confirmReveal && !state.awaitingRevealConfirmation

    private fun applyHint(
        puzzle: ConnectionsPuzzle,
        safeState: ConnectionsSaveState,
        target: ConnectionGroup,
        nextHint: Int,
    ): ConnectionsMoveResult {
        val hintedState =
            when (nextHint) {
                2 -> safeState.copy(selectedWords = target.words.take(2).map(::normalise))
                3 -> safeState.copy(highlightedGroupTitle = target.title)
                4 -> solveGroup(puzzle, safeState, target)
                else -> safeState
            }.copy(
                revealedHintOrders = (safeState.revealedHintOrders + nextHint).distinct(),
                awaitingRevealConfirmation = false,
            )
        val completedState = hintedState.withCompletion(puzzle)
        return ConnectionsMoveResult(
            state = completedState,
            message = hintMessage(nextHint),
            completionEvent = pendingCompletion(completedState),
        )
    }

    private fun hintMessage(nextHint: Int): ConnectionsMessage =
        when (nextHint) {
            1 -> ConnectionsMessage.HINT_REVEALED
            2 -> ConnectionsMessage.PAIR_REVEALED
            3 -> ConnectionsMessage.TITLE_REVEALED
            4 -> ConnectionsMessage.GROUP_REVEALED
            else -> ConnectionsMessage.HINT_REVEALED
        }

    fun revealedHintTexts(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): List<String> {
        val safeState = sanitise(puzzle, state)
        val target = nextUnsolvedGroup(puzzle, safeState) ?: puzzle.groups.first()
        return safeState.revealedHintOrders.map { order ->
            when (order) {
                1 -> "One group is about ${target.title.lowercase()}."
                2 -> "${target.words[0]} and ${target.words[1]} belong together."
                3 -> "A group title is ${target.title}."
                4 -> "A group can be revealed if you ask again."
                else ->
                    puzzle.hints
                        .firstOrNull { it.order == order }
                        ?.text
                        .orEmpty()
            }
        }
    }

    fun acknowledgeCompletion(state: ConnectionsSaveState): ConnectionsSaveState =
        if (state.isCompleted) state.copy(completionAcknowledged = true) else state

    fun pendingCompletion(state: ConnectionsSaveState): ConnectionsCompletionEvent? =
        if (state.isCompleted && !state.completionAcknowledged) {
            ConnectionsCompletionEvent(
                puzzleId = state.puzzleId,
                hintsUsed = state.revealedHintOrders.size,
            )
        } else {
            null
        }

    fun shareCard(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
        utcDate: String,
        currentStreak: Int,
        bestStreak: Int,
    ): ShareCardModel {
        val safeState = sanitise(puzzle, state)
        val status =
            if (safeState.isCompleted) {
                "Complete"
            } else if (safeState.isFailed) {
                "Try again tomorrow"
            } else {
                "In progress"
            }
        return ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = utcDate,
            cardType = ShareCardType.INDIVIDUAL_RESULT,
            visibleResultPattern =
                listOf(
                    "Connections ${sharePuzzleNumber(puzzle.id)}",
                    "Groups ${safeState.solvedGroupTitles.size}/$GROUP_SIZE",
                    "Mistakes ${safeState.mistakeCount}/$MAX_MISTAKES",
                    "Hints ${safeState.revealedHintOrders.size}",
                    "Streak $currentStreak",
                    status,
                ).joinToString(separator = "\n"),
            hintsUsed = safeState.revealedHintOrders.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            forbiddenPayloads = puzzle.groups.flatMap { group -> group.words + group.title },
        )
    }

    fun encode(state: ConnectionsSaveState): String = json.encodeToString(state)

    fun decode(payload: String): ConnectionsSaveState = json.decodeFromString(payload)

    private fun correctSubmission(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
        group: ConnectionGroup,
    ): ConnectionsMoveResult {
        val nextState = solveGroup(puzzle, state, group).withCompletion(puzzle)
        return ConnectionsMoveResult(
            state = nextState,
            message = if (nextState.isCompleted) ConnectionsMessage.PUZZLE_COMPLETE else ConnectionsMessage.GROUP_SOLVED,
            completionEvent = pendingCompletion(nextState),
        )
    }

    private fun incorrectSubmission(
        state: ConnectionsSaveState,
        selected: Set<String>,
    ): ConnectionsMoveResult {
        val mistakes = state.mistakeCount + 1
        val failed = mistakes >= MAX_MISTAKES
        val nextState =
            state.copy(
                mistakeCount = mistakes,
                lastMistakeWords = selected.toList(),
                selectedWords = emptyList(),
                isFailed = failed,
            )
        return ConnectionsMoveResult(
            nextState,
            if (failed) ConnectionsMessage.FAILED else ConnectionsMessage.INCORRECT,
        )
    }

    private fun solveGroup(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
        group: ConnectionGroup,
    ): ConnectionsSaveState {
        val solvedTitles = (state.solvedGroupTitles + group.title).distinct()
        val solvedWords =
            puzzle.groups
                .filter { it.title in solvedTitles }
                .flatMap { it.words.map(::normalise) }
                .toSet()
        val activeTileOrder = state.tileOrder.filterNot { normalise(it) in solvedWords }
        val solvedTileOrder =
            puzzle.groups
                .flatMap { it.words }
                .filter { normalise(it) in solvedWords }
        return state.copy(
            solvedGroupTitles = solvedTitles,
            selectedWords = emptyList(),
            lastMistakeWords = emptyList(),
            tileOrder = activeTileOrder + solvedTileOrder,
            awaitingRevealConfirmation = false,
        )
    }

    private fun ConnectionsSaveState.withCompletion(puzzle: ConnectionsPuzzle): ConnectionsSaveState =
        copy(
            isCompleted = solvedGroupTitles.toSet() == puzzle.groups.map { it.title }.toSet(),
            completionAcknowledged = false,
        )

    private fun sanitise(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): ConnectionsSaveState {
        if (state.puzzleId != puzzle.id) return initial(puzzle)
        val allWords = puzzle.groups.flatMap { it.words }
        val wordKeys = allWords.map(::normalise).toSet()
        val titles = puzzle.groups.map { it.title }.toSet()
        val solvedTitles = state.solvedGroupTitles.filter { it in titles }.distinct()
        val solvedKeys =
            puzzle.groups
                .filter { it.title in solvedTitles }
                .flatMap { it.words.map(::normalise) }
                .toSet()
        val tileOrder =
            (state.tileOrder.filter { normalise(it) in wordKeys }.distinctBy(::normalise) + allWords)
                .distinctBy(::normalise)
        val selected = state.selectedWords.filter { it in wordKeys && it !in solvedKeys }.take(GROUP_SIZE)
        val completed = solvedTitles.size == GROUP_SIZE
        return state.copy(
            tileOrder = tileOrder,
            selectedWords = selected,
            solvedGroupTitles = solvedTitles,
            mistakeCount = state.mistakeCount.coerceIn(0, MAX_MISTAKES),
            revealedHintOrders = state.revealedHintOrders.distinct().filter { it in hintOrders(puzzle) },
            lastMistakeWords = state.lastMistakeWords.filter { it in wordKeys },
            highlightedGroupTitle = state.highlightedGroupTitle?.takeIf { it in titles },
            awaitingRevealConfirmation = state.awaitingRevealConfirmation && 4 !in state.revealedHintOrders,
            isCompleted = completed,
            isFailed = !completed && state.isFailed,
            completionAcknowledged = if (completed) state.completionAcknowledged else false,
        )
    }

    private fun nextUnsolvedGroup(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): ConnectionGroup? = puzzle.groups.firstOrNull { it.title !in state.solvedGroupTitles }

    private fun solvedWords(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): Set<String> =
        puzzle.groups
            .filter { it.title in state.solvedGroupTitles }
            .flatMap { it.words.map(::normalise) }
            .toSet()

    private fun activeWords(
        puzzle: ConnectionsPuzzle,
        state: ConnectionsSaveState,
    ): Set<String> = puzzle.groups.flatMap { it.words.map(::normalise) }.toSet() - solvedWords(puzzle, state)

    private fun hintOrders(puzzle: ConnectionsPuzzle): List<Int> =
        puzzle.hints
            .map { it.order }
            .sorted()
            .take(MAX_HINTS)

    private fun shuffledOrder(
        puzzleId: String,
        words: List<String>,
    ): List<String> {
        val offset = puzzleId.fold(0) { acc, char -> acc + char.code }.mod(words.size.coerceAtLeast(1))
        return rotated(words, offset)
    }

    private fun rotated(
        words: List<String>,
        offset: Int,
    ): List<String> {
        if (words.isEmpty()) return emptyList()
        val safeOffset = offset.mod(words.size)
        return words.drop(safeOffset) + words.take(safeOffset)
    }

    private fun normalise(word: String): String = word.trim().lowercase()

    private const val GROUP_SIZE = 4
    private const val MAX_MISTAKES = 5
    private const val MAX_HINTS = 4

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

@Serializable
data class ConnectionsSaveState(
    val puzzleId: String,
    val tileOrder: List<String>,
    val selectedWords: List<String> = emptyList(),
    val solvedGroupTitles: List<String> = emptyList(),
    val mistakeCount: Int = 0,
    val revealedHintOrders: List<Int> = emptyList(),
    val lastMistakeWords: List<String> = emptyList(),
    val highlightedGroupTitle: String? = null,
    val awaitingRevealConfirmation: Boolean = false,
    val shuffleCount: Int = 0,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val completionAcknowledged: Boolean = false,
)

data class ConnectionsTile(
    val word: String,
    val isSelected: Boolean,
)

data class ConnectionsSolvedGroup(
    val title: String,
    val words: List<String>,
    val explanation: String,
)

data class ConnectionsMoveResult(
    val state: ConnectionsSaveState,
    val message: ConnectionsMessage? = null,
    val completionEvent: ConnectionsCompletionEvent? = null,
)

data class ConnectionsCompletionEvent(
    val puzzleId: String,
    val hintsUsed: Int,
)

enum class ConnectionsMessage(
    val userText: String,
) {
    SELECT_FOUR("Pick exactly four words first."),
    INCORRECT("Not a group yet. Try a different link."),
    GROUP_SOLVED("Group found."),
    PUZZLE_COMPLETE("Connections complete."),
    FAILED("No more tries today. Come back tomorrow."),
    ALREADY_COMPLETE("Connections is already complete."),
    HINT_REVEALED("Hint unlocked."),
    PAIR_REVEALED("Two linked words are selected."),
    TITLE_REVEALED("Group title revealed."),
    CONFIRM_REVEAL("Tap hint again to reveal a group."),
    GROUP_REVEALED("A group was revealed."),
    NO_HINTS_LEFT("No hints left."),
}
