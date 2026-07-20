package com.dailyquestkids.app

import androidx.test.platform.app.InstrumentationRegistry
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShareCardRendererInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val clock = Clock.fixed(Instant.parse("2026-07-20T12:30:45Z"), ZoneOffset.UTC)

    @Test
    fun rendererProducesProductionSizedBitmap() {
        val bitmap = ShareCardRenderer.render(model())

        assertEquals(1080, bitmap.width)
        assertEquals(1350, bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun cacheFileHasSafeNameAndNoBroadStoragePermission() {
        val file = ShareCardRenderer.renderToCacheFile(context, model(), clock)

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.name.matches(Regex("[a-zA-Z0-9._-]+")))
        assertFalse(file.name.contains(" "))
        assertTrue(file.parentFile?.name == "share-cards")
    }

    @Test
    fun cleanupRemovesPreviousShareCards() {
        ShareCardRenderer.renderToCacheFile(context, model(), clock)

        ShareCardRenderer.cleanupOldShareFiles(context)

        val remaining =
            context.cacheDir
                .resolve("share-cards")
                .listFiles()
                .orEmpty()
        assertTrue(remaining.none { it.extension == "png" })
    }

    private fun model(): ShareCardModel =
        ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = "2026-07-20",
            cardType = ShareCardType.DAILY_FIVE,
            visibleResultPattern = "Daily Five\nCompleted 5/5\nCurrent streak 3",
            hintsUsed = 0,
            currentStreak = 3,
            bestStreak = 5,
            forbiddenPayloads = listOf("flown", "known", "plant"),
        )
}
