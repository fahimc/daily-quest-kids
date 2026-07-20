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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.puzzle.engine.ShareSafety
import com.dailyquestkids.puzzle.engine.SpellingBCompletionEvent
import com.dailyquestkids.puzzle.engine.SpellingBFoundWord
import com.dailyquestkids.puzzle.engine.SpellingBGameEngine
import com.dailyquestkids.puzzle.engine.SpellingBMessage
import com.dailyquestkids.puzzle.engine.SpellingBMoveResult
import com.dailyquestkids.puzzle.engine.SpellingBSaveState
import kotlinx.coroutines.launch

@Composable
internal fun SpellingBRoute(
    data: SpellingBRouteData,
    dependencies: SpellingBRouteDependencies,
    onBack: () -> Unit,
    onReturnHome: () -> Unit,
) {
    val savedState by dependencies.spellingProgressStore
        .stateFor(data.puzzle.id)
        .collectAsStateWithLifecycle(initialValue = null)
    val gameState = savedState ?: SpellingBGameEngine.initial(data.puzzle)
    val scope = rememberCoroutineScope()
    var transientMessage by remember(data.puzzle.id) { mutableStateOf<String?>(null) }

    fun persist(result: SpellingBMoveResult) {
        transientMessage = result.message?.userText
        scope.launch {
            dependencies.spellingProgressStore.save(result.state)
            handleSpellingCompletion(
                event = result.completionEvent,
                resultState = result.state,
                dependencies = dependencies,
            )
        }
    }

    SpellingBGameScreen(
        state =
            SpellingBUiMapper.map(
                puzzle = data.puzzle,
                gameState = gameState,
                settings = data.settings,
                homeState = data.homeState,
                transientMessage = transientMessage,
            ),
        actions =
            SpellingBGameActions(
                onBack = onBack,
                onUseHint = {
                    persist(SpellingBGameEngine.revealHint(data.puzzle, gameState))
                },
                onLetter = { letter ->
                    persist(SpellingBGameEngine.appendLetter(data.puzzle, gameState, letter))
                },
                onDelete = {
                    scope.launch {
                        dependencies.spellingProgressStore.save(SpellingBGameEngine.deleteLetter(data.puzzle, gameState))
                    }
                },
                onClear = {
                    scope.launch {
                        dependencies.spellingProgressStore.save(SpellingBGameEngine.clearInput(data.puzzle, gameState))
                    }
                },
                onShuffle = {
                    scope.launch {
                        dependencies.spellingProgressStore.save(SpellingBGameEngine.shuffle(data.puzzle, gameState))
                    }
                },
                onSubmit = {
                    persist(SpellingBGameEngine.submit(data.puzzle, gameState))
                },
                onReturnHome = onReturnHome,
            ),
    )
}

private suspend fun handleSpellingCompletion(
    event: SpellingBCompletionEvent?,
    resultState: SpellingBSaveState,
    dependencies: SpellingBRouteDependencies,
) {
    if (event == null) return

    dependencies.progressStore.markCompleted(
        puzzleId = event.puzzleId,
        dayIndex = dependencies.dayIndex,
        todaysPuzzleIds = dependencies.todaysPuzzleIds,
    )
    dependencies.spellingProgressStore.save(SpellingBGameEngine.acknowledgeCompletion(resultState))
}

