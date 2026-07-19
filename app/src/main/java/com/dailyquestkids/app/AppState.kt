package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonDayState
import com.dailyquestkids.core.model.Difficulty
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import java.time.LocalDate

data class QuestSettings(
    val onboardingComplete: Boolean = false,
    val soundEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val highContrast: Boolean = false,
    val largePuzzleText: Boolean = false,
    val optionalTimer: Boolean = false,
    val mistakeChecking: Boolean = true,
)

data class StoredProgress(
    val greatestDayObserved: Int = 0,
    val startedPuzzleIds: Set<String> = emptySet(),
    val completedPuzzleIds: Set<String> = emptySet(),
    val dailyFiveCompletedDays: Set<Int> = emptySet(),
) {
    fun statusFor(puzzleId: String): PuzzleStatus =
        when {
            puzzleId in completedPuzzleIds -> PuzzleStatus.COMPLETED
            puzzleId in startedPuzzleIds -> PuzzleStatus.IN_PROGRESS
            else -> PuzzleStatus.NOT_STARTED
        }
}

data class DailyHomeUiState(
    val date: LocalDate,
    val dayState: SeasonDayState,
    val globalDayNumber: Int,
    val completedCount: Int,
    val currentDailyFiveStreak: Int,
    val bestDailyFiveStreak: Int,
    val perfectDayCount: Int,
    val cards: List<QuestCardUiState>,
    val message: String,
) {
    val isDailyFiveComplete: Boolean = completedCount == cards.size && cards.isNotEmpty()
}

data class QuestCardUiState(
    val puzzle: Puzzle,
    val category: PuzzleCategory,
    val title: String,
    val description: String,
    val difficulty: Difficulty,
    val status: PuzzleStatus,
    val categoryStreak: Int,
    val hintCount: Int,
    val actionLabel: String,
)
