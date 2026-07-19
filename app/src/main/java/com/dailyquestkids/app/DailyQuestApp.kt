package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailyquestkids.core.common.SeasonDayState
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.design.QuestCategoryStyle
import com.dailyquestkids.core.design.categoryStyle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DailyQuestApp(container: DailyQuestContainer) {
    val settings by container.settingsStore.settings.collectAsStateWithLifecycle(initialValue = QuestSettings())
    val progress by container.progressStore.progress.collectAsStateWithLifecycle(initialValue = StoredProgress())
    val coordinator =
        remember(container) {
            DailyHomeCoordinator(
                packRepository = container.packRepository,
                calendar = container.calendar,
            )
        }
    val homeState = remember(progress) { coordinator.homeState(progress) }
    val scope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        showSplash = false
    }

    LaunchedEffect(homeState.globalDayNumber) {
        container.progressStore.observeDay(homeState.globalDayNumber - 1)
    }

    DailyQuestTheme(highContrast = settings.highContrast) {
        when {
            showSplash -> SplashScreen()
            !settings.onboardingComplete ->
                OnboardingFlow(
                    onComplete = {
                        scope.launch { container.settingsStore.completeOnboarding() }
                    },
                )
            else ->
                MainQuestScaffold(
                    settings = settings,
                    progress = progress,
                    coordinator = coordinator,
                    homeState = homeState,
                    container = container,
                )
        }
    }
}

