package com.dailyquestkids.core.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SeasonCalendarTest {
    @Test
    fun indexZeroMapsToDayOne() {
        val calendar = SeasonCalendar("2026-09-01", fixedClock("2026-09-01T00:01:00Z"))

        assertEquals(0, calendar.currentDayIndex())
        assertEquals(SeasonDayState.Active(0, 1), calendar.stateFor(0))
    }

    @Test
    fun leapDayAndMonthBoundariesUseUtcCalendarDays() {
        val calendar = SeasonCalendar("2028-02-28", fixedClock("2028-03-01T00:00:00Z"))

        assertEquals(2, calendar.currentDayIndex())
    }

    @Test
    fun dayThreeHundredSixtyFiveEndsSeason() {
        val calendar = SeasonCalendar("2026-09-01", fixedClock("2027-09-01T00:00:00Z"))

        assertEquals(365, calendar.currentDayIndex())
        assertEquals(SeasonDayState.SeasonComplete, calendar.stateFor(365))
    }

    private fun fixedClock(isoInstant: String): Clock = Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC)
}
