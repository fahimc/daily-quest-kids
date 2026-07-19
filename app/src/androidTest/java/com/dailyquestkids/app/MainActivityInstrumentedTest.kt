package com.dailyquestkids.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainActivityInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsDailyQuestBrand() {
        compose.onNodeWithText("Daily Quest Kids").assertIsDisplayed()
        compose.onNodeWithText("Five puzzles. A new quest every day.").assertIsDisplayed()
    }

    @Test
    fun heroPanelScreenshotSmokeHasRenderablePixels() {
        val image = compose.onNodeWithTag("heroStreakPanel").captureToImage()

        assertTrue(image.width > 0)
        assertTrue(image.height > 0)
    }
}
