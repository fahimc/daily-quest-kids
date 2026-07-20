package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DailyHomeCoordinatorTest {
    private val repository = SamplePackRepository()
    private val calendar = SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock())
    private val coordinator = DailyHomeCoordinator(repository, calendar)

    @Test
    fun startedAndCompletedPuzzleIdsDriveCardState() {
        val firstPuzzle =
            repository.pack.days
                .first()
                .puzzles
                .first()
        val state =
            coordinator.homeState(
                StoredProgress(
                    startedPuzzleIds = setOf(firstPuzzle.id),
                    completedPuzzleIds = setOf(firstPuzzle.id),
                ),
            )

        assertEquals(1, state.completedCount)
        assertEquals(PuzzleStatus.COMPLETED, state.cards.first().status)
    }

    @Test
    fun allFiveCompletionsShowDailyFiveComplete() {
        val puzzleIds =
            repository.pack.days
                .first()
                .puzzles
                .map { it.id }
                .toSet()
        val state =
            coordinator.homeState(
                StoredProgress(
                    completedPuzzleIds = puzzleIds,
                    dailyFiveCompletedDays = setOf(0),
                ),
            )

        assertTrue(state.isDailyFiveComplete)
        assertEquals(1, state.currentDailyFiveStreak)
    }

    @Test
    fun clockRollbackShowsFriendlyMessageWithoutClearingProgress() {
        val state = coordinator.homeState(StoredProgress(greatestDayObserved = 2))

        assertTrue(state.message.contains("Progress is safe"))
    }

    @Test
    fun categoryStreaksTrackConsecutiveDaysAndHints() {
        val dayTwoCoordinator = coordinatorFor("2026-07-20T12:00:00Z")
        val wordlyDayZero = puzzleId(dayIndex = 0, PuzzleCategory.WORDLY)
        val wordlyDayOne = puzzleId(dayIndex = 1, PuzzleCategory.WORDLY)
        val state =
            dayTwoCoordinator.homeState(
                StoredProgress(
                    completedPuzzleIds = setOf(wordlyDayZero, wordlyDayOne),
                    hintsUsedByPuzzle = mapOf(wordlyDayZero to 2, wordlyDayOne to 0),
                ),
            )
        val wordly = state.categoryStreaks.single { it.category == PuzzleCategory.WORDLY }

        assertEquals(2, wordly.currentStreak)
        assertEquals(2, wordly.bestStreak)
        assertEquals(2, wordly.totalSolved)
        assertEquals(2, wordly.hintsUsed)
        assertEquals(1, wordly.hintFreeCompletions)
    }

    @Test
    fun currentStreakFallsToZeroAfterMissedDayButBestStays() {
        val dayFourCoordinator = coordinatorFor("2026-07-22T12:00:00Z")
        val completedIds =
            setOf(
                puzzleId(dayIndex = 0, PuzzleCategory.WORDLY),
                puzzleId(dayIndex = 1, PuzzleCategory.WORDLY),
            )
        val state =
            dayFourCoordinator.homeState(
                StoredProgress(
                    completedPuzzleIds = completedIds,
                    dailyFiveCompletedDays = setOf(0, 1),
                ),
            )
        val wordly = state.categoryStreaks.single { it.category == PuzzleCategory.WORDLY }

        assertEquals(0, state.currentDailyFiveStreak)
        assertEquals(2, state.bestDailyFiveStreak)
        assertEquals(0, wordly.currentStreak)
        assertEquals(2, wordly.bestStreak)
        assertTrue(state.comebackMessage.contains("new streak"))
    }

    @Test
    fun historyAndAchievementsReflectPerfectPartialFutureAndHintFreeProgress() {
        val dayTwoCoordinator = coordinatorFor("2026-07-20T12:00:00Z")
        val dayZeroIds = puzzleIdsForDay(0)
        val dayOneFirstPuzzle =
            repository.pack.days[1]
                .puzzles
                .first()
                .id
        val state =
            dayTwoCoordinator.homeState(
                StoredProgress(
                    completedPuzzleIds = dayZeroIds + dayOneFirstPuzzle,
                    dailyFiveCompletedDays = setOf(0),
                    hintsUsedByPuzzle = dayZeroIds.associateWith { 0 } + (dayOneFirstPuzzle to 1),
                ),
            )

        assertEquals(HistoryDayState.PERFECT, state.historyDays.single { it.dayIndex == 0 }.state)
        assertEquals(HistoryDayState.PARTIAL, state.historyDays.single { it.dayIndex == 1 }.state)
        assertTrue(state.historyDays.any { it.state == HistoryDayState.FUTURE })
        assertTrue(state.achievements.single { it.id == "daily-five" }.unlocked)
        assertTrue(state.achievements.single { it.id == "hint-free" }.unlocked)
        assertEquals("1/5", state.achievements.single { it.id == "five-perfect" }.progressText)
    }

    private fun coordinatorFor(instant: String): DailyHomeCoordinator =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)),
        )

    private fun puzzleId(
        dayIndex: Int,
        category: PuzzleCategory,
    ): String =
        repository
            .pack
            .days[dayIndex]
            .puzzles
            .single { it.category == category }
            .id

    private fun puzzleIdsForDay(dayIndex: Int): Set<String> =
        repository
            .pack
            .days[dayIndex]
            .puzzles
            .map { it.id }
            .toSet()

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
