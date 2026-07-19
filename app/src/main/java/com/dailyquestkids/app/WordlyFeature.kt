package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailyquestkids.core.model.PuzzleCategory
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
            )
        WordlyOutcome.FAILURE -> dependencies.progressStore.markFailed(event.puzzleId)
    }

    dependencies.wordlyProgressStore.save(WordlyGameEngine.acknowledgeCompletion(resultState))
}

@OptIn(ExperimentalLayoutApi::class)
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
        val horizontalPadding = if (maxWidth < 360.dp) 10.dp else 16.dp
        val boardWidth = (maxWidth - horizontalPadding * 2).coerceAtMost(390.dp)
        val tileGap = if (maxWidth < 360.dp) 4.dp else 6.dp
        val tileSize = ((boardWidth - tileGap * 4) / 5).coerceIn(42.dp, 70.dp)
        val keyHeight = if (maxWidth < 360.dp) 40.dp else 48.dp

        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { WordlyTopBar(state = state, onBack = actions.onBack) }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(onClick = {}, label = { Text(state.streakLabel) })
                    AssistChip(onClick = {}, label = { Text(state.prompt) })
                    AssistChip(onClick = {}, label = { Text(state.starLabel) })
                }
            }
            item {
                WordlyBoard(
                    rows = state.rows,
                    tileSize = tileSize,
                    gap = tileGap,
                    largeText = state.largeText,
                )
            }
            item {
                WordlyKeyboard(
                    rows = state.keyboardRows,
                    keyHeight = keyHeight,
                    actions = actions,
                    enabled = !state.isTerminal,
                )
            }
            item {
                WordlyCluePanel(
                    state = state,
                    onUseHint = actions.onUseHint,
                    onReturnHome = actions.onReturnHome,
                )
            }
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

@Composable
private fun WordlyTopBar(
    state: WordlyUiState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextButton(
            onClick = onBack,
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f)),
        ) {
            Text("<", fontWeight = FontWeight.Black, fontSize = 24.sp)
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF4FFE7).copy(alpha = 0.96f),
            border = BorderStroke(2.dp, Color(0xFF67A95B)),
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF41B939)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Wordly", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Letter Garden", style = MaterialTheme.typography.bodySmall)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFB4CE9F)),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Attempts", style = MaterialTheme.typography.labelSmall)
                        Text(state.attemptsLabel, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordlyBoard(
    rows: List<WordlyBoardRow>,
    tileSize: Dp,
    gap: Dp,
    largeText: Boolean,
) {
    Column(
        modifier = Modifier.testTag("wordlyBoard"),
        verticalArrangement = Arrangement.spacedBy(gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.letters.forEachIndexed { columnIndex, letter ->
                    WordlyTile(
                        letter = letter,
                        state = row.states[columnIndex],
                        size = tileSize,
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
    size: Dp,
    largeText: Boolean,
    tag: String,
) {
    val colors = tileColors(state)
    Surface(
        modifier =
            Modifier
                .size(size)
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
                fontSize = if (largeText) 32.sp else 28.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WordlyKeyboard(
    rows: List<List<WordlyKeyUiState>>,
    keyHeight: Dp,
    actions: WordlyGameActions,
    enabled: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("wordlyKeyboard"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (rowIndex == rows.lastIndex) {
                    KeyboardAction("Check", keyHeight, actions.onSubmit, enabled, Modifier.weight(1.35f))
                }
                row.forEach { key ->
                    LetterKey(
                        key = key,
                        height = keyHeight,
                        onLetter = actions.onLetter,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowIndex == rows.lastIndex) {
                    KeyboardAction("Delete", keyHeight, actions.onDelete, enabled, Modifier.weight(1.35f))
                }
            }
        }
    }
}

@Composable
private fun LetterKey(
    key: WordlyKeyUiState,
    height: Dp,
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
                .height(height)
                .aspectRatio(0.82f),
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
        Text(key.letter.toString(), fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
private fun KeyboardAction(
    label: String,
    height: Dp,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WordlyCluePanel(
    state: WordlyUiState,
    onUseHint: () -> Unit,
    onReturnHome: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("wordlyCluePanel"),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF9FFE8).copy(alpha = 0.96f),
        border = BorderStroke(2.dp, Color(0xFFB7D477)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Clue", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(state.clueText, style = MaterialTheme.typography.bodyLarge)
            state.openHintText?.let { hint ->
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF2C0)) {
                    Text(hint, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                }
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.testTag("wordlyMessage"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.summary != null) {
                WordlySummaryPanel(summary = state.summary, sharePattern = state.sharePattern)
                Button(onClick = onReturnHome, modifier = Modifier.fillMaxWidth()) {
                    Text("Return to Daily Five")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onUseHint,
                        enabled = state.canUseHint,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Use Hint ${state.hintsRemaining}")
                    }
                    Button(
                        onClick = onReturnHome,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save and Exit")
                    }
                }
            }
        }
    }
}

@Composable
private fun WordlySummaryPanel(
    summary: WordlyLearningSummary,
    sharePattern: String?,
) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(summary.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(summary.body)
            summary.example?.let { Text(it, fontWeight = FontWeight.SemiBold) }
            summary.morphology?.let { Text(it) }
            sharePattern?.let { pattern ->
                Text("Share-safe result", fontWeight = FontWeight.Bold)
                Text(
                    text = pattern,
                    modifier = Modifier.testTag("wordlySharePreview"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        val safeSharePattern =
            shareCard
                ?.takeUnless { ShareSafety.leaksForbiddenPayload(it) }
                ?.visibleResultPattern
        val openHints = puzzle.hints.filter { it.order in gameState.revealedHintOrders }.sortedBy { it.order }

        return WordlyUiState(
            attemptsLabel = "${gameState.attempts.size}/${WordlyGameEngine.MAX_ATTEMPTS}",
            streakLabel = "Streak: ${wordlyStreak(homeState)}",
            starLabel = "Stars ${starScore(gameState)}/20",
            prompt = "Find the hidden 5-letter word.",
            rows = WordlyGameEngine.boardRows(puzzle, gameState),
            keyboardRows = keyboardRows(WordlyGameEngine.keyboardStates(puzzle, gameState)),
            clueText = puzzle.definition,
            openHintText = openHints.lastOrNull()?.text,
            hintsRemaining = puzzle.hints.size - openHints.size,
            canUseHint = !gameState.isTerminal && openHints.size < puzzle.hints.size,
            message = transientMessage ?: terminalMessage(gameState),
            summary = if (gameState.isTerminal) WordlyGameEngine.learningSummary(puzzle, gameState) else null,
            sharePattern = safeSharePattern,
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
)

internal data class WordlyUiState(
    val attemptsLabel: String,
    val streakLabel: String,
    val starLabel: String,
    val prompt: String,
    val rows: List<WordlyBoardRow>,
    val keyboardRows: List<List<WordlyKeyUiState>>,
    val clueText: String,
    val openHintText: String?,
    val hintsRemaining: Int,
    val canUseHint: Boolean,
    val message: String?,
    val summary: WordlyLearningSummary?,
    val sharePattern: String?,
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
