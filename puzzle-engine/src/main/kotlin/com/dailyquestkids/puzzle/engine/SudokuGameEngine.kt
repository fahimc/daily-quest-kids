package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import com.dailyquestkids.core.model.SudokuPuzzle
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SudokuGameEngine {
    fun initial(puzzle: SudokuPuzzle): SudokuSaveState {
        val selected = puzzle.givens.indexOfFirst { it == EMPTY }.takeIf { it >= 0 } ?: 0
        return SudokuSaveState(
            puzzleId = puzzle.id,
            selectedIndex = selected,
        )
    }

    fun boardCells(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): List<SudokuBoardCell> {
        val safeState = sanitise(puzzle, state)
        val selected = safeState.selectedIndex
        val selectedValue = valueAt(puzzle, safeState, selected)
        val peers = peersFor(selected)
        val conflicts = conflictIndices(puzzle, safeState)

        return (0 until CELL_COUNT).map { index ->
            val given = puzzle.givens.getOrElse(index) { EMPTY }
            val playerValue = safeState.cellValues[index]
            val value = if (given > EMPTY) given else playerValue
            SudokuBoardCell(
                row = index / SIZE,
                column = index % SIZE,
                index = index,
                givenValue = given.takeIf { it > EMPTY },
                playerValue = playerValue,
                solutionValue = puzzle.solution.getOrElse(index) { EMPTY },
                notes = safeState.notes[index].orEmpty().sorted(),
                isSelected = index == selected,
                isPeer = index in peers,
                isSameValue = value != null && value == selectedValue && index != selected,
                isConflict = index in conflicts,
                isMistake = playerValue != null && playerValue != puzzle.solution.getOrElse(index) { EMPTY },
                isRevealed = index in safeState.revealedCellIndices,
            )
        }
    }

    fun selectCell(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        row: Int,
        column: Int,
    ): SudokuSaveState {
        val safeState = sanitise(puzzle, state)
        val index = row * SIZE + column
        return if (index in 0 until CELL_COUNT) safeState.copy(selectedIndex = index) else safeState
    }

    fun togglePencil(state: SudokuSaveState): SudokuSaveState = state.copy(pencilMode = !state.pencilMode)

    fun setPencilMode(
        state: SudokuSaveState,
        enabled: Boolean,
    ): SudokuSaveState = state.copy(pencilMode = enabled)

    fun inputNumber(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        number: Int,
        mistakeChecking: Boolean,
    ): SudokuMoveResult {
        val safeState = sanitise(puzzle, state)
        val index = safeState.selectedIndex
        return when {
            safeState.isCompleted -> SudokuMoveResult(safeState, SudokuMessage.ALREADY_FINISHED)
            number !in DIGITS -> SudokuMoveResult(safeState, SudokuMessage.NUMBERS_ONLY)
            !isEditable(puzzle, safeState, index) -> SudokuMoveResult(safeState, SudokuMessage.GIVEN_LOCKED)
            safeState.pencilMode -> toggleNote(safeState, index, number)
            else -> placeNumber(puzzle, safeState, index, number, mistakeChecking)
        }
    }

    fun erase(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): SudokuSaveState {
        val safeState = sanitise(puzzle, state)
        val index = safeState.selectedIndex
        return if (safeState.isCompleted || !isEditable(puzzle, safeState, index)) {
            safeState
        } else {
            val nextState =
                safeState.copy(
                    cellValues = safeState.cellValues - index,
                    notes = safeState.notes - index,
                    redoStack = emptyList(),
                )
            nextState.withHistory(safeState)
        }
    }

    fun undo(state: SudokuSaveState): SudokuSaveState {
        val previous = state.history.lastOrNull() ?: return state
        return state.fromSnapshot(previous).copy(
            history = state.history.dropLast(1),
            redoStack = state.redoStack + state.toSnapshot(),
        )
    }

    fun redo(state: SudokuSaveState): SudokuSaveState {
        val next = state.redoStack.lastOrNull() ?: return state
        return state.fromSnapshot(next).copy(
            history = state.history + state.toSnapshot(),
            redoStack = state.redoStack.dropLast(1),
        )
    }

    fun revealHint(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): SudokuMoveResult {
        val safeState = sanitise(puzzle, state)
        val nextHint =
            hintOrders(puzzle).firstOrNull { it !in safeState.revealedHintOrders }
                ?: return SudokuMoveResult(safeState, SudokuMessage.NO_HINTS_LEFT)
        val target = hintTarget(puzzle, safeState)
        val hintedState =
            when (nextHint) {
                1, 2, 3 -> safeState.copy(selectedIndex = target)
                4 -> revealCell(puzzle, safeState, target)
                else -> safeState
            }.copy(
                revealedHintOrders = safeState.revealedHintOrders + nextHint,
                redoStack = emptyList(),
            )
        val completed = isComplete(puzzle, hintedState)
        val completedState = hintedState.copy(isCompleted = completed, completionAcknowledged = false)
        return SudokuMoveResult(
            state = if (nextHint == 4) completedState.withHistory(safeState) else completedState,
            message =
                when (nextHint) {
                    1 -> SudokuMessage.AREA_HIGHLIGHTED
                    2 -> SudokuMessage.CANDIDATES_EXPLAINED
                    3 -> SudokuMessage.SINGLE_FOUND
                    4 -> SudokuMessage.NUMBER_REVEALED
                    else -> SudokuMessage.HINT_REVEALED
                },
            completionEvent = pendingCompletion(completedState),
        )
    }

    fun revealedHintTexts(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): List<String> {
        val safeState = sanitise(puzzle, state)
        val target = hintTarget(puzzle, safeState)
        val row = target / SIZE + 1
        val column = target % SIZE + 1
        val solution = puzzle.solution.getOrElse(target) { EMPTY }
        val candidates = candidatesFor(puzzle, safeState, target).joinToString()
        return safeState.revealedHintOrders.map { order ->
            when (order) {
                1 -> "Look closely at row $row and column $column."
                2 -> "The other numbers leave these choices: $candidates."
                3 -> "Row $row, column $column can only be $solution."
                4 -> "A helpful $solution was placed at row $row, column $column."
                else ->
                    puzzle.hints
                        .firstOrNull { it.order == order }
                        ?.text
                        .orEmpty()
            }
        }
    }

    fun candidatesFor(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
    ): Set<Int> {
        val safeState = sanitise(puzzle, state)
        if (index !in 0 until CELL_COUNT || valueAt(puzzle, safeState, index) != null) return emptySet()
        val used = peersFor(index).mapNotNull { peer -> valueAt(puzzle, safeState, peer) }.toSet()
        return DIGITS.filterNot { it in used }.toSet()
    }

    fun acknowledgeCompletion(state: SudokuSaveState): SudokuSaveState =
        if (state.isCompleted) state.copy(completionAcknowledged = true) else state

    fun pendingCompletion(state: SudokuSaveState): SudokuCompletionEvent? =
        if (state.isCompleted && !state.completionAcknowledged) {
            SudokuCompletionEvent(
                puzzleId = state.puzzleId,
                hintsUsed = state.revealedHintOrders.size,
            )
        } else {
            null
        }

    fun shareCard(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        utcDate: String,
        currentStreak: Int,
        bestStreak: Int,
    ): ShareCardModel {
        val safeState = sanitise(puzzle, state)
        val filled = (0 until CELL_COUNT).count { valueAt(puzzle, safeState, it) != null }
        val mistakes = boardCells(puzzle, safeState).count { it.isMistake }
        return ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = utcDate,
            cardType = ShareCardType.INDIVIDUAL_RESULT,
            visibleResultPattern =
                listOf(
                    "Sudoku ${sharePuzzleNumber(puzzle.id)}",
                    "Filled $filled/$CELL_COUNT cells",
                    "Mistakes $mistakes",
                    "Hints ${safeState.revealedHintOrders.size}",
                    "Streak $currentStreak",
                    if (safeState.isCompleted) "Complete" else "In progress",
                ).joinToString(separator = "\n"),
            hintsUsed = safeState.revealedHintOrders.size,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            forbiddenPayloads = solutionPayloads(puzzle),
        )
    }

    fun encode(state: SudokuSaveState): String = json.encodeToString(state)

    fun decode(payload: String): SudokuSaveState = json.decodeFromString(payload)

    private fun placeNumber(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
        number: Int,
        mistakeChecking: Boolean,
    ): SudokuMoveResult {
        val nextState =
            state.copy(
                cellValues = state.cellValues + (index to number),
                notes = state.notes - index,
                redoStack = emptyList(),
            )
        val completed = isComplete(puzzle, nextState)
        val completedState =
            nextState
                .copy(isCompleted = completed, completionAcknowledged = false)
                .withHistory(state)
        val message =
            when {
                completed -> SudokuMessage.PUZZLE_COMPLETE
                hasConflictAt(puzzle, completedState, index) -> SudokuMessage.CONFLICT
                mistakeChecking && number != puzzle.solution[index] -> SudokuMessage.MISTAKE
                else -> null
            }
        return SudokuMoveResult(
            state = completedState,
            message = message,
            completionEvent = pendingCompletion(completedState),
        )
    }

    private fun toggleNote(
        state: SudokuSaveState,
        index: Int,
        number: Int,
    ): SudokuMoveResult {
        val current = state.notes[index].orEmpty()
        val toggledNotes =
            if (number in current) {
                current - number
            } else {
                current + number
            }
        val nextNotes = toggledNotes.distinct().sorted()
        val nextState =
            state
                .copy(
                    notes = if (nextNotes.isEmpty()) state.notes - index else state.notes + (index to nextNotes),
                    redoStack = emptyList(),
                ).withHistory(state)
        return SudokuMoveResult(nextState, SudokuMessage.NOTE_UPDATED)
    }

    private fun revealCell(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
    ): SudokuSaveState =
        if (!isEditable(puzzle, state, index)) {
            state
        } else {
            state.copy(
                selectedIndex = index,
                cellValues = state.cellValues + (index to puzzle.solution[index]),
                notes = state.notes - index,
                revealedCellIndices = (state.revealedCellIndices + index).distinct(),
            )
        }

    private fun sanitise(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): SudokuSaveState {
        if (state.puzzleId != puzzle.id) return initial(puzzle)
        val selected = state.selectedIndex.coerceIn(0, CELL_COUNT - 1)
        val cellValues =
            state.cellValues
                .filterKeys { it in 0 until CELL_COUNT && puzzle.givens.getOrElse(it) { EMPTY } == EMPTY }
                .filterValues { it in DIGITS }
        val notes =
            state.notes
                .filterKeys { it in 0 until CELL_COUNT && puzzle.givens.getOrElse(it) { EMPTY } == EMPTY }
                .mapValues { (_, values) -> values.filter { it in DIGITS }.distinct().sorted() }
                .filterValues { it.isNotEmpty() }
        val revealed = state.revealedCellIndices.distinct().filter { it in 0 until CELL_COUNT }
        val hints = state.revealedHintOrders.distinct().filter { it in hintOrders(puzzle) }
        val completed = isComplete(puzzle, state.copy(cellValues = cellValues))
        return state.copy(
            selectedIndex = selected,
            cellValues = cellValues,
            notes = notes,
            revealedCellIndices = revealed,
            revealedHintOrders = hints,
            isCompleted = completed,
            completionAcknowledged = if (completed) state.completionAcknowledged else false,
        )
    }

    private fun SudokuSaveState.withHistory(previous: SudokuSaveState): SudokuSaveState =
        copy(history = (previous.history + previous.toSnapshot()).takeLast(HISTORY_LIMIT))

    private fun SudokuSaveState.toSnapshot(): SudokuSnapshot =
        SudokuSnapshot(
            cellValues = cellValues,
            notes = notes,
            selectedIndex = selectedIndex,
            pencilMode = pencilMode,
            revealedHintOrders = revealedHintOrders,
            revealedCellIndices = revealedCellIndices,
            isCompleted = isCompleted,
            completionAcknowledged = completionAcknowledged,
        )

    private fun SudokuSaveState.fromSnapshot(snapshot: SudokuSnapshot): SudokuSaveState =
        copy(
            cellValues = snapshot.cellValues,
            notes = snapshot.notes,
            selectedIndex = snapshot.selectedIndex,
            pencilMode = snapshot.pencilMode,
            revealedHintOrders = snapshot.revealedHintOrders,
            revealedCellIndices = snapshot.revealedCellIndices,
            isCompleted = snapshot.isCompleted,
            completionAcknowledged = snapshot.completionAcknowledged,
        )

    private fun hintTarget(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): Int =
        (0 until CELL_COUNT).firstOrNull { index ->
            valueAt(puzzle, state, index) == null && candidatesFor(puzzle, state, index).size == 1
        } ?: (0 until CELL_COUNT).firstOrNull { valueAt(puzzle, state, it) == null } ?: state.selectedIndex

    private fun isEditable(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
    ): Boolean =
        index in 0 until CELL_COUNT &&
            puzzle.givens.getOrElse(index) { EMPTY } == EMPTY &&
            index !in state.revealedCellIndices

    private fun isComplete(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): Boolean =
        puzzle.solution.size == CELL_COUNT &&
            (0 until CELL_COUNT).all { index -> valueAt(puzzle, state, index) == puzzle.solution[index] }

    private fun valueAt(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
    ): Int? {
        val given = puzzle.givens.getOrElse(index) { EMPTY }
        return if (given > EMPTY) given else state.cellValues[index]
    }

    private fun conflictIndices(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
    ): Set<Int> =
        (0 until CELL_COUNT)
            .filter { index -> hasConflictAt(puzzle, state, index) }
            .toSet()

    private fun hasConflictAt(
        puzzle: SudokuPuzzle,
        state: SudokuSaveState,
        index: Int,
    ): Boolean {
        val value = valueAt(puzzle, state, index) ?: return false
        return peersFor(index).any { peer -> valueAt(puzzle, state, peer) == value }
    }

    private fun peersFor(index: Int): Set<Int> {
        val row = index / SIZE
        val column = index % SIZE
        val regionRow = row / REGION_ROWS * REGION_ROWS
        val regionColumn = column / REGION_COLUMNS * REGION_COLUMNS
        val rowPeers = (0 until SIZE).map { row * SIZE + it }
        val columnPeers = (0 until SIZE).map { it * SIZE + column }
        val regionPeers =
            (regionRow until regionRow + REGION_ROWS).flatMap { r ->
                (regionColumn until regionColumn + REGION_COLUMNS).map { c -> r * SIZE + c }
            }
        return (rowPeers + columnPeers + regionPeers).filterNot { it == index }.toSet()
    }

    private fun hintOrders(puzzle: SudokuPuzzle): List<Int> =
        puzzle.hints
            .map { it.order }
            .sorted()
            .take(MAX_HINTS)

    private fun solutionPayloads(puzzle: SudokuPuzzle): List<String> =
        puzzle.solution.chunked(SIZE).map { row -> row.joinToString("") } +
            listOf(puzzle.solution.joinToString(""))

    private const val SIZE = 6
    private const val REGION_ROWS = 2
    private const val REGION_COLUMNS = 3
    private const val CELL_COUNT = SIZE * SIZE
    private const val EMPTY = 0
    private const val MAX_HINTS = 4
    private const val HISTORY_LIMIT = 40

    private val DIGITS = 1..SIZE

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
}

