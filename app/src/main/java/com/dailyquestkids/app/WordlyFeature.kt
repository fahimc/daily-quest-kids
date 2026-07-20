package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.puzzle.engine.ShareSafety
import com.dailyquestkids.puzzle.engine.TileState
import com.dailyquestkids.puzzle.engine.WordlyBoardRow
import com.dailyquestkids.puzzle.engine.WordlyCompletionEvent
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
import com.dailyquestkids.puzzle.engine.WordlyLearningSummary
import com.dailyquestkids.puzzle.engine.WordlyMessage
import com.dailyquestkids.puzzle.engine.WordlyMoveResult
import com.dailyquestkids.puzzle.engine.WordlyOutcome
import com.dailyquestkids.puzzle.engine.WordlySaveState
import kotlinx.coroutines.launch

@Composable
internal fun WordlyRoute(
    data: WordlyRouteData,
    dependencies: WordlyRouteDependencies,
    onBack: () -> Unit,
    onReturnHome: () -> Unit,
    shareActions: ShareActions,
) {
    val savedState by dependencies.wordlyProgressStore
        .stateFor(data.puzzle.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val gameState = savedState ?: WordlyGameEngine.initial(data.puzzle)
    val scope = rememberCoroutineScope()
    var transientMessage by remember(data.puzzle.id) { mutableStateOf<String?>(null) }

    fun persist(result: WordlyMoveResult) {
        transientMessage = result.message?.userText
        scope.launch {
            dependencies.wordlyProgressStore.save(result.state)
            handleCompletion(
                event = result.completionEvent,
                resultState = result.state,
                dependencies = dependencies,
            )
        }
    }

    WordlyGameScreen(
        state =
            WordlyUiMapper.map(
                puzzle = data.puzzle,
                gameState = gameState,
                settings = data.settings,
                homeState = data.homeState,
                transientMessage = transientMessage,
            ),
        actions =
            WordlyGameActions(
                onBack = onBack,
                onUseHint = {
                    persist(WordlyGameEngine.revealHint(data.puzzle, gameState))
                },
                onLetter = { letter ->
                    persist(WordlyGameEngine.appendLetter(data.puzzle, gameState, letter))
                },
                onDelete = {
                    scope.launch {
                        dependencies.wordlyProgressStore.save(WordlyGameEngine.deleteLetter(data.puzzle, gameState))
                    }
                },
                onSubmit = {
                    persist(WordlyGameEngine.submit(data.puzzle, gameState))
                },
                onReturnHome = onReturnHome,
                shareActions = shareActions,
            ),
    )
}

private suspend fun handleCompletion(
    event: WordlyCompletionEvent?,
    resultState: WordlySaveState,
    dependencies: WordlyRouteDependencies,
) {
    if (event == null) return

    when (event.outcome) {
        WordlyOutcome.SUCCESS ->
            dependencies.progressStore.markCompleted(
                puzzleId = event.puzzleId,
                dayIndex = dependencies.dayIndex,
                todaysPuzzleIds = dependencies.todaysPuzzleIds,
                hintsUsed = event.hintsUsed,
            )
        WordlyOutcome.FAILURE -> dependencies.progressStore.markFailed(event.puzzleId)
    }

    dependencies.wordlyProgressStore.save(WordlyGameEngine.acknowledgeCompletion(resultState))
}

@Composable
internal fun WordlyGameScreen(
    state: WordlyUiState,
    actions: WordlyGameActions,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(wordlyBackgroundBrush())
                .testTag("wordlyScreen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        val metrics =
            WordlyLayoutCalculator.calculate(
                widthDp = maxWidth.value,
                heightDp = maxHeight.value,
            )
        var isClueExpanded by remember(state.clueText, state.openHintText, state.isTerminal) {
            mutableStateOf(false)
        }

        Column(
            modifier =
                Modifier
                    .width(metrics.contentWidth.dp)
                    .fillMaxHeight()
                    .padding(vertical = metrics.verticalPadding.dp),
            verticalArrangement = Arrangement.spacedBy(metrics.sectionGap.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WordlyTopBar(state = state, metrics = metrics, onBack = actions.onBack)
            WordlyStatusStrip(state = state, metrics = metrics)
            WordlyBoard(
                rows = state.rows,
                metrics = metrics,
                largeText = state.largeText,
            )
            WordlyKeyboard(
                rows = state.keyboardRows,
                metrics = metrics,
                actions = actions,
                enabled = !state.isTerminal,
            )
            WordlyCluePanel(
                state = state,
                metrics = metrics,
                actions = actions,
                onToggleExpanded = { isClueExpanded = !isClueExpanded },
            )
        }
        if (isClueExpanded && !state.isTerminal) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("wordlyClueDismissLayer")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { isClueExpanded = false },
                        ),
            )
            WordlyExpandedCluePanel(
                state = state,
                metrics = metrics,
                onUseHint = actions.onUseHint,
                onCollapse = { isClueExpanded = false },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(metrics.contentWidth.dp)
                        .padding(bottom = metrics.verticalPadding.dp),
            )
        }
    }
}

