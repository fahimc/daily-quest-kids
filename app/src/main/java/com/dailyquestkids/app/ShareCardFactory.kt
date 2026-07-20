package com.dailyquestkids.app

import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.ShareCardType

internal object ShareCardFactory {
    fun dailyFive(state: DailyHomeUiState): ShareCardModel =
        ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = state.date.toString(),
            cardType = ShareCardType.DAILY_FIVE,
            visibleResultPattern =
                listOf(
                    "Daily Five",
                    "Completed ${state.completedCount}/${state.cards.size}",
                    "Current streak ${state.currentDailyFiveStreak}",
                    "Best streak ${state.bestDailyFiveStreak}",
                ).joinToString(separator = "\n"),
            hintsUsed = 0,
            currentStreak = state.currentDailyFiveStreak,
            bestStreak = state.bestDailyFiveStreak,
            forbiddenPayloads = state.cards.map { it.puzzle.id },
        )

    fun streak(state: DailyHomeUiState): ShareCardModel =
        ShareCardModel(
            brand = "Daily Quest Kids",
            utcDate = state.date.toString(),
            cardType = ShareCardType.STREAK,
            visibleResultPattern =
                listOf(
                    "Puzzle streak",
                    "Current ${state.currentDailyFiveStreak}",
                    "Best ${state.bestDailyFiveStreak}",
                    "Perfect days ${state.perfectDayCount}",
                ).joinToString(separator = "\n"),
            hintsUsed = 0,
            currentStreak = state.currentDailyFiveStreak,
            bestStreak = state.bestDailyFiveStreak,
            forbiddenPayloads = state.cards.map { it.puzzle.id },
        )
}
