package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
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

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