private fun wordlyBackgroundBrush(): Brush =
    Brush.verticalGradient(
        listOf(
            Color(0xFF66D2FF),
            Color(0xFFBFF4CD),
            Color(0xFF168547),
        ),
    )

internal object WordlyLayoutCalculator {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
    ): WordlyLayoutMetrics {
        val horizontalPadding = if (widthDp < 360f) 8f else 12f
        val contentWidth = (widthDp - horizontalPadding * 2f).coerceAtMost(720f)
        val verticalPadding = (heightDp * 0.01f).coerceIn(6f, 12f)
        val sectionGap = (heightDp * 0.007f).coerceIn(4f, 8f)
        val topBarHeight = (heightDp * 0.075f).coerceIn(50f, 74f)
        val statusHeight = (heightDp * 0.045f).coerceIn(30f, 42f)
        val boardGap = if (widthDp < 360f) 3f else 5f
        val keyboardGap = (heightDp * 0.006f).coerceIn(3f, 6f)
        val keyHeight = (heightDp * 0.055f).coerceIn(34f, 50f)
        val cluePanelHeight = (heightDp * 0.14f).coerceIn(88f, 132f)
        val maxExpandedCluePanelHeight = (heightDp - verticalPadding * 2f).coerceAtLeast(cluePanelHeight)
        val minExpandedCluePanelHeight = (cluePanelHeight + 44f).coerceAtMost(maxExpandedCluePanelHeight)
        val expandedCluePanelHeight =
            (heightDp * 0.32f).coerceIn(
                minExpandedCluePanelHeight,
                maxExpandedCluePanelHeight,
            )
        val keyboardHeight = keyHeight * 3f + keyboardGap * 2f
        val boardHeightBudget =
            heightDp -
                verticalPadding * 2f -
                topBarHeight -
                statusHeight -
                keyboardHeight -
                cluePanelHeight -
                sectionGap * 4f
        val tileByHeight = (boardHeightBudget - boardGap * 5f) / 6f
        val tileByWidth = (contentWidth - boardGap * 4f) / 5f
        val minimumTileSize = if (heightDp < 520f) 24f else 28f
        val tileSize = minOf(tileByHeight, tileByWidth, 96f).coerceAtLeast(minimumTileSize)
        val textScale = (tileSize / 56f).coerceIn(0.56f, 1f)

        return WordlyLayoutMetrics(
            contentWidth = contentWidth,
            verticalPadding = verticalPadding,
            sectionGap = sectionGap,
            topBarHeight = topBarHeight,
            statusHeight = statusHeight,
            tileSize = tileSize,
            boardGap = boardGap,
            keyHeight = keyHeight,
            keyboardGap = keyboardGap,
            cluePanelHeight = cluePanelHeight,
            expandedCluePanelHeight = expandedCluePanelHeight,
            textScale = textScale,
        )
    }
}

internal data class WordlyLayoutMetrics(
    val contentWidth: Float,
    val verticalPadding: Float,
    val sectionGap: Float,
    val topBarHeight: Float,
    val statusHeight: Float,
    val tileSize: Float,
    val boardGap: Float,
    val keyHeight: Float,
    val keyboardGap: Float,
    val cluePanelHeight: Float,
    val expandedCluePanelHeight: Float,
    val textScale: Float,
) {
    val totalHeight: Float =
        verticalPadding * 2f +
            topBarHeight +
            statusHeight +
            tileSize * 6f +
            boardGap * 5f +
            keyHeight * 3f +
            keyboardGap * 2f +
            cluePanelHeight +
            sectionGap * 4f
}

