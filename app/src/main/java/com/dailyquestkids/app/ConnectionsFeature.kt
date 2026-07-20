package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.puzzle.engine.ConnectionsCompletionEvent
import com.dailyquestkids.puzzle.engine.ConnectionsGameEngine
import com.dailyquestkids.puzzle.engine.ConnectionsMessage
import com.dailyquestkids.puzzle.engine.ConnectionsMoveResult
import com.dailyquestkids.puzzle.engine.ConnectionsSaveState
import com.dailyquestkids.puzzle.engine.ShareSafety
import kotlinx.coroutines.launch

@Composable
internal fun ConnectionsRoute(
    data: ConnectionsRouteData,
    dependencies: ConnectionsRouteDependencies,
    onBack: () -> Unit,
    onReturnHome: () -> Unit,
    shareActions: ShareActions,
) {
    val savedState by dependencies.connectionsProgressStore
        .stateFor(data.puzzle.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val initialState = remember(data.puzzle.id) { ConnectionsGameEngine.initial(data.puzzle) }
    val sessionState = remember(data.puzzle.id) { mutableStateOf<ConnectionsSaveState?>(null) }
    val gameState = sessionState.value ?: savedState ?: initialState
    val scope = rememberCoroutineScope()
    var transientMessage by remember(data.puzzle.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(savedState, data.puzzle.id) {
        savedState?.let { state ->
            sessionState.value = ConnectionsRouteStateReducer.mergeSavedState(sessionState.value, state)
        }
    }

    fun currentGameState(): ConnectionsSaveState = sessionState.value ?: savedState ?: initialState

    fun saveState(nextState: ConnectionsSaveState) {
        sessionState.value = nextState
        scope.launch { dependencies.connectionsProgressStore.save(nextState) }
    }

    fun persist(result: ConnectionsMoveResult) {
        sessionState.value = result.state
        transientMessage = result.message?.userText
        scope.launch {
            dependencies.connectionsProgressStore.save(result.state)
            handleConnectionsCompletion(
                event = result.completionEvent,
                resultState = result.state,
                dependencies = dependencies,
            )
            if (result.state.isFailed) {
                dependencies.progressStore.markFailed(result.state.puzzleId)
            }
        }
    }

    ConnectionsGameScreen(
        state =
            ConnectionsUiMapper.map(
                puzzle = data.puzzle,
                gameState = gameState,
                homeState = data.homeState,
                transientMessage = transientMessage,
            ),
        actions =
            ConnectionsGameActions(
                onBack = onBack,
                onTile = { word ->
                    saveState(ConnectionsGameEngine.toggleTile(data.puzzle, currentGameState(), word))
                    transientMessage = null
                },
                onSubmit = { persist(ConnectionsGameEngine.submit(data.puzzle, currentGameState())) },
                onShuffle = {
                    saveState(ConnectionsGameEngine.shuffle(data.puzzle, currentGameState()))
                    transientMessage = null
                },
                onDeselect = {
                    saveState(ConnectionsGameEngine.deselectAll(currentGameState()))
                    transientMessage = null
                },
                onUseHint = {
                    val confirm = currentGameState().awaitingRevealConfirmation
                    persist(ConnectionsGameEngine.revealHint(data.puzzle, currentGameState(), confirmReveal = confirm))
                },
                onReturnHome = onReturnHome,
                shareActions = shareActions,
            ),
    )
}

private suspend fun handleConnectionsCompletion(
    event: ConnectionsCompletionEvent?,
    resultState: ConnectionsSaveState,
    dependencies: ConnectionsRouteDependencies,
) {
    if (event == null) return

    dependencies.progressStore.markCompleted(
        puzzleId = event.puzzleId,
        dayIndex = dependencies.dayIndex,
        todaysPuzzleIds = dependencies.todaysPuzzleIds,
        hintsUsed = event.hintsUsed,
    )
    dependencies.connectionsProgressStore.save(ConnectionsGameEngine.acknowledgeCompletion(resultState))
}

internal object ConnectionsRouteStateReducer {
    fun mergeSavedState(
        sessionState: ConnectionsSaveState?,
        savedState: ConnectionsSaveState,
    ): ConnectionsSaveState =
        when {
            sessionState == null -> savedState
            sessionState.puzzleId != savedState.puzzleId -> savedState
            savedState.progressRank() > sessionState.progressRank() -> savedState
            else -> sessionState
        }

    private fun ConnectionsSaveState.progressRank(): Int =
        solvedGroupTitles.size * 10_000 +
            revealedHintOrders.size * 100 +
            mistakeCount * 10 +
            selectedWords.size +
            if (isCompleted || isFailed) 100_000 else 0
}

@Composable
internal fun ConnectionsGameScreen(
    state: ConnectionsUiState,
    actions: ConnectionsGameActions,
) {
    QuestSceneFrame(testTag = "connectionsScreen") { screenMetrics ->
        val metrics =
            ConnectionsLayoutCalculator.calculate(
                widthDp = screenMetrics.widthDp,
                heightDp = screenMetrics.heightDp,
                solvedCount = state.solvedGroups.size,
                tileCount = state.tiles.size,
            )
        Column(
            modifier =
                Modifier
                    .width(metrics.contentWidth.dp)
                    .fillMaxHeight()
                    .padding(vertical = metrics.verticalPadding.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
        ) {
            ConnectionsTopBar(state = state, metrics = metrics, onBack = actions.onBack)
            ConnectionsStatusStrip(state = state, metrics = metrics)
            ConnectionsSolvedGroups(state = state, metrics = metrics)
            if (state.tiles.isNotEmpty()) {
                ConnectionsTileGrid(state = state, metrics = metrics, actions = actions)
            }
            ConnectionsActionRow(state = state, metrics = metrics, actions = actions)
            ConnectionsHintPanel(state = state, metrics = metrics, actions = actions)
        }
    }
}

internal object ConnectionsLayoutCalculator {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
        solvedCount: Int = 0,
        tileCount: Int = 16,
    ): ConnectionsLayoutMetrics {
        val horizontalPadding = if (widthDp < 360f) 10f else 14f
        val contentWidth = (widthDp - horizontalPadding * 2f).coerceAtMost(720f)
        val verticalPadding = (heightDp * 0.01f).coerceIn(5f, 10f)
        val sectionGap = (heightDp * 0.006f).coerceIn(3.5f, 7f)
        val topBarHeight = (heightDp * 0.072f).coerceIn(48f, 66f)
        val statusHeight = (heightDp * 0.052f).coerceIn(34f, 48f)
        val groupBarHeight = (heightDp * 0.062f).coerceIn(38f, 54f)
        val actionHeight = (heightDp * 0.052f).coerceIn(34f, 48f)
        val hintPanelHeight = (heightDp * 0.145f).coerceIn(84f, 124f)
        val solvedHeight = groupBarHeight * solvedCount + sectionGap * solvedCount.coerceAtLeast(0)
        val gridBudget =
            heightDp -
                verticalPadding * 2f -
                topBarHeight -
                statusHeight -
                solvedHeight -
                actionHeight -
                hintPanelHeight -
                sectionGap * 5f
        val rowCount = ((tileCount + 3) / 4).coerceAtLeast(0)
        val tileGap = (contentWidth * 0.014f).coerceIn(4f, 7f)
        val widthLimitedTileSize = (contentWidth - tileGap * 3f) / 4f
        val heightLimitedTileSize =
            if (rowCount == 0) {
                0f
            } else {
                (gridBudget - tileGap * (rowCount - 1)).coerceAtLeast(0f) / rowCount
            }
        val minimumTileSize = if (widthDp < 340f || heightDp < 560f) 30f else 38f
        val tileSize =
            if (rowCount == 0) {
                0f
            } else {
                minOf(widthLimitedTileSize, heightLimitedTileSize).coerceAtLeast(minimumTileSize)
            }
        val gridWidth = if (rowCount == 0) 0f else tileSize * 4f + tileGap * 3f
        val gridHeight = if (rowCount == 0) 0f else tileSize * rowCount + tileGap * (rowCount - 1)
        val textScale = (tileSize / 76f).coerceIn(0.62f, 1.08f)
        return ConnectionsLayoutMetrics(
            contentWidth = contentWidth,
            verticalPadding = verticalPadding,
            sectionGap = sectionGap,
            topBarHeight = topBarHeight,
            statusHeight = statusHeight,
            groupBarHeight = groupBarHeight,
            solvedHeight = solvedHeight,
            actionHeight = actionHeight,
            hintPanelHeight = hintPanelHeight,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            tileGap = tileGap,
            tileSize = tileSize,
            textScale = textScale,
        )
    }
}

internal data class ConnectionsLayoutMetrics(
    val contentWidth: Float,
    val verticalPadding: Float,
    val sectionGap: Float,
    val topBarHeight: Float,
    val statusHeight: Float,
    val groupBarHeight: Float,
    val solvedHeight: Float,
    val actionHeight: Float,
    val hintPanelHeight: Float,
    val gridWidth: Float,
    val gridHeight: Float,
    val tileGap: Float,
    val tileSize: Float,
    val textScale: Float,
) {
    val totalPhoneHeight: Float =
        verticalPadding * 2f +
            topBarHeight +
            statusHeight +
            solvedHeight +
            gridHeight +
            actionHeight +
            hintPanelHeight +
            sectionGap * 5f
}

@Composable
private fun ConnectionsTopBar(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
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
            Text("<", fontWeight = FontWeight.Black, fontSize = (24f * metrics.textScale).sp, color = Color(0xFF8B2D35))
        }
        Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape((18f * metrics.textScale).dp),
            color = Color(0xFFFFF0EA).copy(alpha = 0.97f),
            border = BorderStroke(2.dp, Color(0xFFE45765)),
        ) {
            Row(
                modifier = Modifier.padding((7f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                QuestIconTile(
                    category = PuzzleCategory.CONNECTIONS,
                    modifier = Modifier.size((metrics.topBarHeight - 18f).coerceAtLeast(34f).dp),
                    scale = metrics.textScale,
                )
                QuestAutoText(
                    text = "Connections",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 24f * metrics.textScale,
                            minFontSizeSp = 11f,
                            maxLines = 1,
                            color = Color(0xFF8B2D35),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                ConnectionsTinyBadge("Groups", state.progressBadge, metrics, Modifier.testTag("connectionsProgressBadge"))
                ConnectionsTinyBadge("Hints", state.hintBadge, metrics, Modifier.testTag("connectionsHintsBadge"))
            }
        }
    }
}

@Composable
private fun ConnectionsTinyBadge(
    label: String,
    value: String,
    metrics: ConnectionsLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width((58f * metrics.textScale).coerceIn(46f, 66f).dp).fillMaxHeight(),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFFE45765)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(label, color = Color(0xFF4D2630), fontWeight = FontWeight.Bold, fontSize = (9f * metrics.textScale).sp)
            Text(value, color = Color(0xFF8B2D35), fontWeight = FontWeight.Black, fontSize = (15f * metrics.textScale).sp)
        }
    }
}

