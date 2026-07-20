package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.common.SeasonDayState
import com.dailyquestkids.core.common.StreakEngine
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.CategoryProgress
import com.dailyquestkids.core.model.DailyFiveProgress
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus

class DailyHomeCoordinator(
    private val packRepository: SamplePackRepository,
    private val calendar: SeasonCalendar,
) {
    fun homeState(progress: StoredProgress): DailyHomeUiState {
        val rawDayIndex = calendar.currentDayIndex()
        val dayState = calendar.stateFor(rawDayIndex, seasonLength = packRepository.pack.days.size)
        val activeDayIndex = (dayState as? SeasonDayState.Active)?.dayIndex ?: 0
        val dailySet = packRepository.dailySetFor(activeDayIndex)
        val dailyFiveProgress = dailyFiveProgress(progress, activeDayIndex)
        val categoryStreaks = categoryStreaks(progress, activeDayIndex)
        val categoryByType = categoryStreaks.associateBy { it.category }
        val cards = dailySet.puzzles.map { puzzle -> cardState(puzzle, progress, categoryByType) }
        val completedCount = cards.count { it.status == PuzzleStatus.COMPLETED }

        return DailyHomeUiState(
            date = calendar.dateForDayIndex(activeDayIndex),
            dayState = dayState,
            globalDayNumber = dailySet.globalDayNumber,
            completedCount = completedCount,
            currentDailyFiveStreak = dailyFiveProgress.currentStreak,
            bestDailyFiveStreak = dailyFiveProgress.bestStreak,
            perfectDayCount = dailyFiveProgress.perfectDayCount,
            longestHistoricalStreak = dailyFiveProgress.longestHistoricalStreak,
            cards = cards,
            categoryStreaks = categoryStreaks,
            historyDays = historyDays(activeDayIndex, progress),
            achievements = achievements(progress, dailyFiveProgress, categoryStreaks),
            message = messageFor(dayState, completedCount, rawDayIndex, progress.greatestDayObserved),
            comebackMessage = comebackMessage(activeDayIndex, progress),
        )
    }

    fun puzzleById(puzzleId: String): Puzzle? =
        packRepository.pack.days
            .flatMap { it.puzzles }
            .firstOrNull { it.id == puzzleId }

    fun currentPuzzleIds(): Set<String> {
        val dayIndex = calendar.currentDayIndex().coerceIn(0, packRepository.pack.days.lastIndex)
        return packRepository
            .dailySetFor(dayIndex)
            .puzzles
            .map { it.id }
            .toSet()
    }

    private fun cardState(
        puzzle: Puzzle,
        progress: StoredProgress,
        categoryByType: Map<PuzzleCategory, CategoryStreakUiState>,
    ): QuestCardUiState {
        val status = progress.statusFor(puzzle.id)
        return QuestCardUiState(
            puzzle = puzzle,
            category = puzzle.category,
            title = puzzle.category.displayName,
            description = puzzle.category.destinationName,
            difficulty = puzzle.difficulty,
            status = status,
            categoryStreak = categoryByType[puzzle.category]?.currentStreak ?: 0,
            hintCount = puzzle.hints.size,
            actionLabel = actionLabel(status),
        )
    }

    private fun categoryStreaks(
        progress: StoredProgress,
        activeDayIndex: Int,
    ): List<CategoryStreakUiState> =
        PuzzleCategory.entries.map { category ->
            packRepository.pack.days
                .fold(emptyCategoryProgress(category)) { current, day ->
                    val puzzle = day.puzzles.firstOrNull { it.category == category }
                    if (puzzle?.id in progress.completedPuzzleIds) {
                        StreakEngine.recordCategoryCompletion(
                            progress = current,
                            dayIndex = day.dayIndex,
                            hintsUsedForPuzzle = progress.hintsUsedByPuzzle[puzzle?.id].coerceHints(),
                        )
                    } else {
                        current
                    }
                }.toUiState(activeDayIndex)
        }

    private fun emptyCategoryProgress(category: PuzzleCategory): CategoryProgress =
        CategoryProgress(
            category = category,
            currentStreak = 0,
            bestStreak = 0,
            totalSolved = 0,
            lastSolvedDayIndex = null,
            hintsUsed = 0,
            hintFreeCompletions = 0,
        )

    private fun CategoryProgress.toUiState(activeDayIndex: Int): CategoryStreakUiState =
        CategoryStreakUiState(
            category = category,
            title = category.displayName,
            currentStreak = currentStreak.takeIf { canContinueStreak(lastSolvedDayIndex, activeDayIndex) } ?: 0,
            bestStreak = bestStreak,
            totalSolved = totalSolved,
            hintsUsed = hintsUsed,
            hintFreeCompletions = hintFreeCompletions,
            lastSolvedDayIndex = lastSolvedDayIndex,
        )

    private fun Int?.coerceHints(): Int = this?.coerceAtLeast(0) ?: 0

    private fun actionLabel(status: PuzzleStatus): String =
        when (status) {
            PuzzleStatus.NOT_STARTED -> "Play"
            PuzzleStatus.IN_PROGRESS -> "Continue"
            PuzzleStatus.COMPLETED -> "View Result"
            PuzzleStatus.FAILED -> "View"
            PuzzleStatus.REVEALED -> "Review"
            PuzzleStatus.LOCKED -> "Locked"
        }

    private fun dailyFiveProgress(
        progress: StoredProgress,
        activeDayIndex: Int,
    ): DailyFiveProgress {
        val completedDays = progress.dailyFiveCompletedDays.sorted()
        val rawProgress =
            completedDays
                .fold(
                    DailyFiveProgress(
                        currentStreak = 0,
                        bestStreak = 0,
                        perfectDayCount = 0,
                        longestHistoricalStreak = 0,
                        completedDayIndices = emptySet(),
                    ),
                ) { current, dayIndex ->
                    StreakEngine.recordDailyFiveCompletion(current, dayIndex)
                }
        return rawProgress.copy(
            currentStreak =
                rawProgress.currentStreak
                    .takeIf { canContinueStreak(completedDays.lastOrNull(), activeDayIndex) }
                    ?: 0,
        )
    }

    private fun canContinueStreak(
        lastCompletedDayIndex: Int?,
        activeDayIndex: Int,
    ): Boolean = lastCompletedDayIndex == activeDayIndex || lastCompletedDayIndex == activeDayIndex - 1

    private fun historyDays(
        activeDayIndex: Int,
        progress: StoredProgress,
    ): List<HistoryDayUiState> {
        val start = (activeDayIndex - 3).coerceAtLeast(0)
        val end = (start + 6).coerceAtMost(packRepository.pack.days.lastIndex)
        return (start..end).map { dayIndex ->
            val day = packRepository.dailySetFor(dayIndex)
            val completedCount = day.puzzles.count { it.id in progress.completedPuzzleIds }
            HistoryDayUiState(
                dayIndex = dayIndex,
                globalDayNumber = day.globalDayNumber,
                label = "Day ${day.globalDayNumber}",
                completedCount = completedCount,
                totalCount = day.puzzles.size,
                state = historyState(dayIndex, activeDayIndex, completedCount, day.puzzles.size, progress),
                isToday = dayIndex == activeDayIndex,
            )
        }
    }

    private fun historyState(
        dayIndex: Int,
        activeDayIndex: Int,
        completedCount: Int,
        puzzleCount: Int,
        progress: StoredProgress,
    ): HistoryDayState =
        when {
            dayIndex > activeDayIndex -> HistoryDayState.FUTURE
            completedCount == puzzleCount || dayIndex in progress.dailyFiveCompletedDays -> HistoryDayState.PERFECT
            completedCount > 0 -> HistoryDayState.PARTIAL
            else -> HistoryDayState.NO_ACTIVITY
        }

    private fun achievements(
        progress: StoredProgress,
        dailyFiveProgress: DailyFiveProgress,
        categoryStreaks: List<CategoryStreakUiState>,
    ): List<AchievementBadgeUiState> {
        val totalSolved = progress.completedPuzzleIds.size
        val hintFreeCompletions = categoryStreaks.sumOf { it.hintFreeCompletions }
        val dailyBadges =
            listOf(
                AchievementSpec("first-step", "First Step", "Complete any puzzle.", 1) to totalSolved,
                AchievementSpec(
                    "daily-five",
                    "Daily Five",
                    "Complete all five puzzles in one day.",
                    1,
                ) to dailyFiveProgress.perfectDayCount,
                AchievementSpec(
                    "three-day-streak",
                    "Three-Day Trail",
                    "Complete the Daily Five three days in a row.",
                    3,
                ) to dailyFiveProgress.bestStreak,
                AchievementSpec("five-perfect", "Five Perfect Days", "Complete five Daily Five days.", 5) to
                    dailyFiveProgress.perfectDayCount,
                AchievementSpec("ten-puzzles", "Puzzle Explorer", "Complete ten puzzles.", 10) to totalSolved,
                AchievementSpec("hint-free", "Careful Thinker", "Complete three puzzles without hints.", 3) to hintFreeCompletions,
            ).map { (spec, progress) ->
                achievement(spec = spec, progress = progress)
            }
        val categoryBadges =
            categoryStreaks.map { category ->
                achievement(
                    spec =
                        AchievementSpec(
                            id = "first-${category.category.name.lowercase()}",
                            title = "${category.title} Starter",
                            description = "Complete one ${category.title} puzzle.",
                            target = 1,
                            category = category.category,
                        ),
                    progress = category.totalSolved,
                )
            }
        return dailyBadges + categoryBadges
    }

    private fun achievement(
        spec: AchievementSpec,
        progress: Int,
    ): AchievementBadgeUiState =
        AchievementBadgeUiState(
            id = spec.id,
            title = spec.title,
            description = spec.description,
            unlocked = progress >= spec.target,
            progressText = "${progress.coerceAtMost(spec.target)}/${spec.target}",
            category = spec.category,
        )

    private fun comebackMessage(
        activeDayIndex: Int,
        progress: StoredProgress,
    ): String {
        val lastPerfectDay = progress.dailyFiveCompletedDays.maxOrNull()
        return when {
            lastPerfectDay == null -> "A new streak can begin today."
            lastPerfectDay == activeDayIndex -> "You completed today's Daily Five."
            lastPerfectDay == activeDayIndex - 1 -> "A new Daily Five can keep your streak growing."
            lastPerfectDay < activeDayIndex - 1 -> "A new streak can begin today."
            else -> "Progress is safe on this device."
        }
    }

    private fun messageFor(
        dayState: SeasonDayState,
        completedCount: Int,
        rawDayIndex: Int,
        greatestDayObserved: Int,
    ): String =
        if (rawDayIndex < greatestDayObserved) {
            "Your device date looks earlier than before. Progress is safe, and streaks will continue on a later day."
        } else {
            when (dayState) {
                SeasonDayState.BeforeSeason -> "This season starts soon."
                SeasonDayState.SeasonComplete -> "Season complete. Great thinking."
                is SeasonDayState.Active ->
                    if (completedCount == PuzzleCategory.entries.size) {
                        "Daily Five complete. New puzzles arrive tomorrow."
                    } else {
                        "Ready for another puzzle quest?"
                    }
            }
        }
}

private data class AchievementSpec(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val category: PuzzleCategory? = null,
)