@Composable
private fun WordlyTopBar(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
    onBack: () -> Unit,
) {
    val iconSize = (metrics.topBarHeight - 8f).coerceAtLeast(42f).dp
    val logoSize = (metrics.topBarHeight - 16f).coerceAtLeast(34f).dp
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.topBarHeight.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
        ) {
            Text("<", fontWeight = FontWeight.Black, fontSize = (22f * metrics.textScale).sp)
        }
        Surface(
            modifier =
                Modifier
                    .weight(1f)
                    .height(metrics.topBarHeight.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF4FFE7).copy(alpha = 0.96f),
            border = BorderStroke(2.dp, Color(0xFF67A95B)),
        ) {
            Row(
                modifier = Modifier.padding((8f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(logoSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF41B939)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = (26f * metrics.textScale).sp)
                }
                AutoSizeText(
                    text = "Wordly",
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("wordlyTitleText"),
                    spec =
                        AutoSizeTextSpec(
                            maxFontSizeSp = 28f * metrics.textScale,
                            minFontSizeSp = 16f,
                            maxLines = 1,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            contentAlignment = Alignment.CenterStart,
                            softWrap = false,
                        ),
                )
                WordlyAttemptsBadge(attemptsLabel = state.attemptsLabel, metrics = metrics)
            }
        }
    }
}

@Composable
private fun WordlyAttemptsBadge(
    attemptsLabel: String,
    metrics: WordlyLayoutMetrics,
) {
    Surface(
        modifier =
            Modifier
                .width((82f * metrics.textScale).coerceIn(62f, 88f).dp)
                .fillMaxHeight()
                .testTag("wordlyAttemptsBadge"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFB4CE9F)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AutoSizeText(
                text = "Attempts",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .testTag("wordlyAttemptsLabel"),
                spec =
                    AutoSizeTextSpec(
                        maxFontSizeSp = 8.5f * metrics.textScale,
                        minFontSizeSp = 3.8f,
                        maxLines = 1,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
            AutoSizeText(
                text = attemptsLabel,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                        .testTag("wordlyAttemptsValue"),
                spec =
                    AutoSizeTextSpec(
                        maxFontSizeSp = 14f * metrics.textScale,
                        minFontSizeSp = 5f,
                        maxLines = 1,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
        }
    }
}

@Composable
private fun WordlyStatusStrip(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
) {
    val hasFeedback = state.message != null
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.statusHeight.dp),
        horizontalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WordlyPill(
            text = state.streakLabel,
            metrics = metrics,
            modifier = Modifier.weight(0.72f).testTag("wordlyStreakPill"),
            maxFontSizeSp = 10f * metrics.textScale,
        )
        WordlyPill(
            text = state.statusPrompt,
            metrics = metrics,
            modifier = Modifier.weight(2.4f).testTag("wordlyPromptPill"),
            maxFontSizeSp = if (hasFeedback) 9f * metrics.textScale else 9.6f * metrics.textScale,
            style =
                if (hasFeedback) {
                    WordlyPillStyle(
                        backgroundColor = Color(0xFFFFF7D6),
                        borderColor = Color(0xFFD9A914),
                        textColor = Color(0xFF2E2A14),
                    )
                } else {
                    WordlyPillStyle()
                },
        )
        WordlyPill(
            text = state.starLabel,
            metrics = metrics,
            modifier = Modifier.weight(0.76f).testTag("wordlyStarsPill"),
            maxFontSizeSp = 10f * metrics.textScale,
        )
    }
}

@Composable
private fun WordlyPill(
    text: String,
    metrics: WordlyLayoutMetrics,
    modifier: Modifier,
    maxFontSizeSp: Float,
    style: WordlyPillStyle = WordlyPillStyle(),
) {
    Surface(
        modifier =
            modifier
                .fillMaxHeight()
                .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(18.dp),
        color = style.backgroundColor,
        border = BorderStroke(1.dp, style.borderColor),
    ) {
        AutoSizeText(
            text = text,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = (5f * metrics.textScale).dp),
            spec =
                AutoSizeTextSpec(
                    maxFontSizeSp = maxFontSizeSp,
                    minFontSizeSp = 3.8f,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = style.textColor,
                    softWrap = false,
                ),
        )
    }
}

