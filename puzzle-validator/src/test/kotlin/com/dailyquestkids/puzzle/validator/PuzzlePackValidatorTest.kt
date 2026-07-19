package com.dailyquestkids.puzzle.validator

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.Hint
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.WordlyPuzzle
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
    fun unsafeWordsAreRejected() {
        val original = FixturePackFactory.oneDayPack()
        val day = original.days.single()
        val connections = day.puzzles.filterIsInstance<ConnectionsPuzzle>().single()
        val unsafeConnections =
            connections.copy(
                groups =
                    connections.groups.mapIndexed { index, group ->
                        if (index == 0) group.copy(words = listOf("war", "star", "cloud", "sun")) else group
                    },
            )
        val broken =
            original.copy(
                days =
                    listOf(
                        day.copy(
                            puzzles =
                                day.puzzles.map { puzzle ->
                                    if (puzzle.id == connections.id) unsafeConnections else puzzle
                                },
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
    }

    @Test
    fun hintOrderMustStartAtOneAndBeSequential() {
        val original = FixturePackFactory.oneDayPack()
        val day = original.days.single()
        val wordly = day.puzzles.filterIsInstance<WordlyPuzzle>().single()
        val brokenWordly =
            wordly.copy(
                hints =
                    listOf(
                        Hint(2, "Starts late."),
                        Hint(3, "Still late."),
                    ),
            )
        val broken =
            original.copy(
                days =
                    listOf(
                        day.copy(
                            puzzles =
                                day.puzzles.map { puzzle ->
                                    if (puzzle.id == wordly.id) brokenWordly else puzzle
                                },
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
    }

    @Test
    fun dayIndexesMustBeSequential() {
        val original = FixturePackFactory.phasePreviewPack(dayCount = 2)
        val broken =
            original.copy(
                days =
                    listOf(
                        original.days.first(),
                        original.days.last().copy(dayIndex = 4),
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

    @Test
    fun productionModeRequiresHumanReview() {
        val report = validator.validate(FixturePackFactory.oneDayPack(), requireFullSeason = true)

        assertFalse(report.passed)
        assertTrue(report.errors.any { it.contains("requires human review") })
    }

    @Test
    fun phasePreviewIncludesAtLeastTwentyReviewedWordlyFixtures() {
        val wordlyPuzzles =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<WordlyPuzzle>().single() }

        assertTrue(wordlyPuzzles.size >= 20)
        assertTrue(wordlyPuzzles.all { it.review.humanReviewed })
        assertTrue(wordlyPuzzles.map { it.solution }.toSet().size >= 20)
    }
}