@Composable
internal fun SpellingBGameScreen(
    state: SpellingBUiState,
    actions: SpellingBGameActions,
) {
    QuestSceneFrame(testTag = "spellingScreen") { screenMetrics ->
        val metrics =
            SpellingBLayoutCalculator.calculate(
                widthDp = screenMetrics.widthDp,
                heightDp = screenMetrics.heightDp,
            )
        var expandedFoundList by remember(state.foundRows, state.hintTexts, state.isCompleted) {
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
            SpellingTopBar(state = state, metrics = metrics, onBack = actions.onBack)
            SpellingScorePanel(state = state, metrics = metrics)
            SpellingCurrentWord(state = state, metrics = metrics)
            SpellingHoneycomb(state = state, metrics = metrics, actions = actions)
            SpellingControls(state = state, metrics = metrics, actions = actions)
            Spacer(Modifier.weight(1f))
            SpellingFoundPanel(
                state = state,
                metrics = metrics,
                onUseHint = actions.onUseHint,
                onReturnHome = actions.onReturnHome,
                onExpand = { expandedFoundList = true },
            )
        }

        if (expandedFoundList) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("spellingFoundDismissLayer")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { expandedFoundList = false },
                        ),
            )
            SpellingExpandedFoundPanel(
                state = state,
                metrics = metrics,
                onUseHint = actions.onUseHint,
                onCollapse = { expandedFoundList = false },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .width(metrics.contentWidth.dp)
                        .padding(bottom = metrics.verticalPadding.dp),
            )
        }
    }
}

internal object SpellingBLayoutCalculator {
    fun calculate(
        widthDp: Float,
        heightDp: Float,
    ): SpellingBLayoutMetrics {
        val horizontalPadding = if (widthDp < 360f) 10f else 14f
        val contentWidth = (widthDp - horizontalPadding * 2f).coerceAtMost(720f)
        val verticalPadding = (heightDp * 0.01f).coerceIn(5f, 10f)
        val sectionGap = (heightDp * 0.006f).coerceIn(3.5f, 7f)
        val topBarHeight = (heightDp * 0.072f).coerceIn(48f, 66f)
        val scorePanelHeight = (heightDp * 0.075f).coerceIn(50f, 70f)
        val currentWordHeight = (heightDp * 0.058f).coerceIn(38f, 54f)
        val controlsHeight = (heightDp * 0.058f).coerceIn(40f, 54f)
        val bottomPanelHeight = (heightDp * 0.135f).coerceIn(82f, 122f)
        val fixedHeight =
            verticalPadding * 2f +
                topBarHeight +
                scorePanelHeight +
                currentWordHeight +
                controlsHeight +
                bottomPanelHeight +
                sectionGap * 5f
        val hiveBudget = (heightDp - fixedHeight).coerceAtLeast(146f)
        val hiveSize = minOf(contentWidth * 0.96f, hiveBudget, 380f).coerceAtLeast(146f)
        val tileSize = (hiveSize / 3.45f).coerceIn(42f, 108f)
        val hiveGap = (tileSize * 0.15f).coerceIn(6f, 14f)
        val textScale = (tileSize / 72f).coerceIn(0.62f, 1.08f)
        val expandedPanelHeight = (heightDp * 0.46f).coerceIn(bottomPanelHeight + 84f, heightDp - verticalPadding * 2f)

        return SpellingBLayoutMetrics(
            contentWidth = contentWidth,
            verticalPadding = verticalPadding,
            sectionGap = sectionGap,
            topBarHeight = topBarHeight,
            scorePanelHeight = scorePanelHeight,
            currentWordHeight = currentWordHeight,
            hiveSize = hiveSize,
            hiveTileSize = tileSize,
            hiveGap = hiveGap,
            controlsHeight = controlsHeight,
            bottomPanelHeight = bottomPanelHeight,
            expandedPanelHeight = expandedPanelHeight,
            textScale = textScale,
        )
    }
}

internal data class SpellingBLayoutMetrics(
    val contentWidth: Float,
    val verticalPadding: Float,
    val sectionGap: Float,
    val topBarHeight: Float,
    val scorePanelHeight: Float,
    val currentWordHeight: Float,
    val hiveSize: Float,
    val hiveTileSize: Float,
    val hiveGap: Float,
    val controlsHeight: Float,
    val bottomPanelHeight: Float,
    val expandedPanelHeight: Float,
    val textScale: Float,
) {
    val totalHeight: Float =
        verticalPadding * 2f +
            topBarHeight +
            scorePanelHeight +
            currentWordHeight +
            hiveSize +
            controlsHeight +
            bottomPanelHeight +
            sectionGap * 5f
}