private data class WordlyPillStyle(
    val backgroundColor: Color = Color(0xFFF8FFF0).copy(alpha = 0.94f),
    val borderColor: Color = Color(0xFF75B65B),
    val textColor: Color = Color.Unspecified,
)

@Composable
private fun AutoSizeText(
    text: String,
    modifier: Modifier,
    spec: AutoSizeTextSpec,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = spec.contentAlignment) {
        val maxWidthPx = with(density) { maxWidth.toPx().toInt().coerceAtLeast(1) }
        val maxHeightPx = with(density) { maxHeight.toPx().toInt().coerceAtLeast(1) }
        val constraints =
            Constraints(
                maxWidth = maxWidthPx,
                maxHeight = maxHeightPx,
            )
        val fittedFontSize =
            remember(
                text,
                maxWidthPx,
                maxHeightPx,
                spec,
            ) {
                fittedFontSizeSp(
                    text = text,
                    textMeasurer = textMeasurer,
                    constraints = constraints,
                    spec = spec,
                )
            }

        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = spec.color,
            fontWeight = spec.fontWeight,
            fontSize = fittedFontSize.sp,
            lineHeight = (fittedFontSize * 1.16f).sp,
            maxLines = spec.maxLines,
            overflow = TextOverflow.Clip,
            softWrap = spec.softWrap,
            textAlign = spec.textAlign,
        )
    }
}

private data class AutoSizeTextSpec(
    val maxFontSizeSp: Float,
    val minFontSizeSp: Float,
    val maxLines: Int,
    val fontWeight: FontWeight,
    val textAlign: TextAlign,
    val color: Color = Color.Unspecified,
    val softWrap: Boolean = true,
    val contentAlignment: Alignment = Alignment.Center,
)

private fun fittedFontSizeSp(
    text: String,
    textMeasurer: TextMeasurer,
    constraints: Constraints,
    spec: AutoSizeTextSpec,
): Float {
    val minimumFontSize = minOf(spec.minFontSizeSp, spec.maxFontSizeSp)
    var fontSize = spec.maxFontSizeSp
    while (fontSize >= minimumFontSize) {
        val result =
            textMeasurer.measure(
                text = AnnotatedString(text),
                style =
                    TextStyle(
                        fontSize = fontSize.sp,
                        fontWeight = spec.fontWeight,
                        lineHeight = (fontSize * 1.16f).sp,
                        textAlign = spec.textAlign,
                    ),
                overflow = TextOverflow.Clip,
                softWrap = spec.softWrap,
                maxLines = spec.maxLines,
                constraints = constraints,
            )
        if (!result.didOverflowWidth && !result.didOverflowHeight) {
            return fontSize
        }
        fontSize -= 0.25f
    }
    return minimumFontSize
}

@Composable
private fun WordlyBoard(
    rows: List<WordlyBoardRow>,
    metrics: WordlyLayoutMetrics,
    largeText: Boolean,
) {
    Column(
        modifier = Modifier.testTag("wordlyBoard"),
        verticalArrangement = Arrangement.spacedBy(metrics.boardGap.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(metrics.boardGap.dp)) {
                row.letters.forEachIndexed { columnIndex, letter ->
                    WordlyTile(
                        letter = letter,
                        state = row.states[columnIndex],
                        metrics = metrics,
                        largeText = largeText,
                        tag = "wordlyTile-$rowIndex-$columnIndex",
                    )
                }
            }
        }
    }
}

