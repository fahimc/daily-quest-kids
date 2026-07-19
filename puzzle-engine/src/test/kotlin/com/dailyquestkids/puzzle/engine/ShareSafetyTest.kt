package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSafetyTest {
    @Test
    fun safeResultPatternDoesNotLeakAnswer() {
        val model =
            ShareCardModel(
                brand = "Daily Quest Kids",
                utcDate = "2026-09-01",
                cardType = ShareCardType.INDIVIDUAL_RESULT,
                visibleResultPattern = "green-yellow-grey-grey-green",
                hintsUsed = 1,
                currentStreak = 2,
                bestStreak = 3,
                forbiddenPayloads = listOf("plant"),
            )

        assertFalse(ShareSafety.leaksForbiddenPayload(model))
    }

    @Test
    fun visibleAnswerIsDetectedAsLeak() {
        val model =
            ShareCardModel(
                brand = "Daily Quest Kids",
                utcDate = "2026-09-01",
                cardType = ShareCardType.INDIVIDUAL_RESULT,
                visibleResultPattern = "Solved plant in four",
                hintsUsed = 0,
                currentStreak = 1,
                bestStreak = 1,
                forbiddenPayloads = listOf("plant"),
            )

        assertTrue(ShareSafety.leaksForbiddenPayload(model))
    }
}
