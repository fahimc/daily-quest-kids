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
            seasonStartDateUtc = SEASON_START_DATE,
            checksum = "sample-not-production",
            days = listOf(dayOne()),
        )

    fun phasePreviewPack(dayCount: Int = WORDLY_FIXTURE_COUNT): PuzzlePack =
        PuzzlePack(
            schemaVersion = 1,
            seasonVersion = "phase-preview",
            seasonStartDateUtc = SEASON_START_DATE,
            checksum = "preview-not-production",
            days = List(dayCount) { dayIndex -> day(dayIndex) },
        )

    fun dayOne(): DailyPuzzleSet = day(dayIndex = 0)

    private fun day(dayIndex: Int): DailyPuzzleSet =
        DailyPuzzleSet(
            dayIndex = dayIndex,
            globalDayNumber = dayIndex + 1,
            puzzles =
                listOf(
                    wordlyPuzzle(dayIndex),
                    SpellingBeePuzzle(
                        id = "spelling-${dayId(dayIndex)}",
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
                        id = "crossword-${dayId(dayIndex)}",
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
                        id = "sudoku-${dayId(dayIndex)}",
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
                        id = "connections-${dayId(dayIndex)}",
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

    private fun dayId(dayIndex: Int): String = (dayIndex + 1).toString().padStart(3, '0')

    private fun wordlyPuzzle(dayIndex: Int): WordlyPuzzle {
        val fixture = wordlyFixtures[dayIndex % wordlyFixtures.size]
        return WordlyPuzzle(
            id = "wordly-${dayId(dayIndex)}",
            difficulty = fixture.difficulty,
            curriculumTags = listOf("english:spelling", "english:vocabulary", "year:${fixture.yearGroup}"),
            hints = fixture.hints,
            review = humanReview(),
            solution = fixture.solution,
            validGuesses = wordlyGuessBank,
            definition = fixture.definition,
            exampleSentence = fixture.exampleSentence,
            morphologyNote = fixture.morphologyNote,
        )
    }

    private fun automatedOnlyReview(): ReviewMetadata =
        ReviewMetadata(
            automatedReviewPassed = true,
            humanReviewed = false,
            notes = "Automated fixture review only. Human review is required before production release.",
        )

    private fun humanReview(): ReviewMetadata =
        ReviewMetadata(
            automatedReviewPassed = true,
            humanReviewed = true,
            reviewer = "phase-6-fixture-review",
            notes = "Reviewed fixture for child-safe Wordly phase coverage.",
        )

    private const val SEASON_START_DATE = "2026-07-19"

    private const val WORDLY_FIXTURE_COUNT = 20

    private val wordlyFixtures =
        listOf(
            WordlyFixture(
                solution = "flown",
                definition = "Moved through the air or been carried by the wind.",
                exampleSentence = "The kite has flown high above the field.",
                morphologyNote = "Flown is the past participle of fly.",
                hints =
                    listOf(
                        Hint(1, "It describes something that has moved through the air."),
                        Hint(2, "A kite may have _____ high."),
                        Hint(3, "The first letter is f."),
                    ),
            ),
            WordlyFixture(
                solution = "share",
                definition = "To let someone else use or enjoy something.",
                exampleSentence = "We can share the crayons.",
                morphologyNote = "Share can be a verb or a noun.",
                hints =
                    listOf(
                        Hint(1, "It can mean to let someone else use something."),
                        Hint(2, "We can _____ our ideas in class."),
                        Hint(3, "The first letter is s."),
                    ),
            ),
            WordlyFixture(
                solution = "plant",
                definition = "A living thing that grows in soil.",
                exampleSentence = "The plant needs sunlight and water.",
                morphologyNote = "Plant can also mean to put seeds in soil.",
                hints =
                    listOf(
                        Hint(1, "It grows outside or in a pot."),
                        Hint(2, "It may have leaves and roots."),
                        Hint(3, "The first letter is p."),
                    ),
            ),
            WordlyFixture(
                solution = "cloud",
                definition = "A white or grey shape made of tiny drops in the sky.",
                exampleSentence = "A cloud drifted across the sun.",
                hints =
                    listOf(
                        Hint(1, "You can see it in the sky."),
                        Hint(2, "It may bring rain."),
                        Hint(3, "The first letter is c."),
                    ),
            ),
            WordlyFixture(
                solution = "bloom",
                definition = "A flower, or the action of opening into a flower.",
                exampleSentence = "The roses bloom in summer.",
                morphologyNote = "Bloom can be a noun or a verb.",
                hints =
                    listOf(
                        Hint(1, "It is linked to flowers."),
                        Hint(2, "A bud can _____ in spring."),
                        Hint(3, "The first letter is b."),
                    ),
            ),
            WordlyFixture(
                solution = "river",
                definition = "A long stream of water that flows across land.",
                exampleSentence = "The river curved around the hill.",
                hints =
                    listOf(
                        Hint(1, "It carries water."),
                        Hint(2, "It may run to the sea."),
                        Hint(3, "The first letter is r."),
                    ),
            ),
            WordlyFixture(
                solution = "smile",
                definition = "A happy look made by turning up the corners of your mouth.",
                exampleSentence = "Her smile showed she was pleased.",
                hints =
                    listOf(
                        Hint(1, "It shows a happy feeling."),
                        Hint(2, "You do it with your mouth."),
                        Hint(3, "The first letter is s."),
                    ),
            ),
            WordlyFixture(
                solution = "brave",
                definition = "Ready to face something difficult or scary.",
                exampleSentence = "The brave explorer crossed the bridge.",
                hints =
                    listOf(
                        Hint(1, "It describes courage."),
                        Hint(2, "A hero is often this."),
                        Hint(3, "The first letter is b."),
                    ),
            ),
            WordlyFixture(
                solution = "light",
                definition = "Brightness that lets you see things.",
                exampleSentence = "Morning light filled the room.",
                morphologyNote = "Light can also mean not heavy.",
                hints =
                    listOf(
                        Hint(1, "It helps you see."),
                        Hint(2, "The sun gives this."),
                        Hint(3, "The first letter is l."),
                    ),
            ),
            WordlyFixture(
                solution = "train",
                definition = "A line of carriages pulled along tracks.",
                exampleSentence = "The train stopped at the station.",
                morphologyNote = "Train can also mean to practise a skill.",
                hints =
                    listOf(
                        Hint(1, "It travels on tracks."),
                        Hint(2, "People wait for it at a station."),
                        Hint(3, "The first letter is t."),
                    ),
            ),
            WordlyFixture(
                solution = "bread",
                definition = "Food baked from flour, water and yeast.",
                exampleSentence = "We sliced the bread for lunch.",
                hints =
                    listOf(
                        Hint(1, "You can make sandwiches with it."),
                        Hint(2, "It is baked."),
                        Hint(3, "The first letter is b."),
                    ),
            ),
            WordlyFixture(
                solution = "house",
                definition = "A building where people live.",
                exampleSentence = "The house had a bright red door.",
                hints =
                    listOf(
                        Hint(1, "It is a kind of home."),
                        Hint(2, "It may have rooms, windows and doors."),
                        Hint(3, "The first letter is h."),
                    ),
            ),
            WordlyFixture(
                solution = "apple",
                definition = "A round fruit that can be red, green or yellow.",
                exampleSentence = "He packed an apple in his bag.",
                hints =
                    listOf(
                        Hint(1, "It is a fruit."),
                        Hint(2, "It grows on a tree."),
                        Hint(3, "The first letter is a."),
                    ),
            ),
            WordlyFixture(
                solution = "music",
                definition = "Sounds arranged with rhythm or melody.",
                exampleSentence = "The music made everyone clap.",
                hints =
                    listOf(
                        Hint(1, "You can listen to it."),
                        Hint(2, "It can have rhythm and melody."),
                        Hint(3, "The first letter is m."),
                    ),
            ),
            WordlyFixture(
                solution = "dance",
                definition = "To move your body to music.",
                exampleSentence = "They dance together after school.",
                hints =
                    listOf(
                        Hint(1, "It is a movement activity."),
                        Hint(2, "People often do it to music."),
                        Hint(3, "The first letter is d."),
                    ),
            ),
            WordlyFixture(
                solution = "clean",
                definition = "Free from dirt or mess.",
                exampleSentence = "The clean desk was ready for work.",
                hints =
                    listOf(
                        Hint(1, "It means not dirty."),
                        Hint(2, "You may make a room this after tidying."),
                        Hint(3, "The first letter is c."),
                    ),
            ),
            WordlyFixture(
                solution = "green",
                definition = "The colour of grass and many leaves.",
                exampleSentence = "The green frog sat by the pond.",
                hints =
                    listOf(
                        Hint(1, "It is a colour."),
                        Hint(2, "Grass is often this colour."),
                        Hint(3, "The first letter is g."),
                    ),
            ),
            WordlyFixture(
                solution = "beach",
                definition = "Land beside the sea, often covered with sand.",
                exampleSentence = "We built a castle on the beach.",
                hints =
                    listOf(
                        Hint(1, "It is beside the sea."),
                        Hint(2, "You may find sand there."),
                        Hint(3, "The first letter is b."),
                    ),
            ),
            WordlyFixture(
                solution = "water",
                definition = "A clear liquid that people, animals and plants need.",
                exampleSentence = "The water in the glass was cool.",
                hints =
                    listOf(
                        Hint(1, "You drink it."),
                        Hint(2, "Plants need it to grow."),
                        Hint(3, "The first letter is w."),
                    ),
            ),
            WordlyFixture(
                solution = "story",
                definition = "A tale about people, places or events.",
                exampleSentence = "She read a story before bed.",
                morphologyNote = "The plural of story is stories.",
                hints =
                    listOf(
                        Hint(1, "It can be read or told."),
                        Hint(2, "A book may contain one."),
                        Hint(3, "The first letter is s."),
                    ),
            ),
        )

    private val wordlyGuessBank: List<String> =
        (
            wordlyFixtures.map { it.solution } +
                listOf(
                    "plane",
                    "cloud",
                    "bloom",
                    "flows",
                    "shape",
                    "shale",
                    "crane",
                    "trail",
                    "spark",
                    "chair",
                    "grape",
                    "stone",
                    "flame",
                    "globe",
                    "proud",
                    "sweet",
                    "learn",
                    "magic",
                    "field",
                    "crown",
                )
        ).distinct().sorted()

    private data class WordlyFixture(
        val solution: String,
        val definition: String,
        val exampleSentence: String,
        val hints: List<Hint>,
        val morphologyNote: String? = null,
        val difficulty: Difficulty = Difficulty.STARTER,
        val yearGroup: Int = 3,
    )
}