@Composable
private fun WordlyTile(
    letter: Char,
    state: TileState,
    metrics: WordlyLayoutMetrics,
    largeText: Boolean,
    tag: String,
) {
    val colors = tileColors(state)
    Surface(
        modifier =
            Modifier
                .size(metrics.tileSize.dp)
                .testTag(tag),
        shape = RoundedCornerShape(10.dp),
        color = colors.container,
        border = BorderStroke(2.dp, colors.border),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (letter.isWhitespace()) "" else letter.toString(),
                color = colors.content,
                fontWeight = FontWeight.Black,
                fontSize = ((if (largeText) 32f else 28f) * metrics.textScale).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WordlyKeyboard(
    rows: List<List<WordlyKeyUiState>>,
    metrics: WordlyLayoutMetrics,
    actions: WordlyGameActions,
    enabled: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("wordlyKeyboard"),
        verticalArrangement = Arrangement.spacedBy(metrics.keyboardGap.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((3f * metrics.textScale).dp),
            ) {
                if (rowIndex == rows.lastIndex) {
                    KeyboardAction("Check", metrics, actions.onSubmit, enabled, Modifier.weight(1.3f))
                }
                row.forEach { key ->
                    LetterKey(
                        key = key,
                        metrics = metrics,
                        onLetter = actions.onLetter,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowIndex == rows.lastIndex) {
                    KeyboardAction("Delete", metrics, actions.onDelete, enabled, Modifier.weight(1.3f))
                }
            }
        }
    }
}

@Composable
private fun LetterKey(
    key: WordlyKeyUiState,
    metrics: WordlyLayoutMetrics,
    onLetter: (Char) -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val colors = tileColors(key.state)
    Button(
        onClick = { onLetter(key.letter) },
        enabled = enabled,
        modifier =
            modifier
                .height(metrics.keyHeight.dp),
        shape = RoundedCornerShape(9.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = colors.container,
                contentColor = colors.content,
                disabledContainerColor = colors.container.copy(alpha = 0.72f),
                disabledContentColor = colors.content.copy(alpha = 0.72f),
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(key.letter.toString(), fontWeight = FontWeight.Black, fontSize = (18f * metrics.textScale).sp)
    }
}

@Composable
private fun KeyboardAction(
    label: String,
    metrics: WordlyLayoutMetrics,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(metrics.keyHeight.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = (11f * metrics.textScale).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WordlyCluePanel(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
    actions: WordlyGameActions,
    onToggleExpanded: () -> Unit,
) {
    val panelModifier =
        Modifier
            .fillMaxWidth()
            .height(metrics.cluePanelHeight.dp)
            .testTag("wordlyCluePanel")
            .then(
                if (state.summary == null) {
                    Modifier.clickable(onClick = onToggleExpanded)
                } else {
                    Modifier
                },
            )
    Surface(
        modifier = panelModifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FFE8).copy(alpha = 0.96f),
        border = BorderStroke(2.dp, Color(0xFFB7D477)),
        shadowElevation = 4.dp,
    ) {
        if (state.summary != null) {
            WordlySummaryPanel(
                state = state,
                metrics = metrics,
                actions = actions,
            )
        } else {
            WordlyActiveClueContent(
                state = state,
                metrics = metrics,
                onUseHint = actions.onUseHint,
            )
        }
    }
}

@Composable
private fun WordlyActiveClueContent(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
    onUseHint: () -> Unit,
) {
    Row(
        modifier = Modifier.padding((12f * metrics.textScale).dp),
        horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size((metrics.cluePanelHeight - 28f).coerceIn(52f, 86f).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF2EAF32)),
            contentAlignment = Alignment.Center,
        ) {
            Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = (26f * metrics.textScale).sp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2f * metrics.textScale).dp),
        ) {
            Text("Clue", fontWeight = FontWeight.Black, fontSize = (17f * metrics.textScale).sp, maxLines = 1)
            AutoSizeText(
                text = state.openHintText ?: state.clueText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((metrics.cluePanelHeight * 0.48f).dp),
                spec =
                    AutoSizeTextSpec(
                        maxFontSizeSp = 14f * metrics.textScale,
                        minFontSizeSp = 8.5f,
                        maxLines = if (metrics.cluePanelHeight > 106f) 3 else 2,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Start,
                        contentAlignment = Alignment.TopStart,
                    ),
            )
            state.message?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.testTag("wordlyMessage"),
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * metrics.textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Button(
            onClick = onUseHint,
            enabled = state.canUseHint,
            modifier =
                Modifier
                    .width((104f * metrics.textScale).coerceAtLeast(82f).dp)
                    .height((metrics.cluePanelHeight - 30f).coerceIn(46f, 76f).dp),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Use Hint", fontWeight = FontWeight.Bold, fontSize = (12f * metrics.textScale).sp, maxLines = 1)
                Text("${state.hintsRemaining}", fontWeight = FontWeight.Black, fontSize = (18f * metrics.textScale).sp)
            }
        }
    }
}

