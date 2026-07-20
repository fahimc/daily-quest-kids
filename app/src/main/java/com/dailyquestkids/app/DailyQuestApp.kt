package com.dailyquestkids.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.dailyquestkids.core.design.categoryStyle
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val showBottomBar = currentRoute in setOf(Route.Home.path, Route.Streaks.path, Route.Achievements.path, Route.Settings.path)
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
                QuestBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigate(route.path) },
                )
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
                    onOpenHowToPlay = { navController.navigate(Route.HowToPlay.path) },
                    onOpenParentInfo = { navController.navigate(Route.ParentInfo.path) },
                )
            }
            composable(Route.Streaks.path) {
                StreakScreen(state = homeState, progress = progress)
            }
            composable(Route.Achievements.path) {
                AchievementsScreen(state = homeState, progress = progress)
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
            composable(Route.HowToPlay.path) {
                HowToPlayScreen(onBack = { navController.popBackStack() })
            }
            composable("puzzle/{puzzleId}") { entry ->
                val puzzleId = entry.arguments?.getString("puzzleId").orEmpty()
                val puzzle = coordinator.puzzleById(puzzleId)
                when (puzzle) {
                    is WordlyPuzzle -> {
                        WordlyRoute(
                            data =
                                WordlyRouteData(
                                    puzzle = puzzle,
                                    settings = settings,
                                    homeState = homeState,
                                ),
                            dependencies =
                                WordlyRouteDependencies(
                                    progressStore = container.progressStore,
                                    wordlyProgressStore = container.wordlyProgressStore,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                ),
                            onBack = { navController.popBackStack() },
                            onReturnHome = {
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            },
                        )
                    }
                    is SpellingBeePuzzle -> {
                        SpellingBRoute(
                            data =
                                SpellingBRouteData(
                                    puzzle = puzzle,
                                    settings = settings,
                                    homeState = homeState,
                                ),
                            dependencies =
                                SpellingBRouteDependencies(
                                    progressStore = container.progressStore,
                                    spellingProgressStore = container.spellingProgressStore,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                ),
                            onBack = { navController.popBackStack() },
                            onReturnHome = {
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            },
                        )
                    }
                    is CrosswordPuzzle -> {
                        CrosswordRoute(
                            data =
                                CrosswordRouteData(
                                    puzzle = puzzle,
                                    settings = settings,
                                    homeState = homeState,
                                ),
                            dependencies =
                                CrosswordRouteDependencies(
                                    progressStore = container.progressStore,
                                    crosswordProgressStore = container.crosswordProgressStore,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                ),
                            onBack = { navController.popBackStack() },
                            onReturnHome = {
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            },
                        )
                    }
                    is SudokuPuzzle -> {
                        SudokuRoute(
                            data =
                                SudokuRouteData(
                                    puzzle = puzzle,
                                    settings = settings,
                                    homeState = homeState,
                                ),
                            dependencies =
                                SudokuRouteDependencies(
                                    progressStore = container.progressStore,
                                    sudokuProgressStore = container.sudokuProgressStore,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                ),
                            onBack = { navController.popBackStack() },
                            onReturnHome = {
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            },
                        )
                    }
                    is ConnectionsPuzzle -> {
                        ConnectionsRoute(
                            data =
                                ConnectionsRouteData(
                                    puzzle = puzzle,
                                    settings = settings,
                                    homeState = homeState,
                                ),
                            dependencies =
                                ConnectionsRouteDependencies(
                                    progressStore = container.progressStore,
                                    connectionsProgressStore = container.connectionsProgressStore,
                                    dayIndex = homeState.globalDayNumber - 1,
                                    todaysPuzzleIds = coordinator.currentPuzzleIds(),
                                ),
                            onBack = { navController.popBackStack() },
                            onReturnHome = {
                                navController.navigate(Route.Home.path) {
                                    popUpTo(Route.Home.path) { inclusive = true }
                                }
                            },
                        )
                    }
                    else -> {
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

    fun openHowToPlay() {
        navController.navigate(Route.HowToPlay.path)
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
    QuestSceneFrame(testTag = "splashScreen") { metrics ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.pagePadding, vertical = (32f * metrics.scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height((130f * metrics.scale).dp))
            QuestIntroTitle(metrics)
            ProgressDots(current = 0, total = 3)
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

    if (page == WELCOME_PAGE) {
        WelcomePanel(
            onStart = { page = 0 },
            onHowItWorks = { page = 0 },
            onSkip = onComplete,
        )
    } else {
        QuestSceneFrame(testTag = "onboardingScreen") { metrics ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(metrics.pagePadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(metrics.gap),
            ) {
                QuestLogo(modifier = Modifier.fillMaxWidth(0.62f), compact = true)
                ProgressDots(current = page, total = pages.size)
                QuestPanel {
                    val current = pages[page]
                    Text(current.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(current.body, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.weight(1f))
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
}

@Composable
private fun WelcomePanel(
    onStart: () -> Unit,
    onHowItWorks: () -> Unit,
    onSkip: () -> Unit,
) {
    QuestSceneFrame(testTag = "welcomeScreen") { metrics ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.pagePadding, vertical = (24f * metrics.scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height((92f * metrics.scale).dp))
            QuestIntroTitle(metrics)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(metrics.gap),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp)) {
                    stateCategoryOrder().forEach { category ->
                        QuestIconTile(
                            category = category,
                            modifier = Modifier.size((46f * metrics.scale).dp),
                            scale = metrics.scale,
                        )
                    }
                }
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Playing")
                }
                OutlinedButton(onClick = onHowItWorks, modifier = Modifier.fillMaxWidth()) {
                    Text("How It Works")
                }
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip for now")
                }
                ProgressDots(current = 0, total = 3)
            }
        }
    }
}

@Composable
private fun QuestIntroTitle(metrics: QuestScreenMetrics) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        QuestLogo(
            modifier = Modifier.fillMaxWidth(0.84f),
            compact = metrics.compact,
        )
        Spacer(Modifier.height((28f * metrics.scale).dp))
        QuestAutoText(
            text = "Five puzzles.\nA new quest every day.",
            modifier =
                Modifier
                    .fillMaxWidth(0.75f)
                    .height((68f * metrics.scale).dp),
            spec =
                QuestAutoTextSpec(
                    maxFontSizeSp = 24f * metrics.scale,
                    minFontSizeSp = 12f,
                    maxLines = 2,
                    color = Color(0xFF08284F),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

@Composable
private fun HomeScreen(
    state: DailyHomeUiState,
    onOpenPuzzle: (QuestCardUiState) -> Unit,
    onOpenHowToPlay: () -> Unit,
    onOpenParentInfo: () -> Unit,
) {
    var showComplete by remember(state.globalDayNumber, state.isDailyFiveComplete) {
        mutableStateOf(state.isDailyFiveComplete)
    }

    if (state.isDailyFiveComplete && showComplete) {
        DailyFiveCelebration(state = state, onDone = { showComplete = false })
    } else {
        QuestHomeDashboard(
            state = state,
            onOpenPuzzle = onOpenPuzzle,
            onOpenHowToPlay = onOpenHowToPlay,
            onOpenParentInfo = onOpenParentInfo,
        )
    }
}

@Composable
private fun QuestHomeDashboard(
    state: DailyHomeUiState,
    onOpenPuzzle: (QuestCardUiState) -> Unit,
    onOpenHowToPlay: () -> Unit,
    onOpenParentInfo: () -> Unit,
) {
    QuestSceneFrame(testTag = "homeScreen") { metrics ->
        val headerHeight = if (metrics.compact) 88f else 114f
        val heroHeight = if (metrics.compact) 112f else 132f
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = metrics.pagePadding, vertical = metrics.gap),
            verticalArrangement = Arrangement.spacedBy(metrics.gap),
        ) {
            HomeQuestHeader(
                state = state,
                metrics = metrics,
                onOpenHowToPlay = onOpenHowToPlay,
                onOpenParentInfo = onOpenParentInfo,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((headerHeight * metrics.scale).dp),
            )
            DailyStreakPanel(
                state = state,
                metrics = metrics,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((heroHeight * metrics.scale).dp),
            )
            SeasonStateMessage(state.dayState)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(metrics.gap),
            ) {
                state.cards.forEachIndexed { index, card ->
                    QuestHomeCard(
                        rank = index + 1,
                        card = card,
                        metrics = metrics,
                        onOpen = { onOpenPuzzle(card) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuestHeader(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
    onOpenHowToPlay: () -> Unit,
    onOpenParentInfo: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.gap),
    ) {
        QuestLogo(
            modifier =
                Modifier
                    .weight(0.86f)
                    .fillMaxHeight(),
            compact = true,
        )
        Column(
            modifier = Modifier.weight(1.1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            QuestAutoText(
                text = "Hey, Explorer!",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((36f * metrics.scale).dp),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 28f * metrics.scale,
                        minFontSizeSp = 12f,
                        maxLines = 1,
                        color = Color(0xFF061E3E),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        softWrap = false,
                    ),
            )
            QuestPill(
                text = state.date.questDateLabel(),
                modifier = Modifier.fillMaxWidth(),
                scale = metrics.scale,
            )
        }
        Row(
            modifier = Modifier.weight(0.52f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeCircleButton("?", "homeHowToButton", onOpenHowToPlay, metrics.scale)
            Spacer(Modifier.width((6f * metrics.scale).dp))
            HomeCircleButton("⚙", "homeParentInfoButton", onOpenParentInfo, metrics.scale)
        }
    }
}

@Composable
private fun HomeCircleButton(
    label: String,
    tag: String,
    onClick: () -> Unit,
    scale: Float,
) {
    Surface(
        modifier =
            Modifier
                .size((44f * scale).dp)
                .testTag(tag)
                .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.94f),
        shape = CircleShape,
        shadowElevation = (3f * scale).dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color(0xFF073A73), fontSize = (22f * scale).sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DailyStreakPanel(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.testTag("heroStreakPanel"),
        color = Color(0xFF06386D),
        border = BorderStroke((2f * metrics.scale).dp, Color(0xFF1369BD)),
        shape = RoundedCornerShape((24f * metrics.scale).dp),
        shadowElevation = (4f * metrics.scale).dp,
    ) {
        Row(
            modifier = Modifier.padding((14f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((14f * metrics.scale).dp),
        ) {
            Surface(
                modifier = Modifier.size((74f * metrics.scale).dp),
                color = Color(0xFF0C65C8),
                shape = CircleShape,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("★", color = Color(0xFFFFB51D), fontSize = (38f * metrics.scale).sp, fontWeight = FontWeight.Black)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((7f * metrics.scale).dp),
            ) {
                QuestAutoText(
                    text = "Daily Five Streak ★",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((28f * metrics.scale).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 23f * metrics.scale,
                            minFontSizeSp = 10f,
                            maxLines = 1,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                        ),
                )
                QuestProgressSegments(
                    completedCount = state.completedCount,
                    total = state.cards.size.coerceAtLeast(1),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((14f * metrics.scale).dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy((18f * metrics.scale).dp)) {
                    StreakStat("Current", state.currentDailyFiveStreak, metrics.scale)
                    StreakStat("Best", state.bestDailyFiveStreak, metrics.scale)
                }
            }
            ProgressRing(state = state, scale = metrics.scale)
        }
    }
}

@Composable
private fun StreakStat(
    label: String,
    value: Int,
    scale: Float,
) {
    Column {
        Text(label, color = Color(0xFFBFD5F4), fontSize = (11f * scale).sp)
        Text("★ $value", color = Color.White, fontSize = (18f * scale).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ProgressRing(
    state: DailyHomeUiState,
    scale: Float,
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size((78f * scale).dp)) {
        CircularProgressIndicator(
            progress = {
                state.completedCount /
                    state.cards.size
                        .coerceAtLeast(1)
                        .toFloat()
            },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF9CE33B),
            trackColor = Color(0xFF174A82),
            strokeWidth = (6f * scale).dp,
        )
        Text("${state.completedCount}/${state.cards.size}", color = Color.White, fontSize = (27f * scale).sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun QuestHomeCard(
    rank: Int,
    card: QuestCardUiState,
    metrics: QuestScreenMetrics,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val style = categoryStyle(card.category)
    Surface(
        modifier =
            modifier
                .clickable(onClick = onOpen)
                .semantics {
                    contentDescription = "${card.title}, ${card.description}, ${card.status.label}"
                    role = Role.Button
                },
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke((1.4f * metrics.scale).dp, style.border.copy(alpha = 0.72f)),
        shape = RoundedCornerShape((18f * metrics.scale).dp),
        shadowElevation = (2f * metrics.scale).dp,
    ) {
        Row(
            modifier = Modifier.padding((10f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((10f * metrics.scale).dp),
        ) {
            RankBubble(rank = rank, category = card.category, scale = metrics.scale)
            QuestIconTile(
                category = card.category,
                completed = card.status == PuzzleStatus.COMPLETED,
                scale = metrics.scale,
                modifier = Modifier.aspectRatio(1f).fillMaxHeight(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                QuestAutoText(
                    text = card.title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((28f * metrics.scale).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 24f * metrics.scale,
                            minFontSizeSp = 11f,
                            maxLines = 1,
                            color = style.accent,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                        ),
                )
                QuestAutoText(
                    text = card.description,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height((22f * metrics.scale).dp),
                    spec =
                        QuestAutoTextSpec(
                            maxFontSizeSp = 14f * metrics.scale,
                            minFontSizeSp = 7f,
                            maxLines = 1,
                            color = Color(0xFF172337),
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start,
                            softWrap = false,
                        ),
                )
                QuestStatusBadge(status = card.status, category = card.category, scale = metrics.scale)
            }
            QuestActionButton(
                text = if (card.status == PuzzleStatus.COMPLETED) "✓" else card.actionLabel,
                color = if (card.status == PuzzleStatus.COMPLETED) Color(0xFF72C73E) else style.accent,
                onClick = onOpen,
                modifier =
                    Modifier
                        .width((112f * metrics.scale).dp)
                        .fillMaxHeight(0.66f),
            )
        }
    }
}

@Composable
private fun RankBubble(
    rank: Int,
    category: PuzzleCategory,
    scale: Float,
) {
    Surface(
        modifier = Modifier.size((26f * scale).dp),
        color = categoryStyle(category).accent,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(rank.toString(), color = Color.White, fontWeight = FontWeight.Black, fontSize = (15f * scale).sp)
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
private fun DailyFiveCelebration(
    state: DailyHomeUiState,
    onDone: () -> Unit,
) {
    QuestSceneFrame(testTag = "dailyFiveCompleteScreen") { metrics ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xCC032453)),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(metrics.pagePadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(metrics.gap),
        ) {
            QuestAutoText(
                text = "DAILY FIVE\nCOMPLETE!",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height((108f * metrics.scale).dp),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 42f * metrics.scale,
                        minFontSizeSp = 18f,
                        maxLines = 2,
                        color = Color(0xFFFFC32A),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                    ),
            )
            QuestAutoText(
                text = "Amazing work, Explorer!\nYou completed all 5 puzzles today!",
                modifier =
                    Modifier
                        .fillMaxWidth(0.88f)
                        .height((46f * metrics.scale).dp),
                spec =
                    QuestAutoTextSpec(
                        maxFontSizeSp = 17f * metrics.scale,
                        minFontSizeSp = 9f,
                        maxLines = 2,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
            )
            CompletionIconStrip(state = state, metrics = metrics)
            CompletionStreakStrip(state = state, metrics = metrics)
            Text(
                text = "Keep it up! Your brain is growing stronger every day.",
                color = Color(0xFFCDE5FF),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = (14f * metrics.scale).sp,
            )
            CompletionSharePreview(
                state = state,
                metrics = metrics,
                modifier =
                    Modifier
                        .fillMaxWidth(0.92f)
                        .weight(1f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.gap),
            ) {
                QuestActionButton("Share", Color(0xFF168CE4), Modifier.weight(1f).height((58f * metrics.scale).dp)) {}
                QuestActionButton("Save", Color(0xFF4DBC43), Modifier.weight(1f).height((58f * metrics.scale).dp)) {}
                QuestActionButton("Done", Color(0xFFFFA40E), Modifier.weight(1f).height((58f * metrics.scale).dp), onDone)
            }
            Text(
                text = "Ask a grown-up before sharing outside the app.",
                color = Color(0xFFB7C8DE),
                fontSize = (12f * metrics.scale).sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CompletionIconStrip(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth(0.92f)
                .height((96f * metrics.scale).dp),
        color = Color(0xFF073564),
        border = BorderStroke((1.5f * metrics.scale).dp, Color(0xFF15599B)),
        shape = RoundedCornerShape((20f * metrics.scale).dp),
    ) {
        Row(
            modifier = Modifier.padding((10f * metrics.scale).dp),
            horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.cards.forEach { card ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy((4f * metrics.scale).dp),
                ) {
                    QuestIconTile(
                        category = card.category,
                        completed = true,
                        modifier = Modifier.size((52f * metrics.scale).dp),
                        scale = metrics.scale,
                    )
                    QuestAutoText(
                        text = card.title,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height((18f * metrics.scale).dp),
                        spec =
                            QuestAutoTextSpec(
                                maxFontSizeSp = 10f * metrics.scale,
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
private fun CompletionStreakStrip(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth(0.92f)
                .height((70f * metrics.scale).dp),
        color = Color(0xFF073564),
        border = BorderStroke((1.5f * metrics.scale).dp, Color(0xFF15599B)),
        shape = RoundedCornerShape((18f * metrics.scale).dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = (16f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("★ Daily Five Streak", color = Color.White, fontSize = (15f * metrics.scale).sp, fontWeight = FontWeight.Black)
            QuestPill("${state.completedCount}/${state.cards.size}", scale = metrics.scale)
            Text(
                "Best ★\n${state.bestDailyFiveStreak}",
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = (13f * metrics.scale).sp,
            )
        }
    }
}

@Composable
private fun CompletionSharePreview(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.95f),
        border = BorderStroke((3f * metrics.scale).dp, Color.White),
        shape = RoundedCornerShape((22f * metrics.scale).dp),
        shadowElevation = (4f * metrics.scale).dp,
    ) {
        Column(
            modifier = Modifier.padding((12f * metrics.scale).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((6f * metrics.scale).dp),
        ) {
            QuestLogo(modifier = Modifier.fillMaxWidth(0.56f), compact = true)
            QuestPill(state.date.questDateLabel(), modifier = Modifier.fillMaxWidth(0.78f), scale = metrics.scale)
            Text("Daily Five Complete!", color = Color(0xFF074176), fontWeight = FontWeight.Black, fontSize = (17f * metrics.scale).sp)
            Row(horizontalArrangement = Arrangement.spacedBy((8f * metrics.scale).dp)) {
                state.cards.forEach { card ->
                    QuestIconTile(
                        category = card.category,
                        completed = true,
                        modifier = Modifier.size((44f * metrics.scale).dp),
                        scale = metrics.scale,
                    )
                }
            }
            CompletionStreakMini(state = state, metrics = metrics)
        }
    }
}

@Composable
private fun CompletionStreakMini(
    state: DailyHomeUiState,
    metrics: QuestScreenMetrics,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .height((46f * metrics.scale).dp),
        color = Color(0xFF06386D),
        shape = RoundedCornerShape((14f * metrics.scale).dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = (12f * metrics.scale).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Daily Five Streak", color = Color.White, fontSize = (12f * metrics.scale).sp, fontWeight = FontWeight.Bold)
            Text(
                "${state.completedCount}/${state.cards.size}",
                color = Color.White,
                fontSize = (15f * metrics.scale).sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Best ${state.bestDailyFiveStreak}",
                color = Color.White,
                fontSize = (12f * metrics.scale).sp,
                fontWeight = FontWeight.Bold,
            )
        }
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
private fun AchievementsScreen(
    state: DailyHomeUiState,
    progress: StoredProgress,
) {
    ScreenColumn {
        Text("Achievements", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        QuestPanel {
            Text("Daily Five Badges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Perfect days: ${state.perfectDayCount}")
            Text("Best daily streak: ${state.bestDailyFiveStreak}")
            Text("Total puzzles solved: ${progress.completedPuzzleIds.size}")
        }
        QuestPanel {
            Text("Puzzle Lands", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            state.cards.forEach { card ->
                Text("${card.title}: ${card.categoryStreak} completed")
            }
        }
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
        Button(onClick = actions::openHowToPlay, modifier = Modifier.fillMaxWidth()) {
            Text("How to play")
        }
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
private fun QuestBottomBar(
    currentRoute: String,
    onNavigate: (Route) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("questBottomBar"),
        color = Color(0xFF04335F),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(Route.Home, Route.Streaks, Route.Achievements, Route.Settings).forEach { route ->
                val selected = currentRoute == route.path
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onNavigate(route) }
                            .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = route.symbol,
                        color = if (selected) Color(0xFFFFD328) else Color(0xFF9FB9D5),
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                    )
                    Text(
                        text = route.label,
                        color = if (selected) Color(0xFFFFD328) else Color(0xFFC9D5E4),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                    )
                }
            }
        }
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
private fun HowToPlayScreen(onBack: () -> Unit) {
    ScreenColumn {
        Text("How to play", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Finish one small puzzle from each land. Hints are for learning and do not break a streak.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GuideSectionCard(
            title = "Wordly",
            subtitle = "Find the hidden 5-letter word.",
            steps =
                listOf(
                    "Type a 5-letter guess, then tap Check.",
                    "Green means the letter is in the right spot.",
                    "Yellow means the letter is in the word but moved.",
                    "Dark means that letter is not in the word.",
                    "Tap the clue box to make it bigger. Use Hint when stuck.",
                ),
        )
        GuideSectionCard(
            title = "Spelling B",
            subtitle = "Build words from the honeycomb letters.",
            steps =
                listOf(
                    "Every word must use the middle letter.",
                    "Use each outside letter as often as you need.",
                    "Longer words score more.",
                    "Try prefixes and endings when you get stuck.",
                ),
        )
        GuideSectionCard(
            title = "Crossword",
            subtitle = "Fill the grid using clues.",
            steps =
                listOf(
                    "Pick a clue, then type the answer into the squares.",
                    "Across answers go left to right. Down answers go top to bottom.",
                    "Use crossing letters to solve harder clues.",
                    "A hint can reveal a helpful letter or clue nudge.",
                ),
        )
        GuideSectionCard(
            title = "Sudoku",
            subtitle = "Complete the number grid.",
            steps =
                listOf(
                    "Each row needs the numbers 1 to 6 once.",
                    "Each column needs the numbers 1 to 6 once.",
                    "Each 2 by 3 box also needs 1 to 6 once.",
                    "Start where the most numbers are already filled in.",
                ),
        )
        GuideSectionCard(
            title = "Connections",
            subtitle = "Find groups that belong together.",
            steps =
                listOf(
                    "Look for four words with the same idea.",
                    "Select four words, then submit the group.",
                    "Solved groups leave the board.",
                    "Read the explanation to learn the link.",
                ),
        )
        QuestPanel {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Wordly, Spelling B, Crossword, Sudoku and Connections are fully playable.",
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun GuideSectionCard(
    title: String,
    subtitle: String,
    steps: List<String>,
) {
    QuestPanel {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge)
        steps.forEachIndexed { index, step ->
            Text("${index + 1}. $step")
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

private fun stateCategoryOrder(): List<PuzzleCategory> =
    listOf(
        PuzzleCategory.WORDLY,
        PuzzleCategory.SPELLING_B,
        PuzzleCategory.CROSSWORD,
        PuzzleCategory.SUDOKU,
        PuzzleCategory.CONNECTIONS,
    )

private fun LocalDate.questDateLabel(): String = format(QuestDateFormatter)

private enum class Route(
    val path: String,
    val label: String,
    val symbol: String,
) {
    Home("home", "Home", "⌂"),
    Streaks("streaks", "Streaks", "★"),
    Achievements("achievements", "Achievements", "♕"),
    Settings("settings", "Settings", "⚙"),
    ParentInfo("parent-info", "Parent Info", "i"),
    HowToPlay("how-to-play", "How to Play", "?"),
}

private val QuestDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH)

private const val WELCOME_PAGE = -1
private const val SPLASH_DURATION_MILLIS = 650L
