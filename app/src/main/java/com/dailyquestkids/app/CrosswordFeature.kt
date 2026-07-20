package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.puzzle.engine.CrosswordBoardCell
import com.dailyquestkids.puzzle.engine.CrosswordClueState
import com.dailyquestkids.puzzle.engine.CrosswordCompletionEvent
import com.dailyquestkids.puzzle.engine.CrosswordGameEngine
import com.dailyquestkids.puzzle.engine.CrosswordMessage
import com.dailyquestkids.puzzle.engine.CrosswordMoveResult
import com.dailyquestkids.puzzle.engine.CrosswordSaveState
import com.dailyquestkids.puzzle.engine.ShareSafety
import kotlinx.coroutines.launch

@Composable
internal fun CrosswordRoute(
    data: CrosswordRouteData,
    dependencies: CrosswordRouteDependencies,
    onBack: () -> Unit,
    onReturnHome: () -> Unit,
) {
    val savedState by dependencies.crosswordProgressStore
        .stateFor(data.puzzle.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val initialState = remember(data.puzzle.id) { CrosswordGameEngine.initial(data.puzzle) }
    val sessionState = remember(data.puzzle.id) { mutableStateOf<CrosswordSaveState?>(null) }
    val gameState = sessionState.value ?: savedState ?: initialState
    val scope = rememberCoroutineScope()
    var transientMessage by remember(data.puzzle.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(savedState, data.puzzle.id) {
        savedState?.let { state ->
            sessionState.value = CrosswordRouteStateReducer.mergeSavedState(sessionState.value, state)
        }
    }

    fun currentGameState(): CrosswordSaveState = sessionState.value ?: savedState ?: initialState

    fun saveState(nextState: CrosswordSaveState) {
        sessionState.value = nextState
        scope.launch { dependencies.crosswordProgressStore.save(nextState) }
    }

    fun persist(result: CrosswordMoveResult) {
        sessionState.value = result.state
        transientMessage = result.message?.userText
        scope.launch {
            dependencies.crosswordProgressStore.save(result.state)
            handleCrosswordCompletion(
                event = result.completionEvent,
                resultState = result.state,
                dependencies = dependencies,
            )
        }
    }

    CrosswordGameScreen(
        state =
            CrosswordUiMapper.map(
                puzzle = data.puzzle,
                gameState = gameState,
                settings = data.settings,
                homeState = data.homeState,
                transientMessage = transientMessage,
            ),
        actions =
            CrosswordGameActions(
                onBack = onBack,
                onCell = { row, column ->
                    saveState(CrosswordGameEngine.selectCell(data.puzzle, currentGameState(), row, column))
                    transientMessage = null
                },
                onEntry = { entryIndex ->
                    saveState(CrosswordGameEngine.selectEntry(data.puzzle, currentGameState(), entryIndex))
                    transientMessage = null
                },
                onToggleDirection = {
                    saveState(CrosswordGameEngine.toggleDirection(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onPrevious = {
                    saveState(CrosswordGameEngine.previousEntry(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onNext = {
                    saveState(CrosswordGameEngine.nextEntry(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onUseHint = {
                    persist(CrosswordGameEngine.revealHint(data.puzzle, currentGameState()))
                },
                onLetter = { letter ->
                    persist(CrosswordGameEngine.appendLetter(data.puzzle, currentGameState(), letter))
                },
                onDelete = {
                    saveState(CrosswordGameEngine.deleteLetter(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onReturnHome = onReturnHome,
            ),
    )
}

private suspend fun handleCrosswordCompletion(
    event: CrosswordCompletionEvent?,
    resultState: CrosswordSaveState,
    dependencies: CrosswordRouteDependencies,
) {
    if (event == null) return

    dependencies.progressStore.markCompleted(
        puzzleId = event.puzzleId,
        dayIndex = dependencies.dayIndex,
        todaysPuzzleIds = dependencies.todaysPuzzleIds,
    )
    dependencies.crosswordProgressStore.save(CrosswordGameEngine.acknowledgeCompletion(resultState))
}

internal object CrosswordRouteStateReducer {
    fun mergeSavedState(
        sessionState: CrosswordSaveState?,
        savedState: CrosswordSaveState,
    ): CrosswordSaveState =
        when {
            sessionState == null -> savedState
            sessionState.puzzleId != savedState.puzzleId -> savedState
            savedState.progressRank() > sessionState.progressRank() -> savedState
            else -> sessionState
        }

    private fun CrosswordSaveState.progressRank(): Int =
        cellValues.size * 1_000 +
            revealedHintOrders.size * 100 +
            if (isCompleted) 100_000 else 0
}

@Composable
internal fun CrosswordGameScreen(
    state: CrosswordUiState,
    actions: CrosswordGameActions,
) {
    QuestSceneFrame(testTag = "crosswordScreen") { screenMetrics ->
        val metrics =
            CrosswordLayoutCalculator.calculate(
                widthDp = screenMetrics.widthDp,
                heightDp = screenMetrics.heightDp,
            )
        var expandedClues by remember(state.cluesAcross, state.cluesDown, state.isCompleted) {
            mutableStateOf(false)
        }

        if (metrics.tablet) {
            CrosswordTabletLayout(
                state = state,
                actions = actions,
                metrics = metrics,
                onExpandClues = { expandedClues = true },
            )
        } else {
            CrosswordPhoneLayout(
                state = state,
                actions = actions,
                metrics = metrics,
                onExpandClues = { expandedClues = true },
            )
        }

        if (expandedClues) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("crosswordClueDismissLayer")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expandedClues = false },
                        ),
            )
            CrosswordExpandedClueList(
                state = state,
                metrics = metrics,
                actions = actions,
                onCollapse = { expandedClues = false },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(metrics.contentWidth.dp)
                        .padding(bottom = metrics.verticalPadding.dp),
            )
        }
    }
}

@Composable
private fun CrosswordPhoneLayout(
    state: CrosswordUiState,
    actions: CrosswordGameActions,
    metrics: CrosswordLayoutMetrics,
    onExpandClues: () -> Unit,
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
        CrosswordTopBar(state = state, metrics = metrics, onBack = actions.onBack)
        CrosswordStatusStrip(state = state, metrics = metrics, onToggleDirection = actions.onToggleDirection)
        CrosswordBoard(state = state, metrics = metrics, actions = actions)
        CrosswordCluePanel(
            state = state,
            metrics = metrics,
            actions = actions,
            onExpandClues = onExpandClues,
        )
        CrosswordKeyboard(state = state, metrics = metrics, actions = actions)
    }
}

@Composable
private fun CrosswordTabletLayout(
    state: CrosswordUiState,
    actions: CrosswordGameActions,
    metrics: CrosswordLayoutMetrics,
    onExpandClues: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(metrics.contentWidth.dp)
                .fillMaxHeight()
                .padding(vertical = metrics.verticalPadding.dp),
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
    ) {
        CrosswordTopBar(state = state, metrics = metrics, onBack = actions.onBack)
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy((16f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrosswordBoard(state = state, metrics = metrics, actions = actions)
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
            ) {
                CrosswordStatusStrip(state = state, metrics = metrics, onToggleDirection = actions.onToggleDirection)
                CrosswordCluePanel(
                    state = state,
                    metrics = metrics,
                    actions = actions,
                    onExpandClues = onExpandClues,
                )
                CrosswordKeyboard(state = state, metrics = metrics, actions = actions)
            }
        }
    }
}

internal object CrosswordLayoutCalculator {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
    ): CrosswordLayoutMetrics {
        val tablet = widthDp >= 600f
        val horizontalPadding = if (widthDp < 360f) 10f else 14f
        val contentWidth = (widthDp - horizontalPadding * 2f).coerceAtMost(if (tablet) 940f else 720f)
        val verticalPadding = (heightDp * 0.01f).coerceIn(5f, 10f)
        val sectionGap = (heightDp * 0.006f).coerceIn(3.5f, 7f)
        val topBarHeight = (heightDp * 0.072f).coerceIn(48f, 66f)
        val statusHeight = (heightDp * 0.052f).coerceIn(36f, 48f)
        val cluePanelHeight = (heightDp * 0.135f).coerceIn(84f, 122f)
        val keyHeight = (heightDp * 0.052f).coerceIn(34f, 48f)
        val keyboardGap = (heightDp * 0.005f).coerceIn(3f, 5f)
        val keyboardHeight = keyHeight * 3f + keyboardGap * 2f
        val boardBudget =
            if (tablet) {
                heightDp - verticalPadding * 2f - topBarHeight - sectionGap
            } else {
                heightDp -
                    verticalPadding * 2f -
                    topBarHeight -
                    statusHeight -
                    cluePanelHeight -
                    keyboardHeight -
                    sectionGap * 4f
            }
        val boardWidthLimit = if (tablet) contentWidth * 0.54f else contentWidth
        val maxBoardSize = if (tablet) 420f else contentWidth
        val rawBoardSize = minOf(boardWidthLimit, boardBudget, maxBoardSize).coerceAtLeast(0f)
        val boardSize = if (tablet) rawBoardSize.coerceAtLeast(196f) else rawBoardSize
        val cellSize = boardSize / 7f
        val textScale = (cellSize / 48f).coerceIn(0.62f, 1.08f)
        val expandedPanelHeight = (heightDp * 0.52f).coerceIn(cluePanelHeight + 120f, heightDp - verticalPadding * 2f)

        return CrosswordLayoutMetrics(
            contentWidth = contentWidth,
            verticalPadding = verticalPadding,
            sectionGap = sectionGap,
            topBarHeight = topBarHeight,
            statusHeight = statusHeight,
            cluePanelHeight = cluePanelHeight,
            boardSize = boardSize,
            cellSize = cellSize,
            keyHeight = keyHeight,
            keyboardGap = keyboardGap,
            keyboardHeight = keyboardHeight,
            expandedPanelHeight = expandedPanelHeight,
            textScale = textScale,
            tablet = tablet,
        )
    }
}

internal data class CrosswordLayoutMetrics(
    val contentWidth: Float,
    val verticalPadding: Float,
    val sectionGap: Float,
    val topBarHeight: Float,
    val statusHeight: Float,
    val cluePanelHeight: Float,
    val boardSize: Float,
    val cellSize: Float,
    val keyHeight: Float,
    val keyboardGap: Float,
    val keyboardHeight: Float,
    val expandedPanelHeight: Float,
    val textScale: Float,
    val tablet: Boolean,
) {
    val totalPhoneHeight: Float =
        verticalPadding * 2f +
            topBarHeight +
            statusHeight +
            boardSize +
            cluePanelHeight +
            keyboardHeight +
            sectionGap * 4f
}

@Composable
private fun CrosswordTopBar(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
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
            color = Color(0xFFEAF6FF).copy(alpha = 0.97f),
            border = BorderStroke(2.dp, Color(0xFF1788D8)),
        ) {
            Row(
                modifier = Modifier.padding((7f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                QuestIconTile(
                    category = PuzzleCategory.CROSSWORD,
                    modifier = Modifier.size((metrics.topBarHeight - 18f).coerceAtLeast(34f).dp),
                    scale = metrics.textScale,
                )
                QuestAutoText(
                    text = "Crossword",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 25f * metrics.textScale,
                            minFontSizeSp = 12f,
                            maxLines = 1,
                            color = Color(0xFF073B6D),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                CrosswordTinyBadge("Clues", state.progressBadge, metrics, Modifier.testTag("crosswordProgressBadge"))
                CrosswordTinyBadge("Hints", state.hintBadge, metrics, Modifier.testTag("crosswordHintsBadge"))
            }
        }
    }
}

@Composable
private fun CrosswordTinyBadge(
    title: String,
    value: String,
    metrics: CrosswordLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width((58f * metrics.textScale).coerceIn(46f, 66f).dp)
                .fillMaxHeight()
                .semantics(mergeDescendants = true) {},
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF8FC5E9)),
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
                        color = Color(0xFF073B6D),
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
                        color = Color(0xFF173444),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
        }
    }
}

@Composable
private fun CrosswordStatusStrip(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    onToggleDirection: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.statusHeight.dp)
                .testTag("crosswordStatusStrip")
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy((5f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrosswordPill(state.streakLabel, metrics, Modifier.weight(0.82f))
        CrosswordPill(state.statusText, metrics, Modifier.weight(1.95f))
        Surface(
            modifier = Modifier.weight(0.82f).fillMaxHeight().clickable(onClick = onToggleDirection),
            color = Color(0xFFFFF7D6),
            border = BorderStroke(1.dp, Color(0xFFD6A900)),
            shape = RoundedCornerShape((18f * metrics.textScale).dp),
        ) {
            QuestAutoText(
                text = state.directionLabel,
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 11f * metrics.textScale,
                        minFontSizeSp = 5f,
                        maxLines = 1,
                        color = Color(0xFF173444),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
        }
    }
}

@Composable
private fun CrosswordPill(
    text: String,
    metrics: CrosswordLayoutMetrics,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0xFF8FC5E9)),
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
                    color = Color(0xFF173444),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
private fun CrosswordBoard(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    actions: CrosswordGameActions,
) {
    Column(
        modifier =
            Modifier
                .size(metrics.boardSize.dp)
                .testTag("crosswordBoard"),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        state.cells.chunked(state.width).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                row.forEach { cell ->
                    CrosswordCell(
                        cell = cell,
                        metrics = metrics,
                        largeText = state.largeText,
                        onClick = { actions.onCell(cell.row, cell.column) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrosswordCell(
    cell: CrosswordBoardCellUiState,
    metrics: CrosswordLayoutMetrics,
    largeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = crosswordCellColors(cell)
    Surface(
        modifier =
            modifier
                .testTag("crosswordCell-${cell.row}-${cell.column}")
                .then(if (cell.isPlayable) Modifier.clickable(onClick = onClick) else Modifier),
        color = colors.background,
        border = BorderStroke(if (cell.isActive) 2.dp else 1.dp, colors.border),
        shape = RoundedCornerShape(3.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            cell.number?.let { number ->
                Text(
                    text = number.toString(),
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp, top = 1.dp),
                    color = colors.content.copy(alpha = 0.85f),
                    fontSize = (7f * metrics.textScale).sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Text(
                text = cell.playerLetter?.toString().orEmpty(),
                modifier = Modifier.align(Alignment.Center),
                color = colors.content,
                fontSize = ((if (largeText) 25f else 22f) * metrics.textScale).sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CrosswordCluePanel(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    actions: CrosswordGameActions,
    onExpandClues: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.cluePanelHeight.dp)
                .testTag("crosswordCluePanel")
                .clickable(onClick = onExpandClues)
                .semantics(mergeDescendants = true) {},
        color = Color(0xFFEAF6FF).copy(alpha = 0.98f),
        border = BorderStroke(2.dp, Color(0xFF1788D8)),
        shape = RoundedCornerShape((22f * metrics.textScale).dp),
        shadowElevation = 4.dp,
    ) {
        if (state.isCompleted) {
            CrosswordCompletionPanel(state = state, metrics = metrics, onReturnHome = actions.onReturnHome)
        } else {
            Row(
                modifier = Modifier.padding((10f * metrics.textScale).dp),
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrosswordSmallAction(
                    "<",
                    metrics,
                    actions.onPrevious,
                    Modifier
                        .width((42f * metrics.textScale).coerceAtLeast(34f).dp)
                        .testTag("crosswordPreviousClue"),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((3f * metrics.textScale).dp)) {
                    QuestAutoText(
                        text = state.activeClueLabel,
                        modifier = Modifier.fillMaxWidth().height((22f * metrics.textScale).coerceAtLeast(16f).dp),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 14f * metrics.textScale,
                                minFontSizeSp = 6f,
                                maxLines = 1,
                                color = Color(0xFF073B6D),
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Start,
                                softWrap = false,
                                contentAlignment = Alignment.CenterStart,
                            ),
                    )
                    QuestAutoText(
                        text = state.activeClueText,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 13f * metrics.textScale,
                                minFontSizeSp = 6f,
                                maxLines = 3,
                                color = Color(0xFF173444),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start,
                                contentAlignment = Alignment.TopStart,
                            ),
                    )
                    state.hintTexts.lastOrNull()?.let { hint ->
                        Text(
                            text = hint,
                            color = Color(0xFF084A7A),
                            fontSize = (10f * metrics.textScale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                Column(
                    modifier = Modifier.width((86f * metrics.textScale).coerceAtLeast(70f).dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy((5f * metrics.textScale).dp),
                ) {
                    CrosswordSmallAction(
                        "Hint ${state.hintsRemaining}",
                        metrics,
                        actions.onUseHint,
                        Modifier
                            .weight(1f)
                            .testTag("crosswordHintButton"),
                        state.canUseHint,
                    )
                    CrosswordSmallAction(
                        ">",
                        metrics,
                        actions.onNext,
                        Modifier
                            .weight(1f)
                            .testTag("crosswordNextClue"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrosswordCompletionPanel(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    onReturnHome: () -> Unit,
) {
    Row(
        modifier = Modifier.padding((11f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp)) {
            Text("Crossword complete", color = Color(0xFF073B6D), fontWeight = FontWeight.Black, fontSize = (16f * metrics.textScale).sp)
            Text(state.sharePattern.orEmpty(), color = Color(0xFF173444), fontSize = (10f * metrics.textScale).sp, maxLines = 3)
        }
        Button(
            onClick = onReturnHome,
            modifier =
                Modifier
                    .width((88f * metrics.textScale).coerceAtLeast(70f).dp)
                    .fillMaxHeight()
                    .testTag("crosswordDoneButton"),
            shape = RoundedCornerShape((16f * metrics.textScale).dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Text("Done", fontWeight = FontWeight.Black, fontSize = (14f * metrics.textScale).sp)
        }
    }
}

@Composable
private fun CrosswordSmallAction(
    label: String,
    metrics: CrosswordLayoutMetrics,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape((12f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1788D8), contentColor = Color.White),
    ) {
        QuestAutoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 12f * metrics.textScale,
                    minFontSizeSp = 5f,
                    maxLines = 1,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
private fun CrosswordKeyboard(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    actions: CrosswordGameActions,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("crosswordKeyboard"),
        verticalArrangement = Arrangement.spacedBy(metrics.keyboardGap.dp),
    ) {
        state.keyboardRows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((3f * metrics.textScale).dp),
            ) {
                row.forEach { letter ->
                    CrosswordKey(
                        label = letter.toString(),
                        metrics = metrics,
                        enabled = !state.isCompleted,
                        onClick = { actions.onLetter(letter) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .testTag("crosswordKey-$letter"),
                    )
                }
                if (rowIndex == state.keyboardRows.lastIndex) {
                    CrosswordKey(
                        label = "Delete",
                        metrics = metrics,
                        enabled = !state.isCompleted,
                        onClick = actions.onDelete,
                        modifier =
                            Modifier
                                .weight(1.45f)
                                .testTag("crosswordDeleteKey"),
                    )
                }
            }
        }
    }
}

@Composable
private fun CrosswordKey(
    label: String,
    metrics: CrosswordLayoutMetrics,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(metrics.keyHeight.dp),
        shape = RoundedCornerShape((9f * metrics.textScale).dp),
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.94f),
                contentColor = Color(0xFF073B6D),
                disabledContainerColor = Color.White.copy(alpha = 0.62f),
                disabledContentColor = Color(0xFF073B6D).copy(alpha = 0.5f),
            ),
    ) {
        QuestAutoText(
            text = label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 15f * metrics.textScale,
                    minFontSizeSp = 5f,
                    maxLines = 1,
                    color = Color(0xFF073B6D),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

@Composable
private fun CrosswordExpandedClueList(
    state: CrosswordUiState,
    metrics: CrosswordLayoutMetrics,
    actions: CrosswordGameActions,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(metrics.expandedPanelHeight.dp)
                .testTag("crosswordExpandedClueList")
                .clickable(onClick = onCollapse),
        color = Color(0xFFEAF6FF).copy(alpha = 0.99f),
        border = BorderStroke(2.dp, Color(0xFF1788D8)),
        shape = RoundedCornerShape((24f * metrics.textScale).dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding((14f * metrics.textScale).dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        ) {
            Text("Across", color = Color(0xFF073B6D), fontWeight = FontWeight.Black, fontSize = (17f * metrics.textScale).sp)
            state.cluesAcross.forEach { clue ->
                CrosswordClueRow(clue, metrics) {
                    actions.onEntry(clue.entryIndex)
                    onCollapse()
                }
            }
            Text("Down", color = Color(0xFF073B6D), fontWeight = FontWeight.Black, fontSize = (17f * metrics.textScale).sp)
            state.cluesDown.forEach { clue ->
                CrosswordClueRow(clue, metrics) {
                    actions.onEntry(clue.entryIndex)
                    onCollapse()
                }
            }
        }
    }
}

@Composable
private fun CrosswordClueRow(
    clue: CrosswordClueUiState,
    metrics: CrosswordLayoutMetrics,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (clue.isSelected) Color(0xFFFFF7D6) else Color.White.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, if (clue.isSolved) Color(0xFF3DBB5D) else Color(0xFF8FC5E9)),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
    ) {
        Row(
            modifier = Modifier.padding((9f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        ) {
            Text(
                text = clue.number.toString(),
                color = Color(0xFF073B6D),
                fontWeight = FontWeight.Black,
                fontSize = (13f * metrics.textScale).sp,
            )
            Text(
                text = clue.clue,
                modifier = Modifier.weight(1f),
                color = Color(0xFF173444),
                fontWeight = FontWeight.Bold,
                fontSize = (12f * metrics.textScale).sp,
            )
            Text(
                text = if (clue.isSolved) "Done" else "${clue.answerLength}",
                color = if (clue.isSolved) Color(0xFF167D31) else Color(0xFF50616E),
                fontWeight = FontWeight.Black,
                fontSize = (11f * metrics.textScale).sp,
            )
        }
    }
}

internal object CrosswordUiMapper {
    fun map(
        puzzle: CrosswordPuzzle,
        gameState: CrosswordSaveState,
        settings: QuestSettings,
        homeState: DailyHomeUiState,
        transientMessage: String?,
    ): CrosswordUiState {
        val cells = CrosswordGameEngine.boardCells(puzzle, gameState).map { it.toUiState() }
        val clueStates = CrosswordGameEngine.clueStates(puzzle, gameState)
        val selected = clueStates.firstOrNull { it.isSelected } ?: clueStates.firstOrNull()
        val solvedCount = clueStates.count { it.isSolved }
        val shareCard =
            if (gameState.isCompleted) {
                CrosswordGameEngine.shareCard(
                    puzzle = puzzle,
                    state = gameState,
                    utcDate = homeState.date.toString(),
                    currentStreak = homeState.currentDailyFiveStreak,
                    bestStreak = homeState.bestDailyFiveStreak,
                )
            } else {
                null
            }
        val safeSharePattern = shareCard?.takeUnless { ShareSafety.leaksForbiddenPayload(it) }?.visibleResultPattern
        val message = transientMessage ?: if (gameState.isCompleted) CrosswordMessage.PUZZLE_COMPLETE.userText else null

        return CrosswordUiState(
            width = puzzle.width,
            height = puzzle.height,
            cells = cells,
            cluesAcross =
                clueStates
                    .filter { it.clue.direction == CrosswordDirection.ACROSS }
                    .map { it.toUiState() },
            cluesDown =
                clueStates
                    .filter { it.clue.direction == CrosswordDirection.DOWN }
                    .map { it.toUiState() },
            progressBadge = "$solvedCount/${puzzle.entries.size}",
            hintBadge = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0).toString(),
            streakLabel = "Streak ${crosswordStreak(homeState)}",
            statusText = message ?: "Fill the crossword.",
            directionLabel =
                selected
                    ?.clue
                    ?.direction
                    ?.let(::formatDirection)
                    .orEmpty(),
            activeClueLabel =
                selected
                    ?.let { "${it.clue.number} ${formatDirection(it.clue.direction)} (${it.clue.answerLength})" }
                    .orEmpty(),
            activeClueText = selected?.clue?.clue ?: "Choose a clue.",
            hintTexts = CrosswordGameEngine.revealedHintTexts(puzzle, gameState),
            hintsRemaining = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0),
            canUseHint = !gameState.isCompleted && gameState.revealedHintOrders.size < puzzle.hints.size,
            keyboardRows = listOf("QWERTYUIOP".toList(), "ASDFGHJKL".toList(), "ZXCVBNM".toList()),
            isCompleted = gameState.isCompleted,
            sharePattern = safeSharePattern,
            largeText = settings.largePuzzleText,
        )
    }

    private fun crosswordStreak(homeState: DailyHomeUiState): Int =
        homeState.cards
            .firstOrNull { it.category == PuzzleCategory.CROSSWORD }
            ?.categoryStreak ?: 0

    private fun formatDirection(direction: CrosswordDirection): String = direction.name.lowercase().replaceFirstChar { it.uppercase() }

    private fun CrosswordBoardCell.toUiState(): CrosswordBoardCellUiState =
        CrosswordBoardCellUiState(
            row = row,
            column = column,
            number = number,
            playerLetter = playerLetter,
            isPlayable = isPlayable,
            isActive = isActive,
            isInActiveEntry = isInActiveEntry,
            isRevealed = isRevealed,
            isCorrect = isCorrect,
        )

    private fun CrosswordClueState.toUiState(): CrosswordClueUiState =
        CrosswordClueUiState(
            entryIndex = clue.entryIndex,
            number = clue.number,
            direction = clue.direction,
            clue = clue.clue,
            answerLength = clue.answerLength,
            isSelected = isSelected,
            isSolved = isSolved,
        )
}

internal data class CrosswordRouteData(
    val puzzle: CrosswordPuzzle,
    val settings: QuestSettings,
    val homeState: DailyHomeUiState,
)

internal data class CrosswordRouteDependencies(
    val progressStore: ProgressStore,
    val crosswordProgressStore: CrosswordProgressStore,
    val dayIndex: Int,
    val todaysPuzzleIds: Set<String>,
)

internal data class CrosswordGameActions(
    val onBack: () -> Unit,
    val onCell: (Int, Int) -> Unit,
    val onEntry: (Int) -> Unit,
    val onToggleDirection: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onUseHint: () -> Unit,
    val onLetter: (Char) -> Unit,
    val onDelete: () -> Unit,
    val onReturnHome: () -> Unit,
)

internal data class CrosswordUiState(
    val width: Int,
    val height: Int,
    val cells: List<CrosswordBoardCellUiState>,
    val cluesAcross: List<CrosswordClueUiState>,
    val cluesDown: List<CrosswordClueUiState>,
    val progressBadge: String,
    val hintBadge: String,
    val streakLabel: String,
    val statusText: String,
    val directionLabel: String,
    val activeClueLabel: String,
    val activeClueText: String,
    val hintTexts: List<String>,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val keyboardRows: List<List<Char>>,
    val isCompleted: Boolean,
    val sharePattern: String?,
    val largeText: Boolean,
)

internal data class CrosswordBoardCellUiState(
    val row: Int,
    val column: Int,
    val number: Int?,
    val playerLetter: Char?,
    val isPlayable: Boolean,
    val isActive: Boolean,
    val isInActiveEntry: Boolean,
    val isRevealed: Boolean,
    val isCorrect: Boolean,
)

internal data class CrosswordClueUiState(
    val entryIndex: Int,
    val number: Int,
    val direction: CrosswordDirection,
    val clue: String,
    val answerLength: Int,
    val isSelected: Boolean,
    val isSolved: Boolean,
)

private data class CrosswordCellColors(
    val background: Color,
    val border: Color,
    val content: Color,
)

private fun crosswordCellColors(cell: CrosswordBoardCellUiState): CrosswordCellColors =
    when {
        !cell.isPlayable -> CrosswordCellColors(Color(0xFF07365F).copy(alpha = 0.82f), Color(0xFF07365F), Color.White)
        cell.isActive -> CrosswordCellColors(Color(0xFFFFD34D), Color(0xFFC38B00), Color(0xFF173444))
        cell.isRevealed -> CrosswordCellColors(Color(0xFFD9F7FF), Color(0xFF1788D8), Color(0xFF073B6D))
        cell.isInActiveEntry -> CrosswordCellColors(Color(0xFFE7F7FF), Color(0xFF65B8E8), Color(0xFF073B6D))
        cell.isCorrect -> CrosswordCellColors(Color(0xFFE7FFE7), Color(0xFF3DBB5D), Color(0xFF073B6D))
        else -> CrosswordCellColors(Color.White.copy(alpha = 0.96f), Color(0xFF8FC5E9), Color(0xFF073B6D))
    }