@Composable
private fun WordlyExpandedCluePanel(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
    onUseHint: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(metrics.expandedCluePanelHeight.dp)
                .testTag("wordlyExpandedCluePanel")
                .clickable(onClick = onCollapse),
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF9FFE8).copy(alpha = 0.98f),
        border = BorderStroke(2.dp, Color(0xFF7BBF58)),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding((14f * metrics.textScale).dp),
            verticalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size((60f * metrics.textScale).coerceIn(48f, 64f).dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF2EAF32)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = (28f * metrics.textScale).sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Clue", fontWeight = FontWeight.Black, fontSize = (20f * metrics.textScale).sp, maxLines = 1)
                }
                Button(
                    onClick = onUseHint,
                    enabled = state.canUseHint,
                    modifier =
                        Modifier
                            .width((112f * metrics.textScale).coerceAtLeast(88f).dp)
                            .height((52f * metrics.textScale).coerceAtLeast(44f).dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Use Hint", fontWeight = FontWeight.Bold, fontSize = (12f * metrics.textScale).sp, maxLines = 1)
                        Text("${state.hintsRemaining}", fontWeight = FontWeight.Black, fontSize = (18f * metrics.textScale).sp)
                    }
                }
            }
            AutoSizeText(
                text = state.clueText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                spec =
                    AutoSizeTextSpec(
                        maxFontSizeSp = 18f * metrics.textScale,
                        minFontSizeSp = 10f,
                        maxLines = 6,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Start,
                        contentAlignment = Alignment.TopStart,
                    ),
            )
            state.openHintText?.let { hint ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFFFF2C0)) {
                    AutoSizeText(
                        text = hint,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height((42f * metrics.textScale).coerceAtLeast(34f).dp)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        spec =
                            AutoSizeTextSpec(
                                maxFontSizeSp = 13f * metrics.textScale,
                                minFontSizeSp = 9f,
                                maxLines = 2,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                contentAlignment = Alignment.TopStart,
                            ),
                    )
                }
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.testTag("wordlyExpandedMessage"),
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * metrics.textScale).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun WordlySummaryPanel(
    state: WordlyUiState,
    metrics: WordlyLayoutMetrics,
    actions: WordlyGameActions,
) {
    val summary = state.summary ?: return
    Row(
        modifier = Modifier.padding((12f * metrics.textScale).dp),
        horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy((2f * metrics.textScale).dp),
        ) {
            Text(summary.title, fontSize = (16f * metrics.textScale).sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(summary.body, fontSize = (12f * metrics.textScale).sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            summary.example?.let {
                Text(it, fontSize = (11f * metrics.textScale).sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            state.sharePattern?.let { pattern ->
                Text(
                    text = pattern,
                    modifier = Modifier.testTag("wordlySharePreview"),
                    fontSize = (9f * metrics.textScale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier =
                Modifier
                    .width((100f * metrics.textScale).coerceAtLeast(78f).dp)
                    .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((6f * metrics.textScale).dp),
        ) {
            state.shareCard?.let { shareCard ->
                Button(
                    onClick = { actions.shareActions.share(shareCard) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("wordlyShareButton"),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text("Share", fontWeight = FontWeight.Bold, fontSize = (13f * metrics.textScale).sp, maxLines = 1)
                }
            }
            Button(
                onClick = actions.onReturnHome,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("wordlyDoneButton"),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = (13f * metrics.textScale).sp, maxLines = 1)
            }
        }
    }
}

internal object WordlyUiMapper {
    fun map(
        puzzle: WordlyPuzzle,
        gameState: WordlySaveState,
        settings: QuestSettings,
        homeState: DailyHomeUiState,
        transientMessage: String?,
    ): WordlyUiState {
        val shareCard =
            if (gameState.isTerminal) {
                WordlyGameEngine.shareCard(
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
        val safeSharePattern =
            safeShareCard
                ?.visibleResultPattern
        val openHints = puzzle.hints.filter { it.order in gameState.revealedHintOrders }.sortedBy { it.order }

        val prompt = "Find the hidden 5-letter word."
        val message = transientMessage ?: terminalMessage(gameState)

        return WordlyUiState(
            attemptsLabel = "${gameState.attempts.size}/${WordlyGameEngine.MAX_ATTEMPTS}",
            streakLabel = "Streak: ${wordlyStreak(homeState)}",
            starLabel = "Stars ${starScore(gameState)}/20",
            prompt = prompt,
            statusPrompt = message ?: prompt,
            rows = WordlyGameEngine.boardRows(puzzle, gameState),
            keyboardRows = keyboardRows(WordlyGameEngine.keyboardStates(puzzle, gameState)),
            clueText = puzzle.definition,
            openHintText = openHints.lastOrNull()?.text,
            hintsRemaining = puzzle.hints.size - openHints.size,
            canUseHint = !gameState.isTerminal && openHints.size < puzzle.hints.size,
            message = message,
            summary = if (gameState.isTerminal) WordlyGameEngine.learningSummary(puzzle, gameState) else null,
            sharePattern = safeSharePattern,
            shareCard = safeShareCard,
            isTerminal = gameState.isTerminal,
            largeText = settings.largePuzzleText,
        )
    }

    private fun keyboardRows(states: Map<Char, TileState>): List<List<WordlyKeyUiState>> =
        listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
            .map { row ->
                row.map { letter ->
                    WordlyKeyUiState(letter = letter, state = states[letter] ?: TileState.EMPTY)
                }
            }

    private fun wordlyStreak(homeState: DailyHomeUiState): Int =
        homeState.cards
            .firstOrNull { it.category == PuzzleCategory.WORDLY }
            ?.categoryStreak ?: 0

    private fun starScore(gameState: WordlySaveState): Int =
        (20 - gameState.attempts.size * 2 - gameState.revealedHintOrders.size * 3).coerceIn(0, 20)

    private fun terminalMessage(gameState: WordlySaveState): String? =
        when {
            gameState.isCompleted -> WordlyMessage.SOLVED.userText
            gameState.isFailed -> WordlyMessage.OUT_OF_ATTEMPTS.userText
            else -> null
        }
}

internal data class WordlyRouteData(
    val puzzle: WordlyPuzzle,
    val settings: QuestSettings,
    val homeState: DailyHomeUiState,
)

internal data class WordlyRouteDependencies(
    val progressStore: ProgressStore,
    val wordlyProgressStore: WordlyProgressStore,
    val dayIndex: Int,
    val todaysPuzzleIds: Set<String>,
)

internal data class WordlyGameActions(
    val onBack: () -> Unit,
    val onUseHint: () -> Unit,
    val onLetter: (Char) -> Unit,
    val onDelete: () -> Unit,
    val onSubmit: () -> Unit,
    val onReturnHome: () -> Unit,
    val shareActions: ShareActions,
)

internal data class WordlyUiState(
    val attemptsLabel: String,
    val streakLabel: String,
    val starLabel: String,
    val prompt: String,
    val statusPrompt: String,
    val rows: List<WordlyBoardRow>,
    val keyboardRows: List<List<WordlyKeyUiState>>,
    val clueText: String,
    val openHintText: String?,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val message: String?,
    val summary: WordlyLearningSummary?,
    val sharePattern: String?,
    val shareCard: ShareCardModel?,
    val isTerminal: Boolean,
    val largeText: Boolean,
)

internal data class WordlyKeyUiState(
    val letter: Char,
    val state: TileState,
)

private data class WordlyTileColors(
    val container: Color,
    val border: Color,
    val content: Color,
)

private fun tileColors(state: TileState): WordlyTileColors =
    when (state) {
        TileState.CORRECT -> WordlyTileColors(Color(0xFF37B534), Color(0xFF147D24), Color.White)
        TileState.PRESENT -> WordlyTileColors(Color(0xFFFFC933), Color(0xFFC88700), Color.White)
        TileState.ABSENT -> WordlyTileColors(Color(0xFF354457), Color(0xFF172432), Color.White)
        TileState.EMPTY -> WordlyTileColors(Color(0xFFEFFFDC), Color(0xFF8CC06F), Color(0xFF173444))
    }
