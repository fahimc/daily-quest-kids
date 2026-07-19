package com.dailyquestkids.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.design.DailyQuestTheme
import com.dailyquestkids.core.design.QuestCategoryStyle
import com.dailyquestkids.core.design.categoryStyle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DailyQuestTheme {
                DailyQuestApp()
            }
        }
    }
}

@Composable
fun DailyQuestApp() {
    val navController = rememberNavController()
    val currentRoute =
        navController
            .currentBackStackEntryAsState()
            .value
            ?.destination
            ?.route ?: Route.Home.path
    val showBottomBar = currentRoute in setOf(Route.Home.path, Route.Streaks.path, Route.Settings.path)

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
                    onOpenPuzzle = { category -> navController.navigate("puzzle/${category.name}") },
                    onOpenParentInfo = { navController.navigate(Route.ParentInfo.path) },
                )
            }
            composable(Route.Streaks.path) { StreakScreen() }
            composable(Route.Settings.path) {
                SettingsScreen(onOpenParentInfo = { navController.navigate(Route.ParentInfo.path) })
            }
            composable(Route.ParentInfo.path) { ParentInformationScreen(onBack = { navController.popBackStack() }) }
            composable("puzzle/{category}") { entry ->
                val categoryName = entry.arguments?.getString("category").orEmpty()
                val category = PuzzleCategory.valueOf(categoryName)
                PuzzlePreviewScreen(category = category, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenPuzzle: (PuzzleCategory) -> Unit,
    onOpenParentInfo: () -> Unit,
) {
    val repository = remember { SamplePackRepository() }
    val calendar = remember { SeasonCalendar(repository.pack.seasonStartDateUtc, Clock.systemUTC()) }
    val day = calendar.currentDayIndex()
    val dailySet = repository.dailySetFor(day.coerceIn(0, repository.pack.days.lastIndex))

    ScreenColumn {
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
        Spacer(Modifier.height(16.dp))
        HeroStreakPanel(dayNumber = day + 1)
        Spacer(Modifier.height(16.dp))
        dailySet.puzzles.forEach { puzzle ->
            QuestCard(
                category = puzzle.category,
                title = puzzle.category.displayName,
                description = puzzle.category.destinationName,
                status = PuzzleStatus.NOT_STARTED,
                onOpen = { onOpenPuzzle(puzzle.category) },
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenParentInfo, modifier = Modifier.fillMaxWidth()) {
            Text("Parent information")
        }
    }
}

@Composable
private fun HeroStreakPanel(dayNumber: Int) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag("heroStreakPanel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4CE)),
        border = BorderStroke(2.dp, Color(0xFFE7A92F)),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
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
                Text("Day $dayNumber - 0/5 solved", style = MaterialTheme.typography.bodyLarge)
                Text("A new streak can begin today.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun QuestCard(
    category: PuzzleCategory,
    title: String,
    description: String,
    status: PuzzleStatus,
    onOpen: () -> Unit,
) {
    val style = categoryStyle(category)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .semantics {
                    contentDescription = "$title, $description, ${status.label}"
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
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
                Text("Difficulty: Explorer - ${status.label}", style = MaterialTheme.typography.labelLarge)
            }
            Button(onClick = onOpen, contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)) {
                Text("Play")
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
    category: PuzzleCategory,
    onBack: () -> Unit,
) {
    val style = categoryStyle(category)
    ScreenColumn {
        Text(category.displayName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            category.destinationName,
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
                    text = "Engine and content validation are wired for ${category.displayName}.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Playable puzzle UI is scheduled for the feature phases.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onBack) {
                    Text("Return to Daily Five")
                }
            }
        }
    }
}

@Composable
private fun StreakScreen() {
    ScreenColumn {
        Text("Streaks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Daily Five: 0 days")
        Text("Best streak: 0 days")
        Text("Perfect days: 0")
    }
}

@Composable
private fun SettingsScreen(onOpenParentInfo: () -> Unit) {
    ScreenColumn {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Sound: off by default")
        Text("Haptics: optional")
        Text("Reduced motion: respected")
        Text("High contrast: supported by design tokens")
        Button(onClick = onOpenParentInfo, modifier = Modifier.fillMaxWidth()) {
            Text("Parent information")
        }
    }
}

@Composable
private fun ParentInformationScreen(onBack: () -> Unit) {
    ScreenColumn {
        Text("Parent Information", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Daily Quest Kids is designed for offline puzzle practice with no adverts, accounts, analytics or internet permission.")
        Text("Progress and settings stay on this device. Shared result cards must never include answers or personal data.")
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
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
