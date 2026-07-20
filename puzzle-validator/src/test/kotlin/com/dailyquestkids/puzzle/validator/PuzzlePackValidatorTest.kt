package com.dailyquestkids.puzzle.validator

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordEntry
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.Hint
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.SpellingBeePuzzle
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

    @Test
    fun phasePreviewIncludesAtLeastTwentyReviewedSpellingFixtures() {
        val spellingPuzzles =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<SpellingBeePuzzle>().single() }

        assertTrue(spellingPuzzles.size >= 20)
        assertTrue(spellingPuzzles.all { it.review.humanReviewed })
        assertTrue(spellingPuzzles.map { it.letters.joinToString("") }.toSet().size >= 20)
        assertTrue(spellingPuzzles.all { it.targetWords.size in 8..24 })
    }

    @Test
    fun phasePreviewIncludesAtLeastTwentyReviewedCrosswordFixtures() {
        val crosswordPuzzles =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<CrosswordPuzzle>().single() }

        assertTrue(crosswordPuzzles.size >= 20)
        assertTrue(crosswordPuzzles.all { it.review.humanReviewed })
        assertTrue(crosswordPuzzles.map { it.entries.first().answer }.toSet().size >= 20)
        assertTrue(crosswordPuzzles.all { it.width == 7 && it.height == 7 })
        assertTrue(crosswordPuzzles.all { it.entries.size in 6..14 })
    }

    @Test
    fun crosswordCrossingConflictsAreRejected() {
        val original = FixturePackFactory.oneDayPack()
        val day = original.days.single()
        val crossword = day.puzzles.filterIsInstance<CrosswordPuzzle>().single()
        val brokenCrossword =
            crossword.copy(
                entries =
                    crossword.entries.mapIndexed { index, entry ->
                        if (index == 1) {
                            entry.copy(answer = "ant", clue = "A tiny insect that can live in a colony.")
                        } else {
                            entry
                        }
                    },
            )
        val broken =
            original.copy(
                days =
                    listOf(
                        day.copy(
                            puzzles =
                                day.puzzles.map { puzzle ->
                                    if (puzzle.id == crossword.id) brokenCrossword else puzzle
                                },
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
        assertTrue(report.errors.any { it.contains("crossing conflict") })
    }

    @Test
    fun disconnectedCrosswordEntriesAreRejected() {
        val original = FixturePackFactory.oneDayPack()
        val day = original.days.single()
        val crossword = day.puzzles.filterIsInstance<CrosswordPuzzle>().single()
        val brokenCrossword =
            crossword.copy(
                entries =
                    listOf(
                        CrosswordEntry(
                            answer = "flowers",
                            clue = "Colourful parts of plants that may smell sweet.",
                            row = 3,
                            column = 0,
                            direction = CrosswordDirection.ACROSS,
                        ),
                        CrosswordEntry("cat", "A small pet with whiskers.", 0, 0, CrosswordDirection.DOWN),
                        CrosswordEntry("dog", "A friendly pet that may bark.", 0, 1, CrosswordDirection.DOWN),
                        CrosswordEntry("hen", "A farm bird that can lay eggs.", 0, 2, CrosswordDirection.DOWN),
                        CrosswordEntry("ink", "A liquid used for writing.", 0, 3, CrosswordDirection.DOWN),
                        CrosswordEntry("log", "A piece of wood from a tree.", 0, 4, CrosswordDirection.DOWN),
                    ),
            )
        val broken =
            original.copy(
                days =
                    listOf(
                        day.copy(
                            puzzles =
                                day.puzzles.map { puzzle ->
                                    if (puzzle.id == crossword.id) brokenCrossword else puzzle
                                },
                        ),
                    ),
            )

        val report = validator.validate(broken)

        assertFalse(report.passed)
        assertTrue(report.errors.any { it.contains("must connect") })
    }
}