@Composable
private fun SpellingTopBar(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.topBarHeight.dp)
                .testTag("spellingTopBar"),
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
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            shape = RoundedCornerShape((18f * metrics.textScale).dp),
            color = Color(0xFFFFF8D7).copy(alpha = 0.97f),
            border = BorderStroke(2.dp, Color(0xFFD29300)),
        ) {
            Row(
                modifier = Modifier.padding((7f * metrics.textScale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                QuestIconTile(
                    category = PuzzleCategory.SPELLING_B,
                    modifier = Modifier.size((metrics.topBarHeight - 18f).coerceAtLeast(34f).dp),
                    scale = metrics.textScale,
                )
                QuestAutoText(
                    text = "Spelling B",
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 25f * metrics.textScale,
                            minFontSizeSp = 12f,
                            maxLines = 1,
                            color = Color(0xFF522900),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                SpellingTinyBadge(
                    title = "Score",
                    value = state.scoreLabel,
                    metrics = metrics,
                    modifier = Modifier.testTag("spellingScoreBadge"),
                )
                SpellingTinyBadge(
                    title = "Hints",
                    value = state.hintLabel,
                    metrics = metrics,
                    modifier = Modifier.testTag("spellingHintsBadge"),
                )
            }
        }
    }
}

@Composable
private fun SpellingTinyBadge(
    title: String,
    value: String,
    metrics: SpellingBLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .width((58f * metrics.textScale).coerceIn(46f, 66f).dp)
                .fillMaxHeight(),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0C96B)),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            QuestAutoText(
                text = title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.45f),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 8f * metrics.textScale,
                        minFontSizeSp = 4f,
                        maxLines = 1,
                        color = Color(0xFF5A3A00),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
            QuestAutoText(
                text = value,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(0.55f),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 12f * metrics.textScale,
                        minFontSizeSp = 5f,
                        maxLines = 1,
                        color = Color(0xFF1C2F3A),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
        }
    }
}

@Composable
private fun SpellingScorePanel(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.scorePanelHeight.dp)
                .testTag("spellingScorePanel"),
        color = Color(0xFF083C6D).copy(alpha = 0.96f),
        border = BorderStroke(2.dp, Color(0xFF1F7DC9)),
        shape = RoundedCornerShape((20f * metrics.textScale).dp),
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = (12f * metrics.textScale).dp, vertical = (7f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((4f * metrics.textScale).dp)) {
                QuestAutoText(
                    text = state.achievementLabel,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((20f * metrics.textScale).coerceAtLeast(15f).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 15f * metrics.textScale,
                            minFontSizeSp = 7f,
                            maxLines = 1,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((12f * metrics.textScale).coerceAtLeast(8f).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF06294E)),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(state.progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFC933)),
                    )
                }
            }
            SpellingPanelStat(text = state.wordCountLabel, metrics = metrics)
            SpellingPanelStat(text = state.streakLabel, metrics = metrics)
        }
    }
}

@Composable
private fun SpellingPanelStat(
    text: String,
    metrics: SpellingBLayoutMetrics,
) {
    Surface(
        modifier =
            Modifier
                .width((88f * metrics.textScale).coerceIn(68f, 98f).dp)
                .fillMaxHeight(),
        color = Color.White.copy(alpha = 0.95f),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
    ) {
        QuestAutoText(
            text = text,
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 3.dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 12f * metrics.textScale,
                    minFontSizeSp = 5.5f,
                    maxLines = 2,
                    color = Color(0xFF173444),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    softWrap = true,
                ),
        )
    }
}

