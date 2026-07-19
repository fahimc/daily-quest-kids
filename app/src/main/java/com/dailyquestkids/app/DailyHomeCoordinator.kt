package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.common.SeasonDayState
import com.dailyquestkids.core.common.StreakEngine
import com.dailyquestkids.core.data.SamplePackRepository
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
        val cards = dailySet.puzzles.map { puzzle -> cardState(puzzle, progress) }
        val completedCount = cards.count { it.status == PuzzleStatus.COMPLETED }
        val dailyFiveProgress = dailyFiveProgress(progress)

        return DailyHomeUiState(
            date = calendar.dateForDayIndex(activeDayIndex),
            dayState = dayState,
            globalDayNumber = dailySet.globalDayNumber,
            completedCount = completedCount,
            currentDailyFiveStreak = dailyFiveProgress.currentStreak,
            bestDailyFiveStreak = dailyFiveProgress.bestStreak,
            perfectDayCount = dailyFiveProgress.perfectDayCount,
            cards = cards,
            message = messageFor(dayState, completedCount, rawDayIndex, progress.greatestDayObserved),
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
    ): QuestCardUiState {
        val status = progress.statusFor(puzzle.id)
        return QuestCardUiState(
            puzzle = puzzle,
            category = puzzle.category,
            title = puzzle.category.displayName,
            description = puzzle.category.destinationName,
            difficulty = puzzle.difficulty,
            status = status,
            categoryStreak = categoryStreak(puzzle.category, progress),
            hintCount = puzzle.hints.size,
            actionLabel = actionLabel(status),
        )
    }

    private fun categoryStreak(
        category: PuzzleCategory,
        progress: StoredProgress,
    ): Int =
        packRepository.pack.days.count { day ->
            val puzzle = day.puzzles.firstOrNull { it.category == category }
            puzzle?.id in progress.completedPuzzleIds
        }

    private fun actionLabel(status: PuzzleStatus): String =
        when (status) {
            PuzzleStatus.NOT_STARTED -> "Play"
            PuzzleStatus.IN_PROGRESS -> "Continue"
            PuzzleStatus.COMPLETED -> "View Result"
            PuzzleStatus.FAILED -> "View"
            PuzzleStatus.REVEALED -> "Review"
            PuzzleStatus.LOCKED -> "Locked"
        }

    private fun dailyFiveProgress(progress: StoredProgress): DailyFiveProgress =
        progress.dailyFiveCompletedDays
            .sorted()
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
