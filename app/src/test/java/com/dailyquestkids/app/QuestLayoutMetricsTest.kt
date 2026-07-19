package com.dailyquestkids.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestLayoutMetricsTest {
    @Test
    fun metricsScaleAcrossCommonPhoneViewports() {
        val viewports =
            listOf(
                320f to 568f,
                360f to 640f,
                393f to 852f,
                412f to 915f,
                480f to 853f,
            )

        viewports.forEach { (width, height) ->
            val metrics = QuestLayoutMetrics.calculate(widthDp = width, heightDp = height)

            assertTrue("$width x $height scale should stay tappable", metrics.scale >= 0.72f)
            assertTrue("$width x $height scale should not overgrow", metrics.scale <= 1.16f)
            assertTrue("$width x $height padding should leave card room", metrics.pagePadding.value <= width * 0.08f)
            assertTrue("$width x $height card corners should remain visible", metrics.cardCorner.value >= 15f)
        }
    }

    @Test
    fun compactFlagOnlyAppliesToConstrainedPhones() {
        assertTrue(QuestLayoutMetrics.calculate(widthDp = 320f, heightDp = 568f).compact)
        assertFalse(QuestLayoutMetrics.calculate(widthDp = 393f, heightDp = 852f).compact)
    }
}