@Serializable
data class SudokuSaveState(
    val puzzleId: String,
    val cellValues: Map<Int, Int> = emptyMap(),
    val notes: Map<Int, List<Int>> = emptyMap(),
    val selectedIndex: Int = 0,
    val pencilMode: Boolean = false,
    val revealedHintOrders: List<Int> = emptyList(),
    val revealedCellIndices: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val completionAcknowledged: Boolean = false,
    val history: List<SudokuSnapshot> = emptyList(),
    val redoStack: List<SudokuSnapshot> = emptyList(),
)

@Serializable
data class SudokuSnapshot(
    val cellValues: Map<Int, Int> = emptyMap(),
    val notes: Map<Int, List<Int>> = emptyMap(),
    val selectedIndex: Int = 0,
    val pencilMode: Boolean = false,
    val revealedHintOrders: List<Int> = emptyList(),
    val revealedCellIndices: List<Int> = emptyList(),
    val isCompleted: Boolean = false,
    val completionAcknowledged: Boolean = false,
)

data class SudokuMoveResult(
    val state: SudokuSaveState,
    val message: SudokuMessage? = null,
    val completionEvent: SudokuCompletionEvent? = null,
)

data class SudokuCompletionEvent(
    val puzzleId: String,
    val hintsUsed: Int,
)

data class SudokuBoardCell(
    val row: Int,
    val column: Int,
    val index: Int,
    val givenValue: Int?,
    val playerValue: Int?,
    val solutionValue: Int,
    val notes: List<Int>,
    val isSelected: Boolean,
    val isPeer: Boolean,
    val isSameValue: Boolean,
    val isConflict: Boolean,
    val isMistake: Boolean,
    val isRevealed: Boolean,
)

enum class SudokuMessage(
    val userText: String,
) {
    NUMBERS_ONLY("Use numbers 1 to 6."),
    GIVEN_LOCKED("That starting number is locked in."),
    NOTE_UPDATED("Note updated."),
    CONFLICT("That number repeats in a row, column or box."),
    MISTAKE("That spot needs another look."),
    AREA_HIGHLIGHTED("Helpful area highlighted."),
    CANDIDATES_EXPLAINED("Choices explained."),
    SINGLE_FOUND("A single-choice cell is highlighted."),
    NUMBER_REVEALED("Helpful number placed."),
    HINT_REVEALED("Hint unlocked."),
    NO_HINTS_LEFT("All hints are already open."),
    PUZZLE_COMPLETE("Sudoku complete."),
    ALREADY_FINISHED("This sudoku is finished."),
}
