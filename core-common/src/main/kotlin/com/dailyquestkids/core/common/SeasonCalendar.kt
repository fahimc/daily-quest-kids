package com.dailyquestkids.core.common

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class SeasonCalendar(
    seasonStartDateUtc: String,
    private val clock: Clock,
) {
    private val startDate = LocalDate.parse(seasonStartDateUtc)

    fun currentDayIndex(): Int {
        val todayUtc = LocalDate.now(clock.withZone(ZoneOffset.UTC))
        return ChronoUnit.DAYS.between(startDate, todayUtc).toInt()
    }

    fun dateForDayIndex(dayIndex: Int): LocalDate = startDate.plusDays(dayIndex.toLong())

    fun stateFor(
        dayIndex: Int,
        seasonLength: Int = 365,
    ): SeasonDayState =
        when {
            dayIndex < 0 -> SeasonDayState.BeforeSeason
            dayIndex >= seasonLength -> SeasonDayState.SeasonComplete
            else -> SeasonDayState.Active(dayIndex = dayIndex, globalDayNumber = dayIndex + 1)
        }
}

sealed interface SeasonDayState {
    data object BeforeSeason : SeasonDayState

    data object SeasonComplete : SeasonDayState

    data class Active(
        val dayIndex: Int,
        val globalDayNumber: Int,
    ) : SeasonDayState
}
