package com.dailyquestkids.core.common

import com.dailyquestkids.core.model.CategoryProgress
import com.dailyquestkids.core.model.DailyFiveProgress
import com.dailyquestkids.core.model.PuzzleCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakEngineTest {
    @Test
    fun consecutiveCategoryDaysAdvanceCurrentAndBestStreaks() {
        val start =
            CategoryProgress(
                category = PuzzleCategory.WORDLY,
                currentStreak = 0,
                bestStreak = 0,
                totalSolved = 0,
                lastSolvedDayIndex = null,
                hintsUsed = 0,
                hintFreeCompletions = 0,
            )

        val first = StreakEngine.recordCategoryCompletion(start, dayIndex = 10, hintsUsedForPuzzle = 0)
        val second = StreakEngine.recordCategoryCompletion(first, dayIndex = 11, hintsUsedForPuzzle = 2)

        assertEquals(2, second.currentStreak)
        assertEquals(2, second.bestStreak)
        assertEquals(2, second.totalSolved)
        assertEquals(1, second.hintFreeCompletions)
    }

    @Test
    fun duplicateDailyFiveCompletionDoesNotAwardTwice() {
        val start =
            DailyFiveProgress(
                currentStreak = 0,
                bestStreak = 0,
                perfectDayCount = 0,
                longestHistoricalStreak = 0,
                completedDayIndices = emptySet(),
            )

        val first = StreakEngine.recordDailyFiveCompletion(start, dayIndex = 4)
        val duplicate = StreakEngine.recordDailyFiveCompletion(first, dayIndex = 4)

        assertEquals(first, duplicate)
    }
}
