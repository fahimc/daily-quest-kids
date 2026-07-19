package com.dailyquestkids.app

import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AppSmokeTest {
    @Test
    fun samplePackLoadsDayOneForSeasonStart() {
        val repository = SamplePackRepository()
        val clock = Clock.fixed(Instant.parse("2026-07-19T12:00:00Z"), ZoneOffset.UTC)
        val calendar = SeasonCalendar(repository.pack.seasonStartDateUtc, clock)

        val day = repository.dailySetFor(calendar.currentDayIndex())

        assertEquals(5, day.puzzles.size)
    }
}
