package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.puzzle.engine.ShareSafety
import com.dailyquestkids.puzzle.engine.SudokuBoardCell
import com.dailyquestkids.puzzle.engine.SudokuCompletionEvent
import com.dailyquestkids.puzzle.engine.SudokuGameEngine
import com.dailyquestkids.puzzle.engine.SudokuMessage
import com.dailyquestkids.puzzle.engine.SudokuMoveResult
import com.dailyquestkids.puzzle.engine.SudokuSaveState
import kotlinx.coroutines.launch

@Composable
internal fun SudokuRoute(
    data: SudokuRouteData,
    dependencies: SudokuRouteDependencies,
    onBack: () -> Unit,
    onReturnHome: () -> Unit,
    shareActions: ShareActions,
) {
    val savedState by dependencies.sudokuProgressStore
        .stateFor(data.puzzle.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val initialState = remember(data.puzzle.id) { SudokuGameEngine.initial(data.puzzle) }
    val sessionState = remember(data.puzzle.id) { mutableStateOf<SudokuSaveState?>(null) }
    val gameState = sessionState.value ?: savedState ?: initialState
    val scope = rememberCoroutineScope()
    var transientMessage by remember(data.puzzle.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(savedState, data.puzzle.id) {
        savedState?.let { state ->
            sessionState.value = SudokuRouteStateReducer.mergeSavedState(sessionState.value, state)
        }
    }

    fun currentGameState(): SudokuSaveState = sessionState.value ?: savedState ?: initialState

    fun saveState(nextState: SudokuSaveState) {
        sessionState.value = nextState
        scope.launch { dependencies.sudokuProgressStore.save(nextState) }
    }

    fun persist(result: SudokuMoveResult) {
        sessionState.value = result.state
        transientMessage = result.message?.userText
        scope.launch {
            dependencies.sudokuProgressStore.save(result.state)
            handleSudokuCompletion(
                event = result.completionEvent,
                resultState = result.state,
                dependencies = dependencies,
            )
        }
    }

    SudokuGameScreen(
        state =
            SudokuUiMapper.map(
                puzzle = data.puzzle,
                gameState = gameState,
                settings = data.settings,
                homeState = data.homeState,
                transientMessage = transientMessage,
            ),
        actions =
            SudokuGameActions(
                onBack = onBack,
                onCell = { row, column ->
                    saveState(SudokuGameEngine.selectCell(data.puzzle, currentGameState(), row, column))
                    transientMessage = null
                },
                onNumber = { number ->
                    persist(
                        SudokuGameEngine.inputNumber(
                            puzzle = data.puzzle,
                            state = currentGameState(),
                            number = number,
                            mistakeChecking = data.settings.mistakeChecking,
                        ),
                    )
                },
                onTogglePencil = {
                    saveState(SudokuGameEngine.togglePencil(currentGameState()))
                    transientMessage = null
                },
                onErase = {
                    saveState(SudokuGameEngine.erase(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onUndo = {
                    saveState(SudokuGameEngine.undo(currentGameState()))
                    transientMessage = null
                },
                onRedo = {
                    saveState(SudokuGameEngine.redo(currentGameState()))
                    transientMessage = null
                },
                onUseHint = {
                    persist(SudokuGameEngine.revealHint(data.puzzle, currentGameState()))
                },
                onReturnHome = onReturnHome,
                shareActions = shareActions,
            ),
    )
}

private suspend fun handleSudokuCompletion(
    event: SudokuCompletionEvent?,
    resultState: SudokuSaveState,
    dependencies: SudokuRouteDependencies,
) {
    if (event == null) return

    dependencies.progressStore.markCompleted(
        puzzleId = event.puzzleId,
        dayIndex = dependencies.dayIndex,
        todaysPuzzleIds = dependencies.todaysPuzzleIds,
        hintsUsed = event.hintsUsed,
    )
    dependencies.sudokuProgressStore.save(SudokuGameEngine.acknowledgeCompletion(resultState))
}

internal object SudokuRouteStateReducer {
    fun mergeSavedState(
        sessionState: SudokuSaveState?,
        savedState: SudokuSaveState,
    ): SudokuSaveState =
        when {
            sessionState == null -> savedState
            sessionState.puzzleId != savedState.puzzleId -> savedState
            savedState.progressRank() > sessionState.progressRank() -> savedState
            else -> sessionState
        }

    private fun SudokuSaveState.progressRank(): Int =
        cellValues.size * 1_000 +
            notes.values.sumOf { it.size } * 10 +
            revealedHintOrders.size * 100 +
            if (isCompleted) 100_000 else 0
}

@Composable
internal fun SudokuGameScreen(
    state: SudokuUiState,
    actions: SudokuGameActions,
) {
    QuestSceneFrame(testTag = "sudokuScreen") { screenMetrics ->
        val metrics =
            SudokuLayoutCalculator.calculate(
                widthDp = screenMetrics.widthDp,
                heightDp = screenMetrics.heightDp,
            )
        if (metrics.tablet) {
            SudokuTabletLayout(state = state, actions = actions, metrics = metrics)
        } else {
            SudokuPhoneLayout(state = state, actions = actions, metrics = metrics)
        }
    }
}

@Composable
private fun SudokuPhoneLayout(
    state: SudokuUiState,
    actions: SudokuGameActions,
    metrics: SudokuLayoutMetrics,
) {
    Column(
        modifier =
            Modifier
                .width(metrics.contentWidth.dp)
                .fillMaxHeight()
                .padding(vertical = metrics.verticalPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
    ) {
        SudokuTopBar(state = state, metrics = metrics, onBack = actions.onBack)
        SudokuStatusStrip(state = state, metrics = metrics)
        SudokuBoard(state = state, metrics = metrics, actions = actions)
        SudokuToolRow(state = state, metrics = metrics, actions = actions)
        SudokuNumberPad(state = state, metrics = metrics, actions = actions)
        SudokuHintPanel(state = state, metrics = metrics, actions = actions)
    }
}

@Composable
private fun SudokuTabletLayout(
    state: SudokuUiState,
    actions: SudokuGameActions,
    metrics: SudokuLayoutMetrics,
) {
    Column(
        modifier =
            Modifier
                .width(metrics.contentWidth.dp)
                .fillMaxHeight()
                .padding(vertical = metrics.verticalPadding.dp),
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
    ) {
        SudokuTopBar(state = state, metrics = metrics, onBack = actions.onBack)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy((16f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SudokuBoard(state = state, metrics = metrics, actions = actions)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
            ) {
                SudokuStatusStrip(state = state, metrics = metrics)
                SudokuToolRow(state = state, metrics = metrics, actions = actions)
                SudokuNumberPad(state = state, metrics = metrics, actions = actions)
                SudokuHintPanel(state = state, metrics = metrics, actions = actions)
            }
        }
    }
}

internal object SudokuLayoutCalculator {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
    ): SudokuLayoutMetrics {
        val tablet = widthDp >= 600f
        val horizontalPadding = if (widthDp < 360f) 10f else 14f
        val contentWidth = (widthDp - horizontalPadding * 2f).coerceAtMost(if (tablet) 940f else 720f)
        val verticalPadding = (heightDp * 0.01f).coerceIn(5f, 10f)
        val sectionGap = (heightDp * 0.006f).coerceIn(3.5f, 7f)
        val topBarHeight = (heightDp * 0.072f).coerceIn(48f, 66f)
        val statusHeight = (heightDp * 0.052f).coerceIn(34f, 48f)
        val toolHeight = (heightDp * 0.052f).coerceIn(34f, 48f)
        val numberHeight = (heightDp * 0.058f).coerceIn(36f, 50f)
        val hintPanelHeight = (heightDp * 0.15f).coerceIn(84f, 126f)
        val boardBudget =
            if (tablet) {
                heightDp - verticalPadding * 2f - topBarHeight - sectionGap
            } else {
                heightDp -
                    verticalPadding * 2f -
                    topBarHeight -
                    statusHeight -
                    toolHeight -
                    numberHeight -
                    hintPanelHeight -
                    sectionGap * 5f
            }
        val boardWidthLimit = if (tablet) contentWidth * 0.52f else contentWidth
        val maxBoardSize = if (tablet) 430f else contentWidth
        val rawBoardSize = minOf(boardWidthLimit, boardBudget, maxBoardSize).coerceAtLeast(0f)
        val boardSize = if (tablet) rawBoardSize.coerceAtLeast(232f) else rawBoardSize
        val cellSize = boardSize / 6f
        val textScale = (cellSize / 52f).coerceIn(0.62f, 1.08f)
        return SudokuLayoutMetrics(
            contentWidth = contentWidth,
            verticalPadding = verticalPadding,
            sectionGap = sectionGap,
            topBarHeight = topBarHeight,
            statusHeight = statusHeight,
            toolHeight = toolHeight,
            numberHeight = numberHeight,
            hintPanelHeight = hintPanelHeight,
            boardSize = boardSize,
            cellSize = cellSize,
            textScale = textScale,
            tablet = tablet,
        )
    }
}

internal data class SudokuLayoutMetrics(
    val contentWidth: Float,
    val verticalPadding: Float,
    val sectionGap: Float,
    val topBarHeight: Float,
    val statusHeight: Float,
    val toolHeight: Float,
    val numberHeight: Float,
    val hintPanelHeight: Float,
    val boardSize: Float,
    val cellSize: Float,
    val textScale: Float,
    val tablet: Boolean,
) {
    val totalPhoneHeight: Float =
        verticalPadding * 2f +
            topBarHeight +
            statusHeight +
            boardSize +
            toolHeight +
            numberHeight +
            hintPanelHeight +
            sectionGap * 5f
}

@Composable
private fun SudokuTopBar(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(metrics.topBarHeight.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((7f * metrics.textScale).dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size((metrics.topBarHeight - 4f).coerceAtLeast(42f).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.94f)),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text("<", fontWeight = FontWeight.Black, fontSize = (24f * metrics.textScale).sp)
        }
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape((18f * metrics.textScale).dp),
            color = Color(0xFFF2EAFF).copy(alpha = 0.97f),
            border = BorderStroke(2.dp, Color(0xFF8757E6)),
        ) {
            Row(
                modifier = Modifier.padding((7f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                QuestIconTile(
                    category = PuzzleCategory.SUDOKU,
                    modifier = Modifier.size((metrics.topBarHeight - 18f).coerceAtLeast(34f).dp),
                    scale = metrics.textScale,
                )
                QuestAutoText(
                    text = "Sudoku",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 25f * metrics.textScale,
                            minFontSizeSp = 12f,
                            maxLines = 1,
                            color = Color(0xFF32165F),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                SudokuTinyBadge("Cells", state.progressBadge, metrics, Modifier.testTag("sudokuProgressBadge"))
                SudokuTinyBadge("Hints", state.hintBadge, metrics, Modifier.testTag("sudokuHintsBadge"))
            }
        }
    }
}

@Composable
private fun SudokuTinyBadge(
    title: String,
    value: String,
    metrics: SudokuLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width((58f * metrics.textScale).coerceIn(46f, 66f).dp)
                .fillMaxHeight()
                .semantics(mergeDescendants = true) {},
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFC6A7FF)),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuestAutoText(
                text = title,
                modifier = Modifier.fillMaxWidth().weight(0.45f),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 8f * metrics.textScale,
                        minFontSizeSp = 4f,
                        maxLines = 1,
                        color = Color(0xFF32165F),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
            QuestAutoText(
                text = value,
                modifier = Modifier.fillMaxWidth().weight(0.55f),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 12f * metrics.textScale,
                        minFontSizeSp = 5f,
                        maxLines = 1,
                        color = Color(0xFF1D2735),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
        }
    }
}

@Composable
private fun SudokuStatusStrip(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.statusHeight.dp)
                .testTag("sudokuStatusStrip")
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy((5f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SudokuPill(state.streakLabel, metrics, Modifier.weight(0.9f))
        SudokuPill(state.statusText, metrics, Modifier.weight(1.8f))
        SudokuPill(state.selectedLabel, metrics, Modifier.weight(0.9f))
    }
}

@Composable
private fun SudokuPill(
    text: String,
    metrics: SudokuLayoutMetrics,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0xFFC6A7FF)),
        shape = RoundedCornerShape((18f * metrics.textScale).dp),
    ) {
        QuestAutoText(
            text = text,
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 11f * metrics.textScale,
                    minFontSizeSp = 5f,
                    maxLines = 1,
                    color = Color(0xFF1D2735),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
private fun SudokuBoard(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
    actions: SudokuGameActions,
) {
    Column(
        modifier =
            Modifier
                .size(metrics.boardSize.dp)
                .background(Color(0xFF32165F), RoundedCornerShape((10f * metrics.textScale).dp))
                .padding((2f * metrics.textScale).dp)
                .testTag("sudokuBoard"),
    ) {
        state.cells.chunked(6).forEach { rowCells ->
            Row(modifier = Modifier.weight(1f)) {
                rowCells.forEach { cell ->
                    SudokuCell(
                        cell = cell,
                        metrics = metrics,
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(
                                    start = if (cell.column % 3 == 0) 1.5.dp else 0.5.dp,
                                    top = if (cell.row % 2 == 0) 1.5.dp else 0.5.dp,
                                    end = if (cell.column == 5) 1.5.dp else 0.5.dp,
                                    bottom = if (cell.row == 5) 1.5.dp else 0.5.dp,
                                ).testTag("sudokuCell-${cell.row}-${cell.column}")
                                .clickable { actions.onCell(cell.row, cell.column) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SudokuCell(
    cell: SudokuCellUiState,
    metrics: SudokuLayoutMetrics,
    modifier: Modifier,
) {
    val colors = sudokuCellColors(cell)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        color = colors.background,
        border = BorderStroke(if (cell.isSelected) 2.dp else 1.dp, colors.border),
        shape = RoundedCornerShape((6f * metrics.textScale).dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val value = cell.givenValue ?: cell.playerValue
            if (value != null) {
                Text(
                    text = value.toString(),
                    color = colors.content,
                    fontSize = ((if (cell.isGiven) 24f else 22f) * metrics.textScale).sp,
                    fontWeight = if (cell.isGiven) FontWeight.Black else FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else if (cell.notes.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    cell.notes.chunked(3).forEach { noteRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            noteRow.forEach { note ->
                                Text(
                                    text = note.toString(),
                                    color = Color(0xFF4D3A7A),
                                    fontSize = (8f * metrics.textScale).sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SudokuToolRow(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
    actions: SudokuGameActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(metrics.toolHeight.dp),
        horizontalArrangement = Arrangement.spacedBy((5f * metrics.textScale).dp),
    ) {
        SudokuActionButton(
            label = if (state.pencilMode) "Notes On" else "Notes",
            metrics = metrics,
            state =
                SudokuActionButtonState(
                    enabled = !state.isCompleted,
                    selected = state.pencilMode,
                ),
            onClick = actions.onTogglePencil,
            modifier = Modifier.weight(1.15f).testTag("sudokuPencilToggle"),
        )
        SudokuActionButton(
            label = "Erase",
            metrics = metrics,
            state = SudokuActionButtonState(enabled = !state.isCompleted),
            onClick = actions.onErase,
            modifier = Modifier.weight(1f).testTag("sudokuErase"),
        )
        SudokuActionButton(
            label = "Undo",
            metrics = metrics,
            state = SudokuActionButtonState(enabled = state.canUndo),
            onClick = actions.onUndo,
            modifier = Modifier.weight(1f).testTag("sudokuUndo"),
        )
        SudokuActionButton(
            label = "Redo",
            metrics = metrics,
            state = SudokuActionButtonState(enabled = state.canRedo),
            onClick = actions.onRedo,
            modifier = Modifier.weight(1f).testTag("sudokuRedo"),
        )
    }
}

@Composable
private fun SudokuNumberPad(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
    actions: SudokuGameActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(metrics.numberHeight.dp).testTag("sudokuNumberPad"),
        horizontalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp),
    ) {
        for (number in 1..6) {
            SudokuActionButton(
                label = number.toString(),
                metrics = metrics,
                state =
                    SudokuActionButtonState(
                        enabled = !state.isCompleted,
                        selected = number == state.selectedValue,
                    ),
                onClick = { actions.onNumber(number) },
                modifier = Modifier.weight(1f).testTag("sudokuNumber-$number"),
            )
        }
    }
}

@Composable
private fun SudokuHintPanel(
    state: SudokuUiState,
    metrics: SudokuLayoutMetrics,
    actions: SudokuGameActions,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.hintPanelHeight.dp)
                .testTag(if (state.isCompleted) "sudokuCompletionPanel" else "sudokuHintPanel")
                .semantics(mergeDescendants = true) {},
        color = Color(0xFFF8F3FF).copy(alpha = 0.98f),
        border = BorderStroke(2.dp, Color(0xFF8757E6)),
        shape = RoundedCornerShape((22f * metrics.textScale).dp),
        shadowElevation = 4.dp,
    ) {
        if (state.isCompleted) {
            Row(
                modifier = Modifier.padding((11f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp),
                ) {
                    Text(
                        text = "Sudoku complete",
                        color = Color(0xFF32165F),
                        fontWeight = FontWeight.Black,
                        fontSize = (16f * metrics.textScale).sp,
                    )
                    Text(
                        text = state.sharePattern.orEmpty(),
                        color = Color(0xFF1D2735),
                        fontSize = (10f * metrics.textScale).sp,
                        maxLines = 3,
                    )
                }
                PuzzleResultShareDoneRail(
                    shareCard = state.shareCard,
                    shareActions = actions.shareActions,
                    onDone = actions.onReturnHome,
                    tagPrefix = "sudoku",
                    modifier = Modifier.width((88f * metrics.textScale).coerceAtLeast(70f).dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.padding((10f * metrics.textScale).dp),
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp)) {
                    Text("Hint", color = Color(0xFF32165F), fontWeight = FontWeight.Black, fontSize = (16f * metrics.textScale).sp)
                    QuestAutoText(
                        text = state.hintText,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 13f * metrics.textScale,
                                minFontSizeSp = 6f,
                                maxLines = 3,
                                color = Color(0xFF1D2735),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                contentAlignment = Alignment.TopStart,
                            ),
                    )
                }
                SudokuActionButton(
                    label = "Hint ${state.hintsRemaining}",
                    metrics = metrics,
                    state = SudokuActionButtonState(enabled = state.canUseHint),
                    onClick = actions.onUseHint,
                    modifier = Modifier.width((92f * metrics.textScale).coerceAtLeast(74f).dp).fillMaxHeight().testTag("sudokuHintButton"),
                )
            }
        }
    }
}

@Composable
private fun SudokuActionButton(
    label: String,
    metrics: SudokuLayoutMetrics,
    state: SudokuActionButtonState,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = state.enabled,
        modifier = modifier,
        shape = RoundedCornerShape((12f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (state.selected) Color(0xFF6F3BD9) else Color(0xFF8757E6),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE5D8FF),
                disabledContentColor = Color(0xFF6E5A9D),
            ),
    ) {
        QuestAutoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 13f * metrics.textScale,
                    minFontSizeSp = 5f,
                    maxLines = 1,
                    color = if (state.enabled) Color.White else Color(0xFF6E5A9D),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

private data class SudokuActionButtonState(
    val enabled: Boolean,
    val selected: Boolean = false,
)

internal object SudokuUiMapper {
    fun map(
        puzzle: SudokuPuzzle,
        gameState: SudokuSaveState,
        settings: QuestSettings,
        homeState: DailyHomeUiState,
        transientMessage: String?,
    ): SudokuUiState {
        val cells =
            SudokuGameEngine
                .boardCells(puzzle, gameState)
                .map { it.toUiState(settings.mistakeChecking) }
        val filled = (puzzle.givens.count { it > 0 } + gameState.cellValues.size).coerceAtMost(36)
        val selected = cells.firstOrNull { it.isSelected } ?: cells.first()
        val selectedValue = selected.givenValue ?: selected.playerValue
        val shareCard =
            if (gameState.isCompleted) {
                SudokuGameEngine.shareCard(
                    puzzle = puzzle,
                    state = gameState,
                    utcDate = homeState.date.toString(),
                    currentStreak = homeState.currentDailyFiveStreak,
                    bestStreak = homeState.bestDailyFiveStreak,
                )
            } else {
                null
            }
        val safeShareCard = shareCard?.takeUnless { ShareSafety.leaksForbiddenPayload(it) }
        val safeSharePattern = safeShareCard?.visibleResultPattern
        val hintText =
            SudokuGameEngine
                .revealedHintTexts(puzzle, gameState)
                .lastOrNull()
                ?: "Use each number 1 to 6 once in every row, column and box."
        return SudokuUiState(
            cells = cells,
            progressBadge = "$filled/36",
            hintBadge = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0).toString(),
            streakLabel = "Streak ${sudokuStreak(homeState)}",
            statusText = transientMessage ?: if (gameState.isCompleted) SudokuMessage.PUZZLE_COMPLETE.userText else "Fill the 6x6 grid.",
            selectedLabel = "R${selected.row + 1} C${selected.column + 1}",
            selectedValue = selectedValue,
            pencilMode = gameState.pencilMode,
            hintText = hintText,
            hintsRemaining = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0),
            canUseHint = !gameState.isCompleted && gameState.revealedHintOrders.size < puzzle.hints.size,
            canUndo = gameState.history.isNotEmpty() && !gameState.isCompleted,
            canRedo = gameState.redoStack.isNotEmpty() && !gameState.isCompleted,
            isCompleted = gameState.isCompleted,
            sharePattern = safeSharePattern,
            shareCard = safeShareCard,
        )
    }

    private fun sudokuStreak(homeState: DailyHomeUiState): Int =
        homeState.cards
            .firstOrNull { it.category == PuzzleCategory.SUDOKU }
            ?.categoryStreak ?: 0

    private fun SudokuBoardCell.toUiState(showMistakes: Boolean): SudokuCellUiState =
        SudokuCellUiState(
            row = row,
            column = column,
            givenValue = givenValue,
            playerValue = playerValue,
            notes = notes,
            isGiven = givenValue != null,
            isSelected = isSelected,
            isPeer = isPeer,
            isSameValue = isSameValue,
            isConflict = isConflict,
            isMistake = showMistakes && isMistake,
            isRevealed = isRevealed,
        )
}

internal data class SudokuRouteData(
    val puzzle: SudokuPuzzle,
    val settings: QuestSettings,
    val homeState: DailyHomeUiState,
)

internal data class SudokuRouteDependencies(
    val progressStore: ProgressStore,
    val sudokuProgressStore: SudokuProgressStore,
    val dayIndex: Int,
    val todaysPuzzleIds: Set<String>,
)

internal data class SudokuGameActions(
    val onBack: () -> Unit,
    val onCell: (Int, Int) -> Unit,
    val onNumber: (Int) -> Unit,
    val onTogglePencil: () -> Unit,
    val onErase: () -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onUseHint: () -> Unit,
    val onReturnHome: () -> Unit,
    val shareActions: ShareActions,
)

internal data class SudokuUiState(
    val cells: List<SudokuCellUiState>,
    val progressBadge: String,
    val hintBadge: String,
    val streakLabel: String,
    val statusText: String,
    val selectedLabel: String,
    val selectedValue: Int?,
    val pencilMode: Boolean,
    val hintText: String,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val canUndo: Boolean,
    val canRedo: Boolean,
    val isCompleted: Boolean,
    val sharePattern: String?,
    val shareCard: ShareCardModel?,
)

internal data class SudokuCellUiState(
    val row: Int,
    val column: Int,
    val givenValue: Int?,
    val playerValue: Int?,
    val notes: List<Int>,
    val isGiven: Boolean,
    val isSelected: Boolean,
    val isPeer: Boolean,
    val isSameValue: Boolean,
    val isConflict: Boolean,
    val isMistake: Boolean,
    val isRevealed: Boolean,
)

private data class SudokuCellColors(
    val background: Color,
    val border: Color,
    val content: Color,
)

private fun sudokuCellColors(cell: SudokuCellUiState): SudokuCellColors =
    when {
        cell.isConflict -> SudokuCellColors(Color(0xFFFFE3E3), Color(0xFFE53E3E), Color(0xFF7A1010))
        cell.isMistake -> SudokuCellColors(Color(0xFFFFF1D6), Color(0xFFE79021), Color(0xFF6B3500))
        cell.isSelected -> SudokuCellColors(Color(0xFFFFE17A), Color(0xFFD6A900), Color(0xFF1D2735))
        cell.isRevealed -> SudokuCellColors(Color(0xFFE0F4FF), Color(0xFF1E9BE8), Color(0xFF32165F))
        cell.isSameValue -> SudokuCellColors(Color(0xFFE5D8FF), Color(0xFF8757E6), Color(0xFF32165F))
        cell.isPeer -> SudokuCellColors(Color(0xFFF5EDFF), Color(0xFFC6A7FF), Color(0xFF1D2735))
        cell.isGiven -> SudokuCellColors(Color.White.copy(alpha = 0.98f), Color(0xFF9B78E8), Color(0xFF32165F))
        else -> SudokuCellColors(Color.White.copy(alpha = 0.94f), Color(0xFFC6A7FF), Color(0xFF1D2735))
    }
