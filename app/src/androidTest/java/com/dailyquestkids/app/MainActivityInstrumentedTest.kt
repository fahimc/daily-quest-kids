package com.dailyquestkids.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class MainActivityInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsDailyQuestBrand() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithTag("questLogo").assertIsDisplayed()
        compose.onNodeWithTag("heroStreakPanel").assertIsDisplayed()
        compose.onNodeWithTag("questBottomBar").assertIsDisplayed()
    }

    @Test
    fun heroPanelScreenshotSmokeHasRenderablePixels() {
        openHomeFromAnyLaunchState()

        val image = compose.onNodeWithTag("heroStreakPanel").captureToImage()

        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }

    @Test
    fun howToPlayGuideOpensFromHome() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithTag("homeHowToButton").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasText("Find the hidden 5-letter word."), 3_000L)
        compose.onNodeWithText("Wordly").assertIsDisplayed()
        compose.onNodeWithText("Find the hidden 5-letter word.").assertIsDisplayed()
    }

    @Test
    fun spellingBCardOpensPlayableGame() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Spelling B").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag("spellingScreen"), 3_000L)
        compose.onNodeWithTag("spellingHoneycomb").assertIsDisplayed()
        compose.onNodeWithTag("spellingSubmitButton").assertIsDisplayed()
    }

    @Test
    fun crosswordCardOpensPlayableGame() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Crossword").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag("crosswordScreen"), 3_000L)
        compose.onNodeWithTag("crosswordBoard").assertIsDisplayed()
        compose.onNodeWithTag("crosswordKeyboard").assertIsDisplayed()
    }

    @Test
    fun sudokuCardOpensPlayableGame() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Sudoku").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag("sudokuScreen"), 3_000L)
        compose.onNodeWithTag("sudokuBoard").assertIsDisplayed()
        compose.onNodeWithTag("sudokuNumberPad").assertIsDisplayed()
    }

    @Test
    fun connectionsCardOpensPlayableGame() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Connections").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasTestTag("connectionsScreen"), 3_000L)
        compose.onNodeWithTag("connectionsTileGrid").assertIsDisplayed()
        compose.onNodeWithTag("connectionsHintPanel").assertIsDisplayed()
    }

    @Test
    fun achievementsTabOpensFromBottomNavigation() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Achievements").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasText("First Step"), 3_000L)
        compose.onNodeWithTag("achievement-ten-puzzles").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("achievement-hint-free").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun streaksTabShowsHistoryAndCategoryRows() {
        openHomeFromAnyLaunchState()

        compose.onNodeWithText("Streaks").assertIsDisplayed().performClick()

        compose.waitUntilAtLeastOneExists(hasText("Recent history"), 3_000L)
        compose.onNodeWithText("Puzzle lands").assertIsDisplayed()
        compose.onNodeWithTag("categoryStreak-WORDLY").assertIsDisplayed()
        compose.onNodeWithTag("historyDay-1").assertIsDisplayed()
    }

    private fun openHomeFromAnyLaunchState() {
        compose.waitUntil(5_000L) {
            compose.onAllNodesWithText("Skip for now").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasTestTag("heroStreakPanel")).fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Skip for now").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Skip for now").performClick()
        }
        compose.waitUntilAtLeastOneExists(hasTestTag("heroStreakPanel"), 5_000L)
    }
}
