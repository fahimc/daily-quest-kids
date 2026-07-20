package com.dailyquestkids.app

import android.content.Context
import com.dailyquestkids.core.common.SeasonCalendar
import com.dailyquestkids.core.data.SamplePackRepository
import java.time.Clock

class DailyQuestContainer(
    context: Context,
    clock: Clock = Clock.systemUTC(),
) {
    private val applicationContext = context.applicationContext

    val packRepository = SamplePackRepository()
    val calendar = SeasonCalendar(packRepository.pack.seasonStartDateUtc, clock)
    val settingsStore = SettingsStore(applicationContext)
    val progressStore = ProgressStore(applicationContext)
    val wordlyProgressStore = WordlyProgressStore(applicationContext)
    val spellingProgressStore = SpellingProgressStore(applicationContext)
    val crosswordProgressStore = CrosswordProgressStore(applicationContext)
    val sudokuProgressStore = SudokuProgressStore(applicationContext)
    val connectionsProgressStore = ConnectionsProgressStore(applicationContext)
}
