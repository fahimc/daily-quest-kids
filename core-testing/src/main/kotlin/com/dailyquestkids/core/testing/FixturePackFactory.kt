package com.dailyquestkids.core.testing

import com.dailyquestkids.core.model.ConnectionGroup
import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordEntry
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.DailyPuzzleSet
import com.dailyquestkids.core.model.Difficulty
import com.dailyquestkids.core.model.Hint
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.ReviewMetadata
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SpellingWord
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle

object FixturePackFactory {
    fun oneDayPack(): PuzzlePack =
        PuzzlePack(
            schemaVersion = 1,
            seasonVersion = "sample-foundation",
            seasonStartDateUtc = "2026-09-01",
            checksum = "sample-not-production",
            days = listOf(dayOne()),
        )

    fun dayOne(): DailyPuzzleSet =
        DailyPuzzleSet(
            dayIndex = 0,
            globalDayNumber = 1,
            puzzles =
                listOf(
                    WordlyPuzzle(
                        id = "wordly-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("english:spelling", "year:3"),
                        hints =
                            listOf(
                                Hint(1, "It can mean to let someone else use something."),
                                Hint(2, "We can _____ our ideas in class."),
                                Hint(3, "The first letter is s."),
                            ),
                        review = automatedOnlyReview(),
                        solution = "share",
                        validGuesses = listOf("share", "shape", "shale"),
                        definition = "To let someone else use or enjoy something.",
                        exampleSentence = "We can share the crayons.",
                    ),
                    SpellingBeePuzzle(
                        id = "spelling-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("english:vocabulary", "year:3"),
                        hints =
                            listOf(
                                Hint(1, "Several words start with p."),
                                Hint(2, "One answer has five letters."),
                                Hint(3, "A small green plant part is a leaf."),
                            ),
                        review = automatedOnlyReview(),
                        letters = listOf('p', 'l', 'a', 'n', 't', 'e', 'r'),
                        centreLetter = 'a',
                        targetWords =
                            listOf(
                                SpellingWord("plant", "A living thing that grows in soil."),
                                SpellingWord("plan", "An idea for what to do next."),
                                SpellingWord("part", "One piece of a whole."),
                                SpellingWord("panel", "A flat part of a door or wall."),
                                SpellingWord("planet", "A world that moves around a star."),
                            ),
                    ),
                    CrosswordPuzzle(
                        id = "crossword-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("english:clues", "science:plants"),
                        hints =
                            listOf(
                                Hint(1, "Think about living things."),
                                Hint(2, "A tree has one."),
                                Hint(3, "The answer starts with l."),
                            ),
                        review = automatedOnlyReview(),
                        width = 7,
                        height = 7,
                        entries =
                            listOf(
                                CrosswordEntry("leaf", "A flat green part of a plant", 0, 0, CrosswordDirection.ACROSS),
                                CrosswordEntry("stem", "Part of a plant that holds it up", 1, 0, CrosswordDirection.ACROSS),
                                CrosswordEntry("seed", "A tiny plant can grow from this", 2, 0, CrosswordDirection.ACROSS),
                            ),
                    ),
                    SudokuPuzzle(
                        id = "sudoku-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("maths:logic", "year:3"),
                        hints =
                            listOf(
                                Hint(1, "Look for a row with only one gap."),
                                Hint(2, "The missing number cannot already be in that row."),
                                Hint(3, "The top-left region needs a 3."),
                            ),
                        review = automatedOnlyReview(),
                        givens =
                            listOf(
                                0,
                                2,
                                3,
                                4,
                                5,
                                6,
                                4,
                                5,
                                6,
                                1,
                                2,
                                3,
                                2,
                                3,
                                4,
                                5,
                                6,
                                1,
                                5,
                                6,
                                1,
                                2,
                                3,
                                4,
                                3,
                                4,
                                5,
                                6,
                                1,
                                2,
                                6,
                                1,
                                2,
                                3,
                                4,
                                5,
                            ),
                        solution =
                            listOf(
                                1,
                                2,
                                3,
                                4,
                                5,
                                6,
                                4,
                                5,
                                6,
                                1,
                                2,
                                3,
                                2,
                                3,
                                4,
                                5,
                                6,
                                1,
                                5,
                                6,
                                1,
                                2,
                                3,
                                4,
                                3,
                                4,
                                5,
                                6,
                                1,
                                2,
                                6,
                                1,
                                2,
                                3,
                                4,
                                5,
                            ),
                    ),
                    ConnectionsPuzzle(
                        id = "connections-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("english:vocabulary", "reasoning:classification"),
                        hints =
                            listOf(
                                Hint(1, "One group is about things in the sky."),
                                Hint(2, "moon and star belong together."),
                                Hint(3, "A group title is Sky objects."),
                            ),
                        review = automatedOnlyReview(),
                        groups =
                            listOf(
                                ConnectionGroup("Sky objects", listOf("moon", "star", "cloud", "sun"), "Things you can see in the sky."),
                                ConnectionGroup("School items", listOf("desk", "book", "pencil", "ruler"), "Useful things in a classroom."),
                                ConnectionGroup("Garden life", listOf("leaf", "seed", "flower", "grass"), "Things that grow outside."),
                                ConnectionGroup("Movement", listOf("jump", "skip", "walk", "climb"), "Ways people can move."),
                            ),
                    ),
                ),
        )

    private fun automatedOnlyReview(): ReviewMetadata =
        ReviewMetadata(
            automatedReviewPassed = true,
            humanReviewed = false,
            notes = "Automated fixture review only. Human review is required before production release.",
        )
}
