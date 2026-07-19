package com.dailyquestkids.core.common

import com.dailyquestkids.core.model.CategoryProgress
import com.dailyquestkids.core.model.DailyFiveProgress

object StreakEngine {
    fun recordCategoryCompletion(
        progress: CategoryProgress,
        dayIndex: Int,
        hintsUsedForPuzzle: Int,
    ): CategoryProgress {
        if (progress.lastSolvedDayIndex == dayIndex) {
            return progress
        }

        val nextCurrent =
            when (progress.lastSolvedDayIndex) {
                null -> 1
                dayIndex - 1 -> progress.currentStreak + 1
                else -> 1
            }
        val nextBest = maxOf(progress.bestStreak, nextCurrent)

        return progress.copy(
            currentStreak = nextCurrent,
            bestStreak = nextBest,
            totalSolved = progress.totalSolved + 1,
            lastSolvedDayIndex = dayIndex,
            hintsUsed = progress.hintsUsed + hintsUsedForPuzzle,
            hintFreeCompletions = progress.hintFreeCompletions + if (hintsUsedForPuzzle == 0) 1 else 0,
        )
    }

    fun recordDailyFiveCompletion(
        progress: DailyFiveProgress,
        dayIndex: Int,
    ): DailyFiveProgress {
        if (dayIndex in progress.completedDayIndices) {
            return progress
        }

        val previousDayCompleted = dayIndex - 1 in progress.completedDayIndices
        val nextCurrent = if (previousDayCompleted) progress.currentStreak + 1 else 1
        val nextBest = maxOf(progress.bestStreak, nextCurrent)

        return progress.copy(
            currentStreak = nextCurrent,
            bestStreak = nextBest,
            perfectDayCount = progress.perfectDayCount + 1,
            longestHistoricalStreak = maxOf(progress.longestHistoricalStreak, nextCurrent),
            completedDayIndices = progress.completedDayIndices + dayIndex,
        )
    }
}
