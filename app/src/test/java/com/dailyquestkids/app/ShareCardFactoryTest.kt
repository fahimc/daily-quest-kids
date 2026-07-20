package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import com.dailyquestkids.puzzle.engine.ShareSafety
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ShareCardFactoryTest {
    private val repository = SamplePackRepository()
    private val coordinator =
        DailyHomeCoordinator(
            repository,
            SeasonCalendar(repository.pack.seasonStartDateUtc, fixedClock()),
        )

    @Test
    fun dailyFiveCardUsesAnswerSafePattern() {
        val state =
            coordinator.homeState(
                StoredProgress(
                    completedPuzzleIds =
                        repository.pack.days
                            .first()
                            .puzzles
                            .map { it.id }
                            .toSet(),
                    dailyFiveCompletedDays = setOf(0),
                ),
            )
        val model = ShareCardFactory.dailyFive(state)

        assertEquals("Daily Quest Kids", model.brand)
        assertFalse(ShareSafety.leaksForbiddenPayload(model))
        state.cards.forEach { card ->
            assertFalse(model.visibleResultPattern.contains(card.puzzle.id))
        }
    }

    @Test
    fun streakCardUsesGenericProgressOnly() {
        val state = coordinator.homeState(StoredProgress(dailyFiveCompletedDays = setOf(0, 1, 2)))
        val model = ShareCardFactory.streak(state)

        assertFalse(ShareSafety.leaksForbiddenPayload(model))
        assertFalse(model.visibleResultPattern.contains("wordly", ignoreCase = true))
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
}
