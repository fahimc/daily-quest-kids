package com.dailyquestkids.puzzle.validator

import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzlePackValidatorTest {
    private val validator = PuzzlePackValidator()

    @Test
    fun validSampleDayPassesDevelopmentValidation() {
        val report = validator.validate(FixturePackFactory.oneDayPack())

        assertTrue(report.errors.joinToString(), report.passed)
    }

    @Test
    fun missingCategoryIsRejected() {
        val original = FixturePackFactory.oneDayPack()
        val broken =
            original.copy(
                days =
                    listOf(
                        original.days.single().copy(
                            puzzles =
                                original.days
                                    .single()
                                    .puzzles
                                    .drop(1),
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
    }

    @Test
    fun duplicateIdsAreRejected() {
        val original = FixturePackFactory.oneDayPack()
        val duplicatePuzzle =
            original.days
                .single()
                .puzzles
                .first()
        val broken =
            original.copy(
                days =
                    listOf(
                        DailyPuzzleSet(
                            dayIndex = 0,
                            globalDayNumber = 1,
                            puzzles = original.days.single().puzzles + duplicatePuzzle,
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
    }

    @Test
    fun fullSeasonModeRequiresExactlyThreeHundredSixtyFiveDays() {
        val report =
            validator.validate(
                PuzzlePack(
                    schemaVersion = 1,
                    seasonVersion = "sample",
                    seasonStartDateUtc = "2026-09-01",
                    checksum = "x",
                    days = emptyList(),
                ),
                requireFullSeason = true,
            )

        assertFalse(report.passed)
    }
}
