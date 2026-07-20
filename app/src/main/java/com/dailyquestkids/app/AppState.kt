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
    val failedPuzzleIds: Set<String> = emptySet(),
    val dailyFiveCompletedDays: Set<Int> = emptySet(),
    val hintsUsedByPuzzle: Map<String, Int> = emptyMap(),
) {
    fun statusFor(puzzleId: String): PuzzleStatus =
        when {
            puzzleId in completedPuzzleIds -> PuzzleStatus.COMPLETED
            puzzleId in failedPuzzleIds -> PuzzleStatus.FAILED
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
    val longestHistoricalStreak: Int,
    val cards: List<QuestCardUiState>,
    val categoryStreaks: List<CategoryStreakUiState>,
    val historyDays: List<HistoryDayUiState>,
    val achievements: List<AchievementBadgeUiState>,
    val message: String,
    val comebackMessage: String,
) {
    val isDailyFiveComplete: Boolean = completedCount == cards.size && cards.isNotEmpty()
}

data class CategoryStreakUiState(
    val category: PuzzleCategory,
    val title: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalSolved: Int,
    val hintsUsed: Int,
    val hintFreeCompletions: Int,
    val lastSolvedDayIndex: Int?,
)

data class HistoryDayUiState(
    val dayIndex: Int,
    val globalDayNumber: Int,
    val label: String,
    val completedCount: Int,
    val totalCount: Int,
    val state: HistoryDayState,
    val isToday: Boolean,
)

enum class HistoryDayState {
    NO_ACTIVITY,
    PARTIAL,
    PERFECT,
    FUTURE,
}

data class AchievementBadgeUiState(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    val progressText: String,
    val category: PuzzleCategory? = null,
)

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
