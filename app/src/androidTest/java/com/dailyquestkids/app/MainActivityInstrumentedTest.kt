package com.dailyquestkids.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.waitUntilAtLeastOneExists(hasText("Five puzzles. A new quest every day."), 3_000L)
        compose.onNodeWithText("Daily Quest Kids").assertIsDisplayed()
        compose.onNodeWithText("Five puzzles. A new quest every day.").assertIsDisplayed()
    }

    @Test
    fun heroPanelScreenshotSmokeHasRenderablePixels() {
        compose.waitUntilAtLeastOneExists(hasText("Skip for now"), 3_000L)
        compose.onNodeWithText("Skip for now").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("heroStreakPanel"), 3_000L)

        val image = compose.onNodeWithTag("heroStreakPanel").captureToImage()

        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }
}
