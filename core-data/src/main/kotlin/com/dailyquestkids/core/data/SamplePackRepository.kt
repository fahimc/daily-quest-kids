package com.dailyquestkids.core.data

import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.testing.FixturePackFactory

class SamplePackRepository {
    val pack: PuzzlePack = FixturePackFactory.oneDayPack()

    fun dailySetFor(dayIndex: Int): DailyPuzzleSet =
        pack.days.firstOrNull { it.dayIndex == dayIndex }
            ?: pack.days.first()
}