@Composable
private fun ConnectionsStatusStrip(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(metrics.statusHeight.dp).testTag("connectionsStatusStrip"),
        horizontalArrangement = Arrangement.spacedBy((6f * metrics.textScale).dp),
    ) {
        ConnectionsPill("Tries ${state.remainingMistakes}", metrics, Modifier.weight(0.8f))
        ConnectionsPill(state.statusText, metrics, Modifier.weight(1.8f))
        ConnectionsPill("${state.selectedCount}/4", metrics, Modifier.weight(0.7f))
    }
}

@Composable
private fun ConnectionsPill(
    text: String,
    metrics: ConnectionsLayoutMetrics,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape((16f * metrics.textScale).dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color(0xFFE45765)),
    ) {
        QuestAutoText(
            text = text,
            modifier = Modifier.fillMaxSize().padding(horizontal = (8f * metrics.textScale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 13f * metrics.textScale,
                    minFontSizeSp = 6f,
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
private fun ConnectionsSolvedGroups(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
    ) {
        state.solvedGroups.forEachIndexed { index, group ->
            Surface(
                modifier = Modifier.fillMaxWidth().height(metrics.groupBarHeight.dp).testTag("connectionsSolvedGroup-$index"),
                shape = RoundedCornerShape((13f * metrics.textScale).dp),
                color = group.color,
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.9f)),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = (10f * metrics.textScale).dp, vertical = (4f * metrics.textScale).dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    QuestAutoText(
                        text = group.title,
                        modifier = Modifier.fillMaxWidth().weight(0.45f),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 13f * metrics.textScale,
                                minFontSizeSp = 6f,
                                maxLines = 1,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center,
                                softWrap = false,
                            ),
                    )
                    QuestAutoText(
                        text = group.words.joinToString("  "),
                        modifier = Modifier.fillMaxWidth().weight(0.55f),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 10f * metrics.textScale,
                                minFontSizeSp = 5f,
                                maxLines = 1,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                softWrap = false,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionsTileGrid(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
    actions: ConnectionsGameActions,
) {
    Column(
        modifier =
            Modifier
                .width(metrics.gridWidth.dp)
                .height(metrics.gridHeight.dp)
                .testTag("connectionsTileGrid"),
        verticalArrangement = Arrangement.spacedBy(metrics.tileGap.dp),
    ) {
        state.tiles.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(metrics.tileSize.dp),
                horizontalArrangement = Arrangement.spacedBy(metrics.tileGap.dp),
            ) {
                row.forEach { tile ->
                    ConnectionsTile(tile = tile, metrics = metrics, onClick = { actions.onTile(tile.word) }, modifier = Modifier.weight(1f))
                }
                repeat(4 - row.size) {
                    Surface(modifier = Modifier.weight(1f).fillMaxHeight(), color = Color.Transparent) {}
                }
            }
        }
    }
}

@Composable
private fun ConnectionsTile(
    tile: ConnectionsTileUiState,
    metrics: ConnectionsLayoutMetrics,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val borderColor =
        when {
            tile.isSelected -> Color(0xFFFFC736)
            tile.isRecentMistake -> Color(0xFFE53E3E)
            else -> Color(0xFFE45765)
        }
    val background =
        when {
            tile.isSelected -> Color(0xFFFFE17A)
            tile.isRecentMistake -> Color(0xFFFFE3E3)
            else -> Color.White.copy(alpha = 0.95f)
        }
    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .testTag("connectionsTile-${tile.word}")
                .clickable(enabled = tile.enabled, onClick = onClick),
        shape = RoundedCornerShape((12f * metrics.textScale).dp),
        color = background,
        border = BorderStroke(if (tile.isSelected) 3.dp else 2.dp, borderColor),
    ) {
        QuestAutoText(
            text = tile.word,
            modifier = Modifier.fillMaxSize().padding((5f * metrics.textScale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 13f * metrics.textScale,
                    minFontSizeSp = 5f,
                    maxLines = 2,
                    color = Color(0xFF1D2735),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

@Composable
private fun ConnectionsActionRow(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
    actions: ConnectionsGameActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(metrics.actionHeight.dp),
        horizontalArrangement = Arrangement.spacedBy((6f * metrics.textScale).dp),
    ) {
        ConnectionsButton(
            label = "Shuffle",
            metrics = metrics,
            enabled = !state.isTerminal,
            onClick = actions.onShuffle,
            modifier =
                Modifier
                    .weight(1f)
                    .testTag("connectionsShuffle"),
        )
        ConnectionsButton(
            label = "Deselect",
            metrics = metrics,
            enabled = state.selectedCount > 0 && !state.isTerminal,
            onClick = actions.onDeselect,
            modifier =
                Modifier
                    .weight(1f)
                    .testTag("connectionsDeselect"),
        )
        ConnectionsButton(
            label = "Submit",
            metrics = metrics,
            enabled = state.canSubmit,
            onClick = actions.onSubmit,
            modifier =
                Modifier
                    .weight(1.2f)
                    .testTag("connectionsSubmit"),
        )
    }
}

@Composable
private fun ConnectionsHintPanel(
    state: ConnectionsUiState,
    metrics: ConnectionsLayoutMetrics,
    actions: ConnectionsGameActions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(metrics.hintPanelHeight.dp).testTag("connectionsHintPanel"),
        shape = RoundedCornerShape((18f * metrics.textScale).dp),
        color = Color(0xFFFFF7ED).copy(alpha = 0.97f),
        border = BorderStroke(2.dp, Color(0xFFE45765)),
    ) {
        Row(
            modifier = Modifier.padding((10f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp),
            ) {
                Text(
                    text =
                        if (state.isCompleted) {
                            "Connections complete"
                        } else if (state.isFailed) {
                            "Try again tomorrow"
                        } else {
                            "Hint"
                        },
                    color = Color(0xFF8B2D35),
                    fontWeight = FontWeight.Black,
                    fontSize = (16f * metrics.textScale).sp,
                )
                Text(
                    text = state.panelText,
                    color = Color(0xFF1D2735),
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * metrics.textScale).sp,
                    lineHeight = (14f * metrics.textScale).sp,
                    maxLines = 3,
                )
            }
            if (state.isCompleted) {
                PuzzleResultShareDoneRail(
                    shareCard = state.shareCard,
                    shareActions = actions.shareActions,
                    onDone = actions.onReturnHome,
                    tagPrefix = "connections",
                    modifier = Modifier.width((92f * metrics.textScale).coerceAtLeast(74f).dp),
                )
            } else if (state.isFailed) {
                ConnectionsButton(
                    label = "Done",
                    metrics = metrics,
                    enabled = true,
                    onClick = actions.onReturnHome,
                    modifier =
                        Modifier
                            .width((92f * metrics.textScale).coerceAtLeast(74f).dp)
                            .fillMaxHeight()
                            .testTag("connectionsDoneButton"),
                )
            } else {
                ConnectionsButton(
                    label = "Hint ${state.hintsRemaining}",
                    metrics = metrics,
                    enabled = state.canUseHint,
                    onClick = actions.onUseHint,
                    modifier =
                        Modifier
                            .width((92f * metrics.textScale).coerceAtLeast(74f).dp)
                            .fillMaxHeight()
                            .testTag("connectionsHintButton"),
                )
            }
        }
    }
}

@Composable
private fun ConnectionsButton(
    label: String,
    metrics: ConnectionsLayoutMetrics,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape((12f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE45765),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFFFD9D3),
                disabledContentColor = Color(0xFF8B6B6E),
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
                    color = if (enabled) Color.White else Color(0xFF8B6B6E),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = false,
                ),
        )
    }
}

internal object ConnectionsUiMapper {
    fun map(
        puzzle: ConnectionsPuzzle,
        gameState: ConnectionsSaveState,
        homeState: DailyHomeUiState,
        transientMessage: String?,
    ): ConnectionsUiState {
        val tiles = ConnectionsGameEngine.tiles(puzzle, gameState)
        val solvedGroups = ConnectionsGameEngine.solvedGroups(puzzle, gameState)
        val safeShareCard = safeShareCard(puzzle, gameState, homeState)
        val latestHintText =
            ConnectionsGameEngine
                .revealedHintTexts(puzzle, gameState)
                .lastOrNull()
        return ConnectionsUiState(
            tiles =
                tiles.map { tile ->
                    ConnectionsTileUiState(
                        word = tile.word,
                        isSelected = tile.isSelected,
                        isRecentMistake = tile.word.lowercase() in gameState.lastMistakeWords,
                        enabled = !gameState.isCompleted && !gameState.isFailed,
                    )
                },
            solvedGroups =
                solvedGroups.mapIndexed { index, group ->
                    ConnectionsSolvedGroupUiState(
                        title = group.title,
                        words = group.words,
                        explanation = group.explanation,
                        color = groupColor(index),
                    )
                },
            progressBadge = "${gameState.solvedGroupTitles.size}/4",
            hintBadge = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0).toString(),
            statusText = statusText(gameState, transientMessage),
            selectedCount = gameState.selectedWords.size,
            remainingMistakes = (5 - gameState.mistakeCount).coerceAtLeast(0),
            hintsRemaining = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0),
            canUseHint = !gameState.isCompleted && !gameState.isFailed && gameState.revealedHintOrders.size < puzzle.hints.size,
            canSubmit = gameState.selectedWords.size == 4 && !gameState.isCompleted && !gameState.isFailed,
            panelText = panelText(safeShareCard, latestHintText, solvedGroups.lastOrNull()?.explanation),
            shareCard = if (gameState.isCompleted) safeShareCard else null,
            isCompleted = gameState.isCompleted,
            isFailed = gameState.isFailed,
        )
    }

    private fun safeShareCard(
        puzzle: ConnectionsPuzzle,
        gameState: ConnectionsSaveState,
        homeState: DailyHomeUiState,
    ): ShareCardModel? {
        if (!gameState.isCompleted && !gameState.isFailed) return null
        val shareCard =
            ConnectionsGameEngine.shareCard(
                puzzle = puzzle,
                state = gameState,
                utcDate = homeState.date.toString(),
                currentStreak = homeState.currentDailyFiveStreak,
                bestStreak = homeState.bestDailyFiveStreak,
            )
        return shareCard.takeUnless { ShareSafety.leaksForbiddenPayload(it) }
    }

    private fun statusText(
        gameState: ConnectionsSaveState,
        transientMessage: String?,
    ): String =
        transientMessage
            ?: when {
                gameState.isCompleted -> ConnectionsMessage.PUZZLE_COMPLETE.userText
                gameState.isFailed -> ConnectionsMessage.FAILED.userText
                else -> "Find groups of four."
            }

    private fun panelText(
        safeShareCard: ShareCardModel?,
        latestHintText: String?,
        latestSolvedExplanation: String?,
    ): String =
        safeShareCard?.visibleResultPattern
            ?: latestHintText
            ?: latestSolvedExplanation
            ?: "Find four words that share the same idea."

    private fun groupColor(index: Int): Color =
        listOf(Color(0xFFE45765), Color(0xFFFFA62B), Color(0xFF2D9CDB), Color(0xFF2D8C4A))[index.mod(4)]
}

