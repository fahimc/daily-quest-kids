package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CrosswordGameEngine {
    fun initial(puzzle: CrosswordPuzzle): CrosswordSaveState {
        val firstEntry = numberedEntries(puzzle).firstOrNull()
        return CrosswordSaveState(
            puzzleId = puzzle.id,
            activeRow = firstEntry?.row ?: 0,
            activeColumn = firstEntry?.column ?: 0,
            activeDirection = firstEntry?.direction ?: CrosswordDirection.ACROSS,
            activeEntryIndex = firstEntry?.entryIndex ?: 0,
        )
    }

    fun boardCells(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): List<CrosswordBoardCell> {
        val safeState = sanitise(puzzle, state)
        val solution = solutionCells(puzzle)
        val numbers = cellNumbers(puzzle)
        val activeEntry = selectedEntry(puzzle, safeState)
        val activeCells = activeEntry?.let { entryCells(puzzle, it.entryIndex).toSet() }.orEmpty()
        val activeIndex = cellIndex(puzzle, safeState.activeRow, safeState.activeColumn)

        return (0 until puzzle.height).flatMap { row ->
            (0 until puzzle.width).map { column ->
                val index = cellIndex(puzzle, row, column)
                val solutionLetter = solution[index]
                val playerLetter = safeState.cellValues[index]?.firstOrNull()?.uppercaseChar()
                CrosswordBoardCell(
                    row = row,
                    column = column,
                    index = index,
                    number = numbers[index],
                    solutionLetter = solutionLetter?.uppercaseChar(),
                    playerLetter = playerLetter,
                    isPlayable = solutionLetter != null,
                    isActive = index == activeIndex,
                    isInActiveEntry = index in activeCells,
                    isRevealed = index in safeState.revealedCellIndices,
                    isCorrect =
                        playerLetter != null &&
                            solutionLetter != null &&
                            playerLetter.lowercaseChar() == solutionLetter.lowercaseChar(),
                )
            }
        }
    }

    fun numberedEntries(puzzle: CrosswordPuzzle): List<CrosswordClue> {
        val numbers = cellNumbers(puzzle)
        val clues =
            puzzle.entries.mapIndexed { index, entry ->
                CrosswordClue(
                    entryIndex = index,
                    number = numbers[cellIndex(puzzle, entry.row, entry.column)] ?: index + 1,
                    direction = entry.direction,
                    clue = entry.clue,
                    answerLength = entry.answer.length,
                    row = entry.row,
                    column = entry.column,
                )
            }
        return clues.sortedWith(compareBy<CrosswordClue> { it.number }.thenBy { it.direction.name })
    }

    fun clueStates(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): List<CrosswordClueState> {
        val safeState = sanitise(puzzle, state)
        val selected = selectedEntry(puzzle, safeState)?.entryIndex
        return numberedEntries(puzzle).map { clue ->
            CrosswordClueState(
                clue = clue,
                isSelected = clue.entryIndex == selected,
                isSolved = entrySolved(puzzle, safeState, clue.entryIndex),
            )
        }
    }

    fun selectCell(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        row: Int,
        column: Int,
    ): CrosswordSaveState {
        val safeState = sanitise(puzzle, state)
        val index = cellIndex(puzzle, row, column)
        if (index !in solutionCells(puzzle)) return safeState
        val currentEntry = selectedEntry(puzzle, safeState)
        val sameCell = row == safeState.activeRow && column == safeState.activeColumn
        val preferredDirection =
            if (sameCell) {
                opposite(safeState.activeDirection)
            } else {
                safeState.activeDirection
            }
        val entry =
            containingEntry(puzzle, index, preferredDirection)
                ?: containingEntry(puzzle, index, safeState.activeDirection)
                ?: containingEntry(puzzle, index, opposite(safeState.activeDirection))
                ?: currentEntry
                ?: numberedEntries(puzzle).firstOrNull()
        return safeState.copy(
            activeRow = row,
            activeColumn = column,
            activeDirection = entry?.direction ?: safeState.activeDirection,
            activeEntryIndex = entry?.entryIndex ?: safeState.activeEntryIndex,
        )
    }

    fun selectEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        entryIndex: Int,
    ): CrosswordSaveState {
        val safeState = sanitise(puzzle, state)
        val entry = puzzle.entries.getOrNull(entryIndex) ?: return safeState
        return safeState.copy(
            activeRow = entry.row,
            activeColumn = entry.column,
            activeDirection = entry.direction,
            activeEntryIndex = entryIndex,
        )
    }

    fun toggleDirection(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordSaveState {
        val safeState = sanitise(puzzle, state)
        val activeIndex = cellIndex(puzzle, safeState.activeRow, safeState.activeColumn)
        val entry =
            containingEntry(puzzle, activeIndex, opposite(safeState.activeDirection))
                ?: containingEntry(puzzle, activeIndex, safeState.activeDirection)
                ?: selectedEntry(puzzle, safeState)
                ?: return safeState
        return safeState.copy(
            activeDirection = entry.direction,
            activeEntryIndex = entry.entryIndex,
        )
    }

    fun nextEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordSaveState = moveEntry(puzzle, state, step = 1)

    fun previousEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordSaveState = moveEntry(puzzle, state, step = -1)

    fun appendLetter(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        letter: Char,
    ): CrosswordMoveResult {
        val safeState = sanitise(puzzle, state)
        val normalised = letter.lowercaseChar()
        val activeIndex = cellIndex(puzzle, safeState.activeRow, safeState.activeColumn)
        val solution = solutionCells(puzzle)

        return when {
            safeState.isCompleted -> CrosswordMoveResult(safeState, CrosswordMessage.ALREADY_FINISHED)
            !normalised.isLetter() -> CrosswordMoveResult(safeState, CrosswordMessage.LETTERS_ONLY)
            activeIndex !in solution -> CrosswordMoveResult(safeState)
            activeIndex in safeState.revealedCellIndices -> CrosswordMoveResult(safeState, CrosswordMessage.REVEALED_CELL_LOCKED)
            else -> recordLetter(puzzle, safeState, activeIndex, normalised)
        }
    }

    fun deleteLetter(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordSaveState {
        val safeState = sanitise(puzzle, state)
        if (safeState.isCompleted) return safeState
        val activeEntry = selectedEntry(puzzle, safeState)
        return if (activeEntry == null) {
            safeState
        } else {
            val cells = entryCells(puzzle, activeEntry.entryIndex)
            val activeIndex = cellIndex(puzzle, safeState.activeRow, safeState.activeColumn)
            val position = cells.indexOf(activeIndex).coerceAtLeast(0)
            val targetIndex =
                if (activeIndex in safeState.cellValues && activeIndex !in safeState.revealedCellIndices) {
                    activeIndex
                } else {
                    cells.take(position).lastOrNull { it !in safeState.revealedCellIndices } ?: activeIndex
                }
            val nextValues = safeState.cellValues - targetIndex
            safeState.moveToCell(puzzle, targetIndex).copy(cellValues = nextValues)
        }
    }

    fun revealHint(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordMoveResult {
        val safeState = sanitise(puzzle, state)
        val nextHint = hintOrders(puzzle).firstOrNull { it !in safeState.revealedHintOrders }
        val activeEntry = selectedEntry(puzzle, safeState)
        return when {
            nextHint == null -> CrosswordMoveResult(safeState, CrosswordMessage.NO_HINTS_LEFT)
            activeEntry == null -> CrosswordMoveResult(safeState)
            else -> {
                val nextState =
                    when (nextHint) {
                        2 -> revealOneLetter(puzzle, safeState, activeEntry.entryIndex)
                        4 -> revealEntry(puzzle, safeState, activeEntry.entryIndex)
                        else -> safeState
                    }.copy(revealedHintOrders = safeState.revealedHintOrders + nextHint)
                val completed = isComplete(puzzle, nextState)
                val completedState = nextState.copy(isCompleted = completed, completionAcknowledged = false)
                CrosswordMoveResult(
                    state = completedState,
                    message =
                        when (nextHint) {
                            1 -> CrosswordMessage.CLUE_REPHRASED
                            2 -> CrosswordMessage.LETTER_REVEALED
                            3 ->
                                if (entrySolved(puzzle, nextState, activeEntry.entryIndex)) {
                                    CrosswordMessage.CHECK_CORRECT
                                } else {
                                    CrosswordMessage.CHECK_TRY_AGAIN
                                }
                            4 -> CrosswordMessage.ANSWER_REVEALED
                            else -> CrosswordMessage.HINT_REVEALED
                        },
                    completionEvent = pendingCompletion(completedState),
                )
            }
        }
    }

    fun revealedHintTexts(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): List<String> {
        val safeState = sanitise(puzzle, state)
        val activeEntry = selectedEntry(puzzle, safeState) ?: return emptyList()
        val entry = puzzle.entries[activeEntry.entryIndex]
        return safeState.revealedHintOrders.map { order ->
            when (order) {
                1 -> "Think of another way to say: ${entry.clue}"
                2 -> "One letter has been filled in."
                3 ->
                    if (entrySolved(puzzle, safeState, activeEntry.entryIndex)) {
                        "This answer is correct."
                    } else {
                        "Some letters need another look."
                    }
                4 -> "The selected answer has been filled in."
                else ->
                    puzzle.hints
                        .firstOrNull { it.order == order }
                        ?.text
                        .orEmpty()
            }
        }
    }

    fun acknowledgeCompletion(state: CrosswordSaveState): CrosswordSaveState =
        if (state.isCompleted) state.copy(completionAcknowledged = true) else state

    fun pendingCompletion(state: CrosswordSaveState): CrosswordCompletionEvent? =
        if (state.isCompleted && !state.completionAcknowledged) {
            CrosswordCompletionEvent(
                puzzleId = state.puzzleId,
                hintsUsed = state.revealedHintOrders.size,
            )
        } else {
            null
        }

    fun shareCard(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        utcDate: String,
        currentStreak: Int,
        bestStreak: Int,
    ): ShareCardModel {
        val safeState = sanitise(puzzle, state)
        val filled = safeState.cellValues.keys.count { it in solutionCells(puzzle) }
        val total = solutionCells(puzzle).size
        return ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = utcDate,
            cardType = ShareCardType.INDIVIDUAL_RESULT,
            visibleResultPattern =
                listOf(
                    "Crossword ${sharePuzzleNumber(puzzle.id)}",
                    "Clues solved ${solvedEntryCount(puzzle, safeState)}/${puzzle.entries.size}",
                    "Filled $filled/$total cells",
                    "Hints ${safeState.revealedHintOrders.size}",
                    "Streak $currentStreak",
                    if (safeState.isCompleted) "Complete" else "In progress",
                ).joinToString(separator = "\n"),
            hintsUsed = safeState.revealedHintOrders.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            forbiddenPayloads = puzzle.entries.map { it.answer },
        )
    }

    fun encode(state: CrosswordSaveState): String = json.encodeToString(state)

    fun decode(payload: String): CrosswordSaveState = json.decodeFromString(payload)

    private fun recordLetter(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        activeIndex: Int,
        letter: Char,
    ): CrosswordMoveResult {
        val activeEntry = selectedEntry(puzzle, state)
        val nextValues = state.cellValues + (activeIndex to letter.toString())
        val moved = state.copy(cellValues = nextValues).moveToNextCell(puzzle, activeIndex)
        val completed = isComplete(puzzle, moved)
        val completedState = moved.copy(isCompleted = completed, completionAcknowledged = false)
        val solvedEntry = activeEntry?.let { entrySolved(puzzle, completedState, it.entryIndex) } == true
        return CrosswordMoveResult(
            state = completedState,
            message =
                when {
                    completed -> CrosswordMessage.PUZZLE_COMPLETE
                    solvedEntry -> CrosswordMessage.ANSWER_COMPLETE
                    else -> null
                },
            completionEvent = pendingCompletion(completedState),
        )
    }

    private fun sanitise(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordSaveState {
        if (state.puzzleId != puzzle.id) return initial(puzzle)
        val solution = solutionCells(puzzle)
        val safeValues =
            state.cellValues
                .mapNotNull { (index, value) ->
                    val letter = value.firstOrNull()?.lowercaseChar()
                    if (index in solution && letter != null && letter.isLetter()) {
                        index to letter.toString()
                    } else {
                        null
                    }
                }.toMap()
        val safeRevealed = state.revealedCellIndices.distinct().filter { it in solution }
        val safeHints = state.revealedHintOrders.distinct().filter { it in hintOrders(puzzle) }
        val activeEntry = puzzle.entries.getOrNull(state.activeEntryIndex)
        val activeRow = state.activeRow.coerceIn(0, puzzle.height - 1)
        val activeColumn = state.activeColumn.coerceIn(0, puzzle.width - 1)
        val activeIndex = cellIndex(puzzle, activeRow, activeColumn)
        val selected =
            if (activeEntry != null && activeIndex in entryCells(puzzle, state.activeEntryIndex)) {
                state.activeEntryIndex
            } else {
                containingEntry(puzzle, activeIndex, state.activeDirection)?.entryIndex ?: 0
            }
        val completed = isComplete(puzzle, state.copy(cellValues = safeValues))
        return state.copy(
            cellValues = safeValues,
            revealedCellIndices = safeRevealed,
            revealedHintOrders = safeHints,
            activeRow = activeRow,
            activeColumn = activeColumn,
            activeEntryIndex = selected.coerceIn(0, puzzle.entries.lastIndex.coerceAtLeast(0)),
            isCompleted = completed,
            completionAcknowledged = if (completed) state.completionAcknowledged else false,
        )
    }

    private fun selectedEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): CrosswordClue? =
        numberedEntries(puzzle).firstOrNull { it.entryIndex == state.activeEntryIndex }
            ?: numberedEntries(puzzle).firstOrNull()

    private fun moveEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        step: Int,
    ): CrosswordSaveState {
        val safeState = sanitise(puzzle, state)
        val entries = numberedEntries(puzzle)
        if (entries.isEmpty()) return safeState
        val current = entries.indexOfFirst { it.entryIndex == safeState.activeEntryIndex }.takeIf { it >= 0 } ?: 0
        val next = (current + step).floorMod(entries.size)
        return selectEntry(puzzle, safeState, entries[next].entryIndex)
    }

    private fun revealOneLetter(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        entryIndex: Int,
    ): CrosswordSaveState {
        val cells = entryCells(puzzle, entryIndex)
        val target =
            cells.firstOrNull { index ->
                val solution = solutionCells(puzzle)[index]?.lowercaseChar()
                val value = state.cellValues[index]?.firstOrNull()?.lowercaseChar()
                solution != null && value != solution
            } ?: cells.firstOrNull()
        val letter = target?.let { solutionCells(puzzle)[it]?.lowercaseChar() }
        return if (target == null || letter == null) {
            state
        } else {
            val revealedState =
                state.copy(
                    cellValues = state.cellValues + (target to letter.toString()),
                    revealedCellIndices = (state.revealedCellIndices + target).distinct(),
                )
            revealedState.moveToCell(puzzle, target)
        }
    }

    private fun revealEntry(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        entryIndex: Int,
    ): CrosswordSaveState {
        val solution = solutionCells(puzzle)
        val cells = entryCells(puzzle, entryIndex)
        val values =
            cells
                .mapNotNull { index ->
                    solution[index]?.let { index to it.lowercaseChar().toString() }
                }.toMap()
        return state.copy(
            cellValues = state.cellValues + values,
            revealedCellIndices = (state.revealedCellIndices + cells).distinct(),
        )
    }

    private fun CrosswordSaveState.moveToNextCell(
        puzzle: CrosswordPuzzle,
        activeIndex: Int,
    ): CrosswordSaveState {
        val entry = selectedEntry(puzzle, this) ?: return this
        val cells = entryCells(puzzle, entry.entryIndex)
        val position = cells.indexOf(activeIndex)
        val nextIndex = cells.drop(position + 1).firstOrNull() ?: activeIndex
        return moveToCell(puzzle, nextIndex)
    }

    private fun CrosswordSaveState.moveToCell(
        puzzle: CrosswordPuzzle,
        index: Int,
    ): CrosswordSaveState =
        copy(
            activeRow = index / puzzle.width,
            activeColumn = index % puzzle.width,
        )

    private fun containingEntry(
        puzzle: CrosswordPuzzle,
        index: Int,
        direction: CrosswordDirection,
    ): CrosswordClue? =
        numberedEntries(puzzle).firstOrNull { clue ->
            clue.direction == direction && index in entryCells(puzzle, clue.entryIndex)
        }

    private fun entrySolved(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
        entryIndex: Int,
    ): Boolean =
        entryCells(puzzle, entryIndex).all { index ->
            val solution = solutionCells(puzzle)[index]?.lowercaseChar()
            val value = state.cellValues[index]?.firstOrNull()?.lowercaseChar()
            solution != null && solution == value
        }

    private fun solvedEntryCount(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): Int = puzzle.entries.indices.count { entrySolved(puzzle, state, it) }

    private fun isComplete(
        puzzle: CrosswordPuzzle,
        state: CrosswordSaveState,
    ): Boolean =
        solutionCells(puzzle).all { (index, solution) ->
            state.cellValues[index]?.firstOrNull()?.lowercaseChar() == solution.lowercaseChar()
        }

    fun solutionCells(puzzle: CrosswordPuzzle): Map<Int, Char> =
        buildMap {
            puzzle.entries.forEach { entry ->
                entry.answer.lowercase().forEachIndexed { offset, letter ->
                    val row = entry.row + if (entry.direction == CrosswordDirection.DOWN) offset else 0
                    val column = entry.column + if (entry.direction == CrosswordDirection.ACROSS) offset else 0
                    put(cellIndex(puzzle, row, column), letter)
                }
            }
        }

    fun entryCells(
        puzzle: CrosswordPuzzle,
        entryIndex: Int,
    ): List<Int> {
        val entry = puzzle.entries.getOrNull(entryIndex) ?: return emptyList()
        return entry.answer.indices.map { offset ->
            val row = entry.row + if (entry.direction == CrosswordDirection.DOWN) offset else 0
            val column = entry.column + if (entry.direction == CrosswordDirection.ACROSS) offset else 0
            cellIndex(puzzle, row, column)
        }
    }

    private fun cellNumbers(puzzle: CrosswordPuzzle): Map<Int, Int> {
        val starts = puzzle.entries.map { cellIndex(puzzle, it.row, it.column) }.toSet()
        val numbers = mutableMapOf<Int, Int>()
        var number = 1
        for (row in 0 until puzzle.height) {
            for (column in 0 until puzzle.width) {
                val index = cellIndex(puzzle, row, column)
                if (index in starts) numbers[index] = number++
            }
        }
        return numbers
    }

    private fun cellIndex(
        puzzle: CrosswordPuzzle,
        row: Int,
        column: Int,
    ): Int = row * puzzle.width + column

    private fun opposite(direction: CrosswordDirection): CrosswordDirection =
        when (direction) {
            CrosswordDirection.ACROSS -> CrosswordDirection.DOWN
            CrosswordDirection.DOWN -> CrosswordDirection.ACROSS
        }

    private fun hintOrders(puzzle: CrosswordPuzzle): List<Int> =
        puzzle.hints
            .map { it.order }
            .sorted()
            .take(MAX_HINTS)

    private fun Int.floorMod(size: Int): Int = ((this % size) + size) % size

    private const val MAX_HINTS = 4

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

@Serializable
data class CrosswordSaveState(
    val puzzleId: String,
    val cellValues: Map<Int, String> = emptyMap(),
    val activeRow: Int = 0,
    val activeColumn: Int = 0,
    val activeDirection: CrosswordDirection = CrosswordDirection.ACROSS,
    val activeEntryIndex: Int = 0,
    val revealedHintOrders: List<Int> = emptyList(),
    val revealedCellIndices: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val completionAcknowledged: Boolean = false,
)

data class CrosswordMoveResult(
    val state: CrosswordSaveState,
    val message: CrosswordMessage? = null,
    val completionEvent: CrosswordCompletionEvent? = null,
)

data class CrosswordCompletionEvent(
    val puzzleId: String,
    val hintsUsed: Int,
)

data class CrosswordBoardCell(
    val row: Int,
    val column: Int,
    val index: Int,
    val number: Int?,
    val solutionLetter: Char?,
    val playerLetter: Char?,
    val isPlayable: Boolean,
    val isActive: Boolean,
    val isInActiveEntry: Boolean,
    val isRevealed: Boolean,
    val isCorrect: Boolean,
)

data class CrosswordClue(
    val entryIndex: Int,
    val number: Int,
    val direction: CrosswordDirection,
    val clue: String,
    val answerLength: Int,
    val row: Int,
    val column: Int,
)

data class CrosswordClueState(
    val clue: CrosswordClue,
    val isSelected: Boolean,
    val isSolved: Boolean,
)

enum class CrosswordMessage(
    val userText: String,
) {
    LETTERS_ONLY("Use letters A to Z."),
    REVEALED_CELL_LOCKED("That hint letter is locked in."),
    ANSWER_COMPLETE("Answer complete."),
    PUZZLE_COMPLETE("Crossword complete."),
    CLUE_REPHRASED("Clue nudge unlocked."),
    LETTER_REVEALED("Helpful letter filled in."),
    CHECK_CORRECT("This answer is correct."),
    CHECK_TRY_AGAIN("Some letters need another look."),
    ANSWER_REVEALED("Answer filled in."),
    HINT_REVEALED("Hint unlocked."),
    NO_HINTS_LEFT("All hints are already open."),
    ALREADY_FINISHED("This crossword is finished."),
}