@Composable
private fun SpellingCurrentWord(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
) {
    val text = state.currentWord.ifBlank { state.prompt }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.currentWordHeight.dp)
                .testTag("spellingCurrentWord")
                .semantics(mergeDescendants = true) {},
        color = if (state.message == null) Color(0xFFFFFDE8).copy(alpha = 0.95f) else Color(0xFFFFEAA7).copy(alpha = 0.98f),
        border = BorderStroke(1.5.dp, Color(0xFFD8B039)),
        shape = RoundedCornerShape((18f * metrics.textScale).dp),
    ) {
        QuestAutoText(
            text = state.message ?: text,
            modifier = Modifier.fillMaxSize().padding(horizontal = (12f * metrics.textScale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = if (state.currentWord.isBlank()) 14f * metrics.textScale else 23f * metrics.textScale,
                    minFontSizeSp = 6f,
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
private fun SpellingHoneycomb(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    actions: SpellingBGameActions,
) {
    val outer = state.outerLetters.take(6).let { letters -> letters + List(6 - letters.size) { ' ' } }
    Column(
        modifier =
            Modifier
                .size(metrics.hiveSize.dp)
                .testTag("spellingHoneycomb"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.hiveGap.dp)) {
            SpellingLetterButton(outer[0], false, state, metrics, actions)
            SpellingLetterButton(outer[1], false, state, metrics, actions)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.hiveGap.dp)) {
            SpellingLetterButton(outer[2], false, state, metrics, actions)
            SpellingLetterButton(state.centreLetter, true, state, metrics, actions)
            SpellingLetterButton(outer[3], false, state, metrics, actions)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(metrics.hiveGap.dp)) {
            SpellingLetterButton(outer[4], false, state, metrics, actions)
            SpellingLetterButton(outer[5], false, state, metrics, actions)
        }
    }
}

@Composable
private fun SpellingLetterButton(
    letter: Char,
    centre: Boolean,
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    actions: SpellingBGameActions,
) {
    val enabled = !state.isCompleted && letter.isLetter()
    Button(
        onClick = { actions.onLetter(letter) },
        enabled = enabled,
        modifier =
            Modifier
                .size(metrics.hiveTileSize.dp)
                .testTag(if (centre) "spellingCentreLetter" else "spellingLetter-${letter.uppercaseChar()}"),
        shape = HoneycombShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (centre) Color(0xFFFFC933) else Color(0xFFFFF6C5),
                contentColor = Color(0xFF382300),
                disabledContainerColor = if (centre) Color(0xFFFFC933).copy(alpha = 0.72f) else Color(0xFFFFF6C5).copy(alpha = 0.72f),
                disabledContentColor = Color(0xFF382300).copy(alpha = 0.72f),
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(
            text = if (letter.isLetter()) letter.uppercaseChar().toString() else "",
            fontWeight = FontWeight.Black,
            fontSize = ((if (state.largeText) 31f else 28f) * metrics.textScale).sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpellingControls(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    actions: SpellingBGameActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.controlsHeight.dp),
        horizontalArrangement = Arrangement.spacedBy((7f * metrics.textScale).dp),
    ) {
        SpellingControlButton(
            label = "Shuffle",
            enabled = !state.isCompleted,
            metrics = metrics,
            onClick = actions.onShuffle,
            modifier = Modifier.weight(1f).testTag("spellingShuffleButton"),
        )
        SpellingControlButton(
            label = "Delete",
            enabled = !state.isCompleted,
            metrics = metrics,
            onClick = actions.onDelete,
            modifier = Modifier.weight(1f).testTag("spellingDeleteButton"),
        )
        SpellingControlButton(
            label = "Clear",
            enabled = !state.isCompleted,
            metrics = metrics,
            onClick = actions.onClear,
            modifier = Modifier.weight(1f).testTag("spellingClearButton"),
        )
        SpellingControlButton(
            label = "Submit",
            enabled = !state.isCompleted,
            metrics = metrics,
            onClick = actions.onSubmit,
            modifier = Modifier.weight(1.15f).testTag("spellingSubmitButton"),
        )
    }
}

@Composable
private fun SpellingControlButton(
    label: String,
    enabled: Boolean,
    metrics: SpellingBLayoutMetrics,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape((13f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (label == "Submit") Color(0xFF2196F3) else Color(0xFF2D8C4A),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF7FAE82),
                disabledContentColor = Color.White.copy(alpha = 0.75f),
            ),
    ) {
        QuestAutoText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 13f * metrics.textScale,
                    minFontSizeSp = 5.5f,
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
private fun SpellingFoundPanel(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    onUseHint: () -> Unit,
    onReturnHome: () -> Unit,
    onExpand: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(metrics.bottomPanelHeight.dp)
                .testTag("spellingFoundPanel")
                .clickable(onClick = onExpand),
        color = Color(0xFFFFFBEA).copy(alpha = 0.97f),
        border = BorderStroke(2.dp, Color(0xFFD6BC55)),
        shape = RoundedCornerShape((24f * metrics.textScale).dp),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding((12f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * metrics.textScale).dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size((metrics.bottomPanelHeight - 28f).coerceIn(50f, 84f).dp)
                        .clip(RoundedCornerShape((18f * metrics.textScale).dp))
                        .background(Color(0xFFFFC933)),
                contentAlignment = Alignment.Center,
            ) {
                Text("B", color = Color(0xFF6E3900), fontWeight = FontWeight.Black, fontSize = (28f * metrics.textScale).sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy((3f * metrics.textScale).dp)) {
                QuestAutoText(
                    text = if (state.isCompleted) "Hive Complete" else "Found Words",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((22f * metrics.textScale).coerceAtLeast(16f).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 17f * metrics.textScale,
                            minFontSizeSp = 7f,
                            maxLines = 1,
                            color = Color(0xFF173444),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                QuestAutoText(
                    text = state.foundSummary,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((metrics.bottomPanelHeight * 0.44f).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 12f * metrics.textScale,
                            minFontSizeSp = 6f,
                            maxLines = 2,
                            color = Color(0xFF173444),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            contentAlignment = Alignment.TopStart,
                        ),
                )
            }
            if (state.isCompleted) {
                SpellingDoneButton(metrics = metrics, onReturnHome = onReturnHome)
            } else {
                SpellingHintButton(state = state, metrics = metrics, onUseHint = onUseHint)
            }
        }
    }
}

@Composable
private fun SpellingHintButton(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    onUseHint: () -> Unit,
) {
    Button(
        onClick = onUseHint,
        enabled = state.canUseHint,
        modifier =
            Modifier
                .width((104f * metrics.textScale).coerceAtLeast(82f).dp)
                .height((metrics.bottomPanelHeight - 28f).coerceIn(46f, 76f).dp),
        shape = RoundedCornerShape((18f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D8C4A), contentColor = Color.White),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            QuestAutoText(
                text = "Use Hint",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((19f * metrics.textScale).coerceAtLeast(14f).dp),
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
            Text("${state.hintsRemaining}", fontWeight = FontWeight.Black, fontSize = (21f * metrics.textScale).sp)
        }
    }
}

@Composable
private fun SpellingDoneButton(
    metrics: SpellingBLayoutMetrics,
    onReturnHome: () -> Unit,
) {
    Button(
        onClick = onReturnHome,
        modifier =
            Modifier
                .width((104f * metrics.textScale).coerceAtLeast(82f).dp)
                .height((metrics.bottomPanelHeight - 28f).coerceIn(46f, 76f).dp),
        shape = RoundedCornerShape((18f * metrics.textScale).dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.White),
    ) {
        Text("Done", fontWeight = FontWeight.Black, fontSize = (15f * metrics.textScale).sp)
    }
}

@Composable
private fun SpellingExpandedFoundPanel(
    state: SpellingBUiState,
    metrics: SpellingBLayoutMetrics,
    onUseHint: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .height(metrics.expandedPanelHeight.dp)
                .testTag("spellingExpandedFoundPanel")
                .clickable(onClick = onCollapse),
        color = Color(0xFFFFFBEA).copy(alpha = 0.99f),
        border = BorderStroke(2.dp, Color(0xFFD6BC55)),
        shape = RoundedCornerShape((24f * metrics.textScale).dp),
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding((14f * metrics.textScale).dp),
            verticalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                QuestAutoText(
                    text = "Found Words",
                    modifier =
                        Modifier
                            .weight(1f)
                            .height((28f * metrics.textScale).coerceAtLeast(20f).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 19f * metrics.textScale,
                            minFontSizeSp = 8f,
                            maxLines = 1,
                            color = Color(0xFF173444),
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                            contentAlignment = Alignment.CenterStart,
                        ),
                )
                if (!state.isCompleted) {
                    SpellingHintButton(state = state, metrics = metrics, onUseHint = onUseHint)
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
            ) {
                if (state.foundRows.isEmpty()) {
                    Text(
                        text = "Build words with the middle letter. Found words and definitions will appear here.",
                        color = Color(0xFF173444),
                        fontSize = (13f * metrics.textScale).sp,
                    )
                }
                state.foundRows.forEach { row ->
                    SpellingFoundWordRow(row = row, metrics = metrics)
                }
                if (state.hintTexts.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFE8A8),
                        shape = RoundedCornerShape((14f * metrics.textScale).dp),
                    ) {
                        Column(
                            modifier = Modifier.padding((10f * metrics.textScale).dp),
                            verticalArrangement = Arrangement.spacedBy((5f * metrics.textScale).dp),
                        ) {
                            Text("Hints", color = Color(0xFF533300), fontWeight = FontWeight.Black, fontSize = (13f * metrics.textScale).sp)
                            state.hintTexts.forEach { hint ->
                                Text(hint, color = Color(0xFF173444), fontSize = (12f * metrics.textScale).sp)
                            }
                        }
                    }
                }
                state.sharePattern?.let { share ->
                    Text(
                        text = share,
                        modifier = Modifier.testTag("spellingSharePreview"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (10f * metrics.textScale).sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpellingFoundWordRow(
    row: SpellingBFoundWordUiState,
    metrics: SpellingBLayoutMetrics,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.78f),
        shape = RoundedCornerShape((14f * metrics.textScale).dp),
        border = BorderStroke(1.dp, if (row.isPangram) Color(0xFFFFC933) else Color(0xFFE3D896)),
    ) {
        Row(
            modifier = Modifier.padding((9f * metrics.textScale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.textScale).dp),
        ) {
            Surface(
                modifier = Modifier.size((34f * metrics.textScale).coerceAtLeast(28f).dp),
                color = if (row.isPangram) Color(0xFFFFC933) else Color(0xFF2D8C4A),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (row.isPangram) "*" else "+", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(row.word, color = Color(0xFF173444), fontWeight = FontWeight.Black, fontSize = (14f * metrics.textScale).sp)
                Text(row.definition, color = Color(0xFF173444), fontSize = (11f * metrics.textScale).sp)
            }
        }
    }
}

internal object SpellingBUiMapper {
    fun map(
        puzzle: SpellingBeePuzzle,
        gameState: SpellingBSaveState,
        settings: QuestSettings,
        homeState: DailyHomeUiState,
        transientMessage: String?,
    ): SpellingBUiState {
        val score = SpellingBGameEngine.score(puzzle, gameState)
        val totalScore = SpellingBGameEngine.totalScore(puzzle).coerceAtLeast(1)
        val achievement = SpellingBGameEngine.achievement(puzzle, gameState)
        val foundRows = SpellingBGameEngine.foundWordRows(puzzle, gameState).map { it.toUiState() }
        val hints = SpellingBGameEngine.revealedHintTexts(puzzle, gameState)
        val shareCard =
            if (gameState.isCompleted) {
                SpellingBGameEngine.shareCard(
                    puzzle = puzzle,
                    state = gameState,
                    utcDate = homeState.date.toString(),
                    currentStreak = homeState.currentDailyFiveStreak,
                    bestStreak = homeState.bestDailyFiveStreak,
                )
            } else {
                null
            }
        val safeSharePattern =
            shareCard
                ?.takeUnless { ShareSafety.leaksForbiddenPayload(it) }
                ?.visibleResultPattern
        val message = transientMessage ?: if (gameState.isCompleted) SpellingBMessage.PUZZLE_COMPLETE.userText else null
        val prompt = "Use the middle letter in every word."

        return SpellingBUiState(
            scoreLabel = "$score/$totalScore",
            hintLabel = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0).toString(),
            streakLabel = "Streak\n${spellingStreak(homeState)}",
            achievementLabel = "${achievement.title}  $score/$totalScore",
            progress = achievement.progress,
            wordCountLabel = "Words\n${foundRows.size}/${puzzle.targetWords.size}",
            prompt = prompt,
            currentWord = gameState.currentInput.uppercase(),
            centreLetter = puzzle.centreLetter.uppercaseChar(),
            outerLetters = SpellingBGameEngine.outerLetters(puzzle, gameState).map { it.uppercaseChar() },
            foundRows = foundRows,
            foundSummary = foundSummary(foundRows, hints, gameState.isCompleted),
            hintTexts = hints,
            hintsRemaining = (puzzle.hints.size - gameState.revealedHintOrders.size).coerceAtLeast(0),
            canUseHint = !gameState.isCompleted && gameState.revealedHintOrders.size < puzzle.hints.size,
            message = message,
            isCompleted = gameState.isCompleted,
            sharePattern = safeSharePattern,
            largeText = settings.largePuzzleText,
        )
    }

    private fun spellingStreak(homeState: DailyHomeUiState): Int =
        homeState.cards
            .firstOrNull { it.category == PuzzleCategory.SPELLING_B }
            ?.categoryStreak ?: 0

    private fun foundSummary(
        foundRows: List<SpellingBFoundWordUiState>,
        hints: List<String>,
        completed: Boolean,
    ): String =
        when {
            completed -> "All words are found. Tap to review definitions and the share-safe result."
            foundRows.isNotEmpty() -> {
                val latest = foundRows.last()
                "${latest.word}: ${latest.definition}"
            }
            hints.isNotEmpty() -> hints.last()
            else -> "Tap hive letters to build words. Tap this panel to review found words."
        }

    private fun SpellingBFoundWord.toUiState(): SpellingBFoundWordUiState =
        SpellingBFoundWordUiState(
            word = word.uppercase(),
            definition = definition,
            isPangram = isPangram,
        )
}

internal data class SpellingBRouteData(
    val puzzle: SpellingBeePuzzle,
    val settings: QuestSettings,
    val homeState: DailyHomeUiState,
)

internal data class SpellingBRouteDependencies(
    val progressStore: ProgressStore,
    val spellingProgressStore: SpellingProgressStore,
    val dayIndex: Int,
    val todaysPuzzleIds: Set<String>,
)

internal data class SpellingBGameActions(
    val onBack: () -> Unit,
    val onUseHint: () -> Unit,
    val onLetter: (Char) -> Unit,
    val onDelete: () -> Unit,
    val onClear: () -> Unit,
    val onShuffle: () -> Unit,
    val onSubmit: () -> Unit,
    val onReturnHome: () -> Unit,
)

internal data class SpellingBUiState(
    val scoreLabel: String,
    val hintLabel: String,
    val streakLabel: String,
    val achievementLabel: String,
    val progress: Float,
    val wordCountLabel: String,
    val prompt: String,
    val currentWord: String,
    val centreLetter: Char,
    val outerLetters: List<Char>,
    val foundRows: List<SpellingBFoundWordUiState>,
    val foundSummary: String,
    val hintTexts: List<String>,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val message: String?,
    val isCompleted: Boolean,
    val sharePattern: String?,
    val largeText: Boolean,
)

internal data class SpellingBFoundWordUiState(
    val word: String,
    val definition: String,
    val isPangram: Boolean,
)

private val HoneycombShape =
    GenericShape { size, _ ->
        val width = size.width
        val height = size.height
        moveTo(width * 0.5f, 0f)
        lineTo(width * 0.93f, height * 0.25f)
        lineTo(width * 0.93f, height * 0.75f)
        lineTo(width * 0.5f, height)
        lineTo(width * 0.07f, height * 0.75f)
        lineTo(width * 0.07f, height * 0.25f)
        close()
    }