internal data class ConnectionsRouteData(
    val puzzle: ConnectionsPuzzle,
    val settings: QuestSettings,
    val homeState: DailyHomeUiState,
)

internal data class ConnectionsRouteDependencies(
    val progressStore: ProgressStore,
    val connectionsProgressStore: ConnectionsProgressStore,
    val dayIndex: Int,
    val todaysPuzzleIds: Set<String>,
)

internal data class ConnectionsGameActions(
    val onBack: () -> Unit,
    val onTile: (String) -> Unit,
    val onSubmit: () -> Unit,
    val onShuffle: () -> Unit,
    val onDeselect: () -> Unit,
    val onUseHint: () -> Unit,
    val onReturnHome: () -> Unit,
    val shareActions: ShareActions,
)

internal data class ConnectionsUiState(
    val tiles: List<ConnectionsTileUiState>,
    val solvedGroups: List<ConnectionsSolvedGroupUiState>,
    val progressBadge: String,
    val hintBadge: String,
    val statusText: String,
    val selectedCount: Int,
    val remainingMistakes: Int,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val canSubmit: Boolean,
    val panelText: String,
    val shareCard: ShareCardModel?,
    val isCompleted: Boolean,
    val isFailed: Boolean,
) {
    val isTerminal: Boolean = isCompleted || isFailed
}

internal data class ConnectionsTileUiState(
    val word: String,
    val isSelected: Boolean,
    val isRecentMistake: Boolean,
    val enabled: Boolean,
)

internal data class ConnectionsSolvedGroupUiState(
    val title: String,
    val words: List<String>,
    val explanation: String,
    val color: Color,
)