@Composable
private fun MainQuestScaffold(
    settings: QuestSettings,
    progress: StoredProgress,
    coordinator: DailyHomeCoordinator,
    homeState: DailyHomeUiState,
    container: DailyQuestContainer,
) {
    val navController = rememberNavController()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route ?: Route.Home.path
    val showBottomBar = currentRoute in setOf(Route.Home.path, Route.Streaks.path, Route.Settings.path)
    val scope = rememberCoroutineScope()
    val settingsActions =
        remember(container, navController, scope) {
            SettingsActions(
                navController = navController,
                settingsStore = container.settingsStore,
                progressStore = container.progressStore,
                scope = scope,
            )
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(Route.Home, Route.Streaks, Route.Settings).forEach { route ->
                        NavigationBarItem(
                            selected = currentRoute == route.path,
                            onClick = { navController.navigate(route.path) },
                            label = { Text(route.label) },
                            icon = { Text(route.symbol) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    state = homeState,
                    onOpenPuzzle = { card ->
                        scope.launch {
                            container.progressStore.markStarted(card.puzzle.id)
                            navController.navigate("puzzle/${card.puzzle.id}")
                        }
                    },
                    onOpenParentInfo = { navController.navigate(Route.ParentInfo.path) },
                )
            }
            composable(Route.Streaks.path) {
                StreakScreen(state = homeState, progress = progress)
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    settings = settings,
                    actions = settingsActions,
                )
            }
            composable(Route.ParentInfo.path) {
                ParentInformationScreen(onBack = { navController.popBackStack() })
            }
            composable("puzzle/{puzzleId}") { entry ->
                val puzzleId = entry.arguments?.getString("puzzleId").orEmpty()
                val puzzle = coordinator.puzzleById(puzzleId)
                PuzzlePreviewScreen(
                    puzzle = puzzle,
                    status = progress.statusFor(puzzleId),
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        if (puzzle != null) {
                            scope.launch {
                                container.progressStore.markCompleted(
                                    puzzleId = puzzle.id,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                )
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

private class SettingsActions(
    private val navController: NavHostController,
    private val settingsStore: SettingsStore,
    private val progressStore: ProgressStore,
    private val scope: CoroutineScope,
) {
    fun openParentInfo() {
        navController.navigate(Route.ParentInfo.path)
    }

    fun resetProgress() {
        scope.launch { progressStore.resetProgress() }
    }

    fun resetOnboarding() {
        scope.launch { settingsStore.resetOnboarding() }
    }

    fun soundChanged(value: Boolean) {
        scope.launch { settingsStore.updateSound(value) }
    }

    fun hapticsChanged(value: Boolean) {
        scope.launch { settingsStore.updateHaptics(value) }
    }

    fun reducedMotionChanged(value: Boolean) {
        scope.launch { settingsStore.updateReducedMotion(value) }
    }

    fun highContrastChanged(value: Boolean) {
        scope.launch { settingsStore.updateHighContrast(value) }
    }

    fun largePuzzleTextChanged(value: Boolean) {
        scope.launch { settingsStore.updateLargePuzzleText(value) }
    }

    fun timerChanged(value: Boolean) {
        scope.launch { settingsStore.updateOptionalTimer(value) }
    }

    fun mistakeCheckingChanged(value: Boolean) {
        scope.launch { settingsStore.updateMistakeChecking(value) }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD05A)),
                contentAlignment = Alignment.Center,
            ) {
                Text("5", fontSize = 44.sp, fontWeight = FontWeight.Black)
            }
            Text(
                text = "Daily Quest Kids",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun OnboardingFlow(onComplete: () -> Unit) {
    var page by remember { mutableIntStateOf(WELCOME_PAGE) }
    val pages =
        listOf(
            OnboardingPage("Five new puzzles", "Every day brings one word, spelling, crossword, sudoku and connections quest."),
            OnboardingPage("Build calm streaks", "Hints help you learn and do not break a streak."),
            OnboardingPage("Share safely", "Result pictures hide answers. Ask a grown-up before sharing outside the app."),
        )

    ScreenColumn {
        if (page == WELCOME_PAGE) {
            WelcomePanel(
                onStart = { page = 0 },
                onHowItWorks = { page = 0 },
                onSkip = onComplete,
            )
        } else {
            val current = pages[page]
            Text("How It Works", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            ProgressDots(current = page, total = pages.size)
            Spacer(Modifier.height(16.dp))
            QuestPanel {
                Text(current.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(current.body, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { if (page == 0) page = WELCOME_PAGE else page -= 1 },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back")
                }
                Button(
                    onClick = { if (page == pages.lastIndex) onComplete() else page += 1 },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (page == pages.lastIndex) "Start Playing" else "Continue")
                }
            }
            TextButton(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                Text("Skip")
            }
        }
    }
}

@Composable
private fun WelcomePanel(
    onStart: () -> Unit,
    onHowItWorks: () -> Unit,
    onSkip: () -> Unit,
) {
    Text(
        text = "Daily Quest Kids",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.ExtraBold,
    )
    Text(
        text = "Five puzzles. A new quest every day.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))
    QuestPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf("W", "B", "C", "S", "L").forEach { letter ->
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(letter, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
                }
            }
        }
        Text("Solve a small set of friendly puzzle quests each day. Everything works offline.")
    }
    Spacer(Modifier.height(18.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text("Start Playing")
    }
    OutlinedButton(onClick = onHowItWorks, modifier = Modifier.fillMaxWidth()) {
        Text("How It Works")
    }
    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
        Text("Skip for now")
    }
}

@Composable
private fun HomeScreen(
    state: DailyHomeUiState,
    onOpenPuzzle: (QuestCardUiState) -> Unit,
    onOpenParentInfo: () -> Unit,
) {
    ScreenColumn {
        Text(
            text = "Daily Quest Kids",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = state.date.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        HeroStreakPanel(state = state)
        Spacer(Modifier.height(16.dp))
        SeasonStateMessage(state.dayState)
        state.cards.forEach { card ->
            QuestCard(card = card, onOpen = { onOpenPuzzle(card) })
        }
        Spacer(Modifier.height(12.dp))
        if (state.isDailyFiveComplete) {
            DailyFiveCelebration(state)
        }
        OutlinedButton(onClick = onOpenParentInfo, modifier = Modifier.fillMaxWidth()) {
            Text("Parent information")
        }
    }
}

@Composable
private fun HeroStreakPanel(state: DailyHomeUiState) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("heroStreakPanel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4CE)),
        border = BorderStroke(2.dp, Color(0xFFE7A92F)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD05A)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("5", fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Column {
                    Text("Daily Five streak", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Day ${state.globalDayNumber} - ${state.completedCount}/5 solved")
                    Text("Best streak: ${state.bestDailyFiveStreak}")
                }
            }
            LinearProgressIndicator(
                progress = { state.completedCount / PuzzleCategory.entries.size.toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(state.message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SeasonStateMessage(dayState: SeasonDayState) {
    when (dayState) {
        SeasonDayState.BeforeSeason -> StatusPanel("Season starts soon. You can preview Day 1.")
        SeasonDayState.SeasonComplete -> StatusPanel("Season complete. Your history stays safe on this device.")
        is SeasonDayState.Active -> Unit
    }
}

@Composable
private fun StatusPanel(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun QuestCard(
    card: QuestCardUiState,
    onOpen: () -> Unit,
) {
    val style = categoryStyle(card.category)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .semantics {
                    contentDescription = "${card.title}, ${card.description}, ${card.status.label}"
                    role = Role.Button
                },
        colors = CardDefaults.cardColors(containerColor = style.container),
        border = BorderStroke(2.dp, style.border),
        shape = RoundedCornerShape(22.dp),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryMedallion(style)
            Column(modifier = Modifier.weight(1f)) {
                Text(card.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(card.description, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onOpen, label = {
                        Text(
                            card.difficulty.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                        )
                    })
                    AssistChip(onClick = onOpen, label = { Text(card.status.label) })
                }
                Text("Hints: ${card.hintCount} - Category streak: ${card.categoryStreak}")
            }
            Button(onClick = onOpen, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                Text(card.actionLabel)
            }
        }
    }
}

@Composable
private fun CategoryMedallion(style: QuestCategoryStyle) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(style.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(style.initial, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun PuzzlePreviewScreen(
    puzzle: Puzzle?,
    status: PuzzleStatus,
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    if (puzzle == null) {
        ScreenColumn {
            Text("Puzzle unavailable", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("This puzzle record could not be loaded.")
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
        return
    }

    val style = categoryStyle(puzzle.category)
    ScreenColumn {
        Text(puzzle.category.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            puzzle.category.destinationName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = style.container,
            border = BorderStroke(2.dp, style.border),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "${puzzle.category.displayName} engine and validation are ready for this fixture.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Status: ${status.label}")
                Text("Hints available: ${puzzle.hints.size}")
                Button(onClick = onComplete, enabled = status != PuzzleStatus.COMPLETED) {
                    Text(if (status == PuzzleStatus.COMPLETED) "Solved" else "Mark solved")
                }
                OutlinedButton(onClick = onBack) {
                    Text("Return to Daily Five")
                }
            }
        }
    }
}

@Composable
private fun DailyFiveCelebration(state: DailyHomeUiState) {
    QuestPanel {
        Text("Daily Five complete", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Current streak: ${state.currentDailyFiveStreak}")
        Text("Perfect days: ${state.perfectDayCount}")
    }
}

@Composable
private fun StreakScreen(
    state: DailyHomeUiState,
    progress: StoredProgress,
) {
    ScreenColumn {
        Text("Streaks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        QuestPanel {
            Text("Daily Five", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Current streak: ${state.currentDailyFiveStreak}")
            Text("Best streak: ${state.bestDailyFiveStreak}")
            Text("Perfect days: ${state.perfectDayCount}")
            Text("Total puzzles solved: ${progress.completedPuzzleIds.size}")
        }
        Text("Calendar")
        MonthPreview(state)
    }
}

@Composable
private fun MonthPreview(state: DailyHomeUiState) {
    val cells = (1..7).toList()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.forEach { day ->
            val filled = day <= state.completedCount
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.toString(),
                    color = if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: QuestSettings,
    actions: SettingsActions,
) {
    var showResetDialog by remember { mutableStateOf(false) }

    ScreenColumn {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        SettingToggle("Sound", settings.soundEnabled, actions::soundChanged)
        SettingToggle("Haptics", settings.hapticsEnabled, actions::hapticsChanged)
        SettingToggle("Reduced motion", settings.reducedMotion, actions::reducedMotionChanged)
        SettingToggle("High contrast", settings.highContrast, actions::highContrastChanged)
        SettingToggle("Large puzzle text", settings.largePuzzleText, actions::largePuzzleTextChanged)
        SettingToggle("Optional timer", settings.optionalTimer, actions::timerChanged)
        SettingToggle("Mistake checking", settings.mistakeChecking, actions::mistakeCheckingChanged)
        Button(onClick = actions::openParentInfo, modifier = Modifier.fillMaxWidth()) {
            Text("Parent information")
        }
        OutlinedButton(onClick = actions::resetOnboarding, modifier = Modifier.fillMaxWidth()) {
            Text("Show onboarding again")
        }
        OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Reset progress")
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset progress?") },
            text = { Text("This clears local puzzle progress and streaks on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        actions.resetProgress()
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onChanged)
        }
    }
}

@Composable
private fun ParentInformationScreen(onBack: () -> Unit) {
    ScreenColumn {
        Text("Parent Information", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        QuestPanel {
            Text("Daily Quest Kids works offline with no adverts, accounts, analytics or internet permission.")
            Text("Progress and settings stay on this device.")
            Text("Shared result cards must hide answers and personal data.")
            Text("Children should ask a grown-up before sharing outside the app.")
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun QuestPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun ProgressDots(
    current: Int,
    total: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            Box(
                modifier =
                    Modifier
                        .size(if (index == current) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == current) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
            )
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Column(content = content)
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val body: String,
)

private enum class Route(
    val path: String,
    val label: String,
    val symbol: String,
) {
    Home("home", "Home", "Q"),
    Streaks("streaks", "Streaks", "S"),
    Settings("settings", "Settings", "G"),
    ParentInfo("parent-info", "Parent Info", "I"),
}

private const val WELCOME_PAGE = -1
private const val SPLASH_DURATION_MILLIS = 650L
