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

@Suppress("LargeClass")
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
                    spellingPuzzle(dayIndex),
                    crosswordPuzzle(dayIndex),
                    sudokuPuzzle(dayIndex),
                    connectionsPuzzle(dayIndex),
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

    private fun spellingPuzzle(dayIndex: Int): SpellingBeePuzzle {
        val fixture = spellingFixtures[dayIndex % spellingFixtures.size]
        return SpellingBeePuzzle(
            id = "spelling-${dayId(dayIndex)}",
            difficulty = Difficulty.STARTER,
            curriculumTags = listOf("english:vocabulary", "english:spelling", "year:3"),
            hints = spellingHints(),
            review = humanReview(),
            letters = fixture.letters.toList(),
            centreLetter = fixture.centreLetter,
            targetWords = fixture.words.map { SpellingWord(it.word, it.definition) },
        )
    }

    private fun crosswordPuzzle(dayIndex: Int): CrosswordPuzzle {
        val fixture = crosswordFixtures[dayIndex % crosswordFixtures.size]
        return CrosswordPuzzle(
            id = "crossword-${dayId(dayIndex)}",
            difficulty = Difficulty.STARTER,
            curriculumTags = listOf("english:clues", "english:vocabulary", "year:3"),
            hints = crosswordHints(),
            review = humanReview(),
            width = CROSSWORD_SIZE,
            height = CROSSWORD_SIZE,
            entries = crosswordEntries(fixture),
        )
    }

    private fun sudokuPuzzle(dayIndex: Int): SudokuPuzzle {
        val solution = sudokuSolution(dayIndex)
        return SudokuPuzzle(
            id = "sudoku-${dayId(dayIndex)}",
            difficulty = Difficulty.STARTER,
            curriculumTags = listOf("maths:logic", "maths:number", "year:3"),
            hints = sudokuHints(),
            review = humanReview(),
            givens = sudokuGivens(solution, dayIndex),
            solution = solution,
        )
    }

    private fun connectionsPuzzle(dayIndex: Int): ConnectionsPuzzle {
        val fixture = connectionsFixtures[dayIndex % connectionsFixtures.size]
        return ConnectionsPuzzle(
            id = "connections-${dayId(dayIndex)}",
            difficulty = Difficulty.STARTER,
            curriculumTags = listOf("english:vocabulary", "reasoning:classification", "year:3"),
            hints = connectionsHints(),
            review = humanReview(),
            groups = fixture.groups,
        )
    }

    private fun humanReview(): ReviewMetadata =
        ReviewMetadata(
            automatedReviewPassed = true,
            humanReviewed = true,
            reviewer = "phase-6-fixture-review",
            notes = "Reviewed fixture for child-safe Wordly phase coverage.",
        )

    private const val SEASON_START_DATE = "2026-07-19"

    private const val WORDLY_FIXTURE_COUNT = 20

    private fun spellingHints(): List<Hint> =
        listOf(
            Hint(1, "Show how many answers start with each letter."),
            Hint(2, "Show the length of one undiscovered word."),
            Hint(3, "Show a definition for one undiscovered word."),
            Hint(4, "Show the first letter of one undiscovered word."),
        )

    private fun crosswordHints(): List<Hint> =
        listOf(
            Hint(1, "Show a simpler clue for the selected answer."),
            Hint(2, "Reveal one letter in the selected answer."),
            Hint(3, "Check the selected answer."),
            Hint(4, "Reveal the selected answer."),
        )

    private fun sudokuHints(): List<Hint> =
        listOf(
            Hint(1, "Highlight a useful row, column and box."),
            Hint(2, "Explain which numbers cannot fit there."),
            Hint(3, "Highlight a cell with only one choice."),
            Hint(4, "Place one helpful number."),
        )

    private fun connectionsHints(): List<Hint> =
        listOf(
            Hint(1, "Broadly describe one hidden group."),
            Hint(2, "Select two words from the same hidden group."),
            Hint(3, "Reveal one hidden group title."),
            Hint(4, "Reveal one full group after confirmation."),
        )

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
                    "known",
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

    private fun crosswordEntries(fixture: CrosswordFixture): List<CrosswordEntry> {
        val usedStarts = mutableMapOf<Char, Int>()
        val across =
            CrosswordEntry(
                answer = fixture.answer,
                clue = fixture.clue,
                row = CROSSWORD_MIDDLE,
                column = 0,
                direction = CrosswordDirection.ACROSS,
            )
        val down =
            fixture.answer.mapIndexed { column, letter ->
                val answer = nextCrosswordDownWord(letter, usedStarts)
                CrosswordEntry(
                    answer = answer,
                    clue = crosswordClueFor(answer),
                    row = CROSSWORD_MIDDLE,
                    column = column,
                    direction = CrosswordDirection.DOWN,
                )
            }
        return listOf(across) + down
    }

    private fun nextCrosswordDownWord(
        letter: Char,
        usedStarts: MutableMap<Char, Int>,
    ): String {
        val key = letter.lowercaseChar()
        val words = crosswordDownWords.getValue(key)
        val index = usedStarts[key] ?: 0
        usedStarts[key] = index + 1
        return words[index % words.size]
    }

    private fun crosswordClueFor(answer: String): String =
        crosswordClues[answer] ?: "A short answer beginning with ${answer.first().uppercaseChar()}."

    private const val CROSSWORD_SIZE = 7
    private const val CROSSWORD_MIDDLE = 3

    private val crosswordFixtures =
        listOf(
            CrosswordFixture("flowers", "Colourful parts of plants that may smell sweet."),
            CrosswordFixture("animals", "Living things such as cats, birds and fish."),
            CrosswordFixture("morning", "The early part of the day."),
            CrosswordFixture("picture", "An image made with a camera or pencil."),
            CrosswordFixture("friends", "People you like and trust."),
            CrosswordFixture("reading", "Understanding words on a page."),
            CrosswordFixture("drawing", "A picture made with lines."),
            CrosswordFixture("kitchen", "A room where meals are made."),
            CrosswordFixture("outside", "Not indoors."),
            CrosswordFixture("holiday", "A break from school or work."),
            CrosswordFixture("blanket", "A warm cover for a bed."),
            CrosswordFixture("monster", "A make-believe scary character."),
            CrosswordFixture("travels", "Goes from one place to another."),
            CrosswordFixture("teacher", "A person who helps a class learn."),
            CrosswordFixture("rainbow", "A colourful arch in the sky after rain."),
            CrosswordFixture("camping", "Sleeping outdoors in a tent."),
            CrosswordFixture("weather", "Sun, rain, wind or snow conditions."),
            CrosswordFixture("library", "A place to borrow books."),
            CrosswordFixture("playful", "Full of fun and games."),
            CrosswordFixture("gardens", "Places where plants grow."),
        )

    private val crosswordDownWords =
        mapOf(
            'a' to listOf("ant", "arm", "air", "able"),
            'b' to listOf("bag", "bat", "bee", "blue"),
            'c' to listOf("cat", "cap", "cow", "cake"),
            'd' to listOf("dog", "day", "den", "door"),
            'e' to listOf("ear", "eel", "egg", "east"),
            'f' to listOf("fan", "fun", "fox", "frog"),
            'g' to listOf("gap", "gem", "goat", "gold"),
            'h' to listOf("hat", "hen", "hop", "hand"),
            'i' to listOf("ice", "ink", "ivy", "into"),
            'k' to listOf("key", "kit", "kid", "kite"),
            'l' to listOf("log", "leg", "lap", "lion"),
            'm' to listOf("map", "man", "mat", "moon"),
            'n' to listOf("net", "nut", "nap", "nose"),
            'o' to listOf("owl", "oil", "one", "open"),
            'p' to listOf("pen", "pig", "pot", "pond"),
            'r' to listOf("run", "red", "rat", "rain"),
            's' to listOf("sun", "sit", "saw", "star"),
            't' to listOf("top", "tap", "ten", "tree"),
            'u' to listOf("use", "unit", "undo", "upon"),
            'v' to listOf("van", "vet", "vine", "very"),
            'w' to listOf("win", "web", "wet", "wind"),
            'y' to listOf("yes", "yak", "yarn", "yard"),
        )

    private val crosswordClues =
        mapOf(
            "ant" to "A tiny insect that can live in a colony.",
            "arm" to "A body part from shoulder to hand.",
            "air" to "The invisible gas around us.",
            "able" to "Having the skill or chance to do something.",
            "bag" to "A container with handles.",
            "bat" to "A stick used to hit a ball.",
            "bee" to "A buzzing insect that visits flowers.",
            "blue" to "The colour of a clear sky.",
            "cat" to "A small pet with whiskers.",
            "cap" to "A soft hat.",
            "cow" to "A farm animal that gives milk.",
            "cake" to "A sweet food for a celebration.",
            "dog" to "A friendly pet that may bark.",
            "day" to "The time between sunrise and night.",
            "den" to "A cosy room or animal home.",
            "door" to "You open this to enter a room.",
            "ear" to "A body part used to listen.",
            "eel" to "A long fish.",
            "egg" to "A shell-covered food from a hen.",
            "east" to "The direction where the sun rises.",
            "fan" to "Something that moves air.",
            "fun" to "Enjoyment or play.",
            "fox" to "A wild animal with a bushy tail.",
            "frog" to "A small jumping animal that may live near ponds.",
            "gap" to "An empty space between things.",
            "gem" to "A bright precious stone.",
            "goat" to "A farm animal that can climb well.",
            "gold" to "A shiny yellow metal.",
            "hat" to "Something worn on the head.",
            "hen" to "A female chicken.",
            "hop" to "A small jump.",
            "hand" to "A body part with fingers.",
            "ice" to "Frozen water.",
            "ink" to "Coloured liquid used for writing.",
            "ivy" to "A climbing green plant.",
            "into" to "Moving to the inside.",
            "key" to "A tool that opens a lock.",
            "kit" to "A set of things for a task.",
            "kid" to "A child.",
            "kite" to "A toy flown in the wind.",
            "log" to "A piece of cut tree trunk.",
            "leg" to "A body part used for standing.",
            "lap" to "The top of your legs when sitting.",
            "lion" to "A big wild cat with a mane.",
            "map" to "A drawing that shows places.",
            "man" to "A grown-up male person.",
            "mat" to "A small rug or pad.",
            "moon" to "The round object seen in the night sky.",
            "net" to "String tied in holes to catch or hold things.",
            "nut" to "A hard seed used as food.",
            "nap" to "A short sleep.",
            "nose" to "A body part for smelling.",
            "owl" to "A night bird with big eyes.",
            "oil" to "A slippery liquid.",
            "one" to "The number before two.",
            "open" to "Not closed.",
            "pen" to "A tool for writing.",
            "pig" to "A farm animal with a snout.",
            "pot" to "A container for cooking or plants.",
            "pond" to "A small area of water.",
            "run" to "To move quickly on foot.",
            "red" to "The colour of a strawberry.",
            "rat" to "A small animal with a long tail.",
            "rain" to "Water drops falling from clouds.",
            "sun" to "The star that gives Earth light.",
            "sit" to "To rest on a chair or the ground.",
            "saw" to "A tool for cutting wood.",
            "star" to "A bright object in the night sky.",
            "top" to "The highest part.",
            "tap" to "To touch lightly.",
            "ten" to "The number after nine.",
            "tree" to "A tall plant with a trunk.",
            "use" to "To do something with a tool or object.",
            "unit" to "One part of a set.",
            "undo" to "To reverse an action.",
            "upon" to "On or onto.",
            "van" to "A vehicle bigger than a car.",
            "vet" to "A doctor for animals.",
            "vine" to "A plant with a long climbing stem.",
            "very" to "A word that means a lot or extremely.",
            "win" to "To come first in a game.",
            "web" to "A spider can spin this.",
            "wet" to "Covered with water.",
            "wind" to "Moving air.",
            "yes" to "A word for agreeing.",
            "yak" to "A shaggy animal from cold mountains.",
            "yarn" to "Thread used for knitting.",
            "yard" to "An outdoor area near a building.",
        )

    private data class CrosswordFixture(
        val answer: String,
        val clue: String,
    )

    private fun sudokuSolution(dayIndex: Int): List<Int> {
        val rowOrder = sudokuRowOrder(dayIndex)
        val columnOrder = sudokuColumnOrder(dayIndex)
        val digitOffset = dayIndex % SUDOKU_SIZE
        return rowOrder.flatMap { row ->
            columnOrder.map { column ->
                val base = ((row % SUDOKU_REGION_ROWS) * SUDOKU_REGION_COLUMNS + row / SUDOKU_REGION_ROWS + column) % SUDOKU_SIZE + 1
                (base + digitOffset - 1) % SUDOKU_SIZE + 1
            }
        }
    }

    private fun sudokuGivens(
        solution: List<Int>,
        dayIndex: Int,
    ): List<Int> =
        solution.mapIndexed { index, value ->
            val row = index / SUDOKU_SIZE
            val column = index % SUDOKU_SIZE
            if (column == (row + dayIndex) % SUDOKU_SIZE) 0 else value
        }

    private fun sudokuRowOrder(dayIndex: Int): List<Int> {
        val bandOrders =
            listOf(
                listOf(0, 1, 2),
                listOf(1, 2, 0),
                listOf(2, 0, 1),
                listOf(0, 2, 1),
                listOf(2, 1, 0),
            )
        return bandOrders[dayIndex % bandOrders.size].flatMap { band ->
            val rows = listOf(band * SUDOKU_REGION_ROWS, band * SUDOKU_REGION_ROWS + 1)
            if ((dayIndex + band) % 2 == 0) rows else rows.reversed()
        }
    }

    private fun sudokuColumnOrder(dayIndex: Int): List<Int> {
        val stackOrder = if ((dayIndex / 5) % 2 == 0) listOf(0, 1) else listOf(1, 0)
        return stackOrder.flatMap { stack ->
            val columns = (stack * SUDOKU_REGION_COLUMNS until (stack + 1) * SUDOKU_REGION_COLUMNS).toList()
            val shift = (dayIndex + stack) % SUDOKU_REGION_COLUMNS
            columns.drop(shift) + columns.take(shift)
        }
    }

    private const val SUDOKU_SIZE = 6
    private const val SUDOKU_REGION_ROWS = 2
    private const val SUDOKU_REGION_COLUMNS = 3

    private val connectionsFixtures =
        listOf(
            connectionsFixture(
                cg("Sky things", "Things you can see above you.", "moon", "star", "cloud", "sun"),
                cg("School tools", "Useful things in a classroom.", "desk", "book", "pencil", "ruler"),
                cg("Garden life", "Things that grow outside.", "leaf", "seed", "flower", "grass"),
                cg("Ways to move", "Actions people can use to travel.", "jump", "skip", "walk", "climb"),
            ),
            connectionsFixture(
                cg("Fruit", "Sweet foods that grow on plants.", "apple", "pear", "plum", "grape"),
                cg("Weather", "Things you may notice outside.", "rain", "wind", "snow", "storm"),
                cg("Art supplies", "Tools for making pictures.", "paint", "brush", "paper", "crayon"),
                cg("Animal homes", "Places where animals may live.", "nest", "den", "hive", "burrow"),
            ),
            connectionsFixture(
                cg("Ocean animals", "Animals that live in the sea.", "whale", "shark", "crab", "seal"),
                cg("Music words", "Things used to make or read music.", "song", "drum", "flute", "note"),
                cg("Kitchen tools", "Items used when preparing food.", "spoon", "plate", "bowl", "fork"),
                cg("Fast things", "Things known for moving quickly.", "rocket", "train", "cheetah", "jet"),
            ),
            connectionsFixture(
                cg("Camping kit", "Things used on a camping trip.", "tent", "torch", "map", "rope"),
                cg("Feelings", "Words for how someone may feel.", "happy", "calm", "proud", "brave"),
                cg("Shapes", "Names of common shapes.", "circle", "square", "oval", "cube"),
                cg("Birds", "Animals with feathers.", "swan", "robin", "eagle", "owl"),
            ),
            connectionsFixture(
                cg("Transport", "Ways people can travel.", "bus", "bike", "ship", "taxi"),
                cg("Book pages", "Pieces of a book.", "cover", "page", "chapter", "title"),
                cg("Tree features", "Pieces of a tree or plant.", "root", "trunk", "branch", "bark"),
                cg("Sports gear", "Things used in games and sport.", "ball", "bat", "goal", "net"),
            ),
            connectionsFixture(
                cg("Breakfast foods", "Foods people may eat in the morning.", "toast", "cereal", "egg", "porridge"),
                cg("Light sources", "Things that can shine.", "lamp", "candle", "torch", "lantern"),
                cg("Clothing", "Things people wear.", "coat", "scarf", "sock", "shirt"),
                cg("Maths words", "Words used when working with numbers.", "plus", "minus", "equal", "sum"),
            ),
            connectionsFixture(
                cg("Baby animals", "Names for young animals.", "cub", "calf", "chick", "foal"),
                cg("Building parts", "Parts of a building.", "wall", "roof", "door", "window"),
                cg("Water places", "Places where water can be found.", "river", "lake", "pond", "stream"),
                cg("Quiet actions", "Actions that can be done softly.", "whisper", "tiptoe", "listen", "read"),
            ),
            connectionsFixture(
                cg("Time words", "Words that describe when things happen.", "today", "later", "soon", "before"),
                cg("Board games", "Games played on a table or board.", "chess", "draughts", "ludo", "scrabble"),
                cg("Green things", "Things often coloured green.", "frog", "lime", "pea", "moss"),
                cg("Jobs", "People who do different kinds of work.", "doctor", "teacher", "farmer", "baker"),
            ),
            connectionsFixture(
                cg("Park items", "Things you may find at a park.", "slide", "swing", "bench", "path"),
                cg("Measurement", "Tools or words for measuring.", "metre", "scale", "timer", "ruler"),
                cg("Pets", "Animals people may care for at home.", "cat", "dog", "rabbit", "hamster"),
                cg("Cooking actions", "Actions used when making food.", "mix", "stir", "bake", "chop"),
            ),
            connectionsFixture(
                cg("Space words", "Things linked with outer space.", "planet", "comet", "orbit", "galaxy"),
                cg("Beach things", "Things you may see at the seaside.", "sand", "shell", "wave", "bucket"),
                cg("Opposites", "Words with contrasting meanings.", "hot", "cold", "big", "small"),
                cg("Insects", "Small animals with six legs.", "ant", "bee", "beetle", "moth"),
            ),
            connectionsFixture(
                cg("Library words", "Things linked with reading books.", "shelf", "story", "author", "library"),
                cg("Rainforest animals", "Animals from warm forests.", "monkey", "parrot", "jaguar", "toucan"),
                cg("Materials", "Things objects can be made from.", "wood", "metal", "glass", "cloth"),
                cg("Polite words", "Kind words used with other people.", "please", "thanks", "sorry", "hello"),
            ),
            connectionsFixture(
                cg("Farm animals", "Animals often found on farms.", "cow", "pig", "sheep", "goat"),
                cg("Tools", "Items used to build or fix things.", "hammer", "saw", "spade", "wrench"),
                cg("Colours", "Colour names.", "red", "blue", "yellow", "purple"),
                cg("Story settings", "Places where a story may happen.", "castle", "forest", "island", "village"),
            ),
            connectionsFixture(
                cg("Body parts", "Parts of the human body.", "hand", "foot", "knee", "elbow"),
                cg("Computer words", "Things used with digital devices.", "mouse", "screen", "keyboard", "tablet"),
                cg("Drinks", "Things people can drink.", "water", "juice", "milk", "tea"),
                cg("Sound words", "Words for noises.", "bang", "buzz", "hum", "ring"),
            ),
            connectionsFixture(
                cg("Winter things", "Things linked with cold weather.", "ice", "frost", "sledge", "glove"),
                cg("Garden tools", "Tools used outside in a garden.", "rake", "hoe", "trowel", "hose"),
                cg("Exercise", "Activities that make bodies move.", "run", "swim", "dance", "stretch"),
                cg("Containers", "Things that hold other things.", "box", "jar", "bag", "basket"),
            ),
            connectionsFixture(
                cg("Celebrations", "Things linked with parties.", "cake", "card", "music", "balloon"),
                cg("Map words", "Words used when reading maps.", "north", "south", "east", "west"),
                cg("Desert things", "Things linked with dry sandy places.", "sand", "cactus", "camel", "dune"),
                cg("Verbs ending in ing", "Action words in the -ing form.", "reading", "singing", "jumping", "cooking"),
            ),
            connectionsFixture(
                cg("Tiny things", "Small things you can hold or see.", "pin", "seed", "bead", "coin"),
                cg("Royal words", "Words linked with kings and queens.", "crown", "queen", "king", "palace"),
                cg("Road words", "Things found on or near roads.", "lane", "traffic", "bridge", "sign"),
                cg("Sweet treats", "Sweet foods eaten as a treat.", "honey", "jam", "fudge", "biscuit"),
            ),
            connectionsFixture(
                cg("Science tools", "Tools used for investigating.", "magnet", "lens", "beaker", "filter"),
                cg("Reptiles", "Scaly animals.", "snake", "lizard", "turtle", "gecko"),
                cg("House rooms", "Rooms inside a home.", "kitchen", "bedroom", "bathroom", "hall"),
                cg("Writing words", "Things linked with writing.", "letter", "sentence", "comma", "pencil"),
            ),
            connectionsFixture(
                cg("Mountain words", "Things linked with mountains.", "peak", "cliff", "valley", "summit"),
                cg("Team roles", "People in a team or activity.", "captain", "coach", "player", "helper"),
                cg("Round things", "Objects that are round or circular.", "wheel", "coin", "plate", "button"),
                cg("Cleaning items", "Things used to clean.", "soap", "brush", "mop", "sponge"),
            ),
            connectionsFixture(
                cg("Nocturnal animals", "Animals often active at night.", "bat", "owl", "fox", "moth"),
                cg("Classroom actions", "Things pupils do in class.", "learn", "write", "count", "share"),
                cg("Hot things", "Things that can feel hot.", "fire", "sun", "oven", "kettle"),
                cg("Pairs", "Things that usually come in pairs.", "shoes", "socks", "gloves", "earrings"),
            ),
            connectionsFixture(
                cg("Adventure words", "Words linked with exploring.", "route", "map", "trail", "camp"),
                cg("Bakery foods", "Foods made in a bakery.", "bread", "bun", "muffin", "pastry"),
                cg("Things with keys", "Things that use keys.", "piano", "lock", "keyboard", "car"),
                cg("Helpful traits", "Good qualities people can show.", "kind", "fair", "patient", "honest"),
            ),
        )

    private fun connectionsFixture(vararg groups: ConnectionGroup): ConnectionsFixture = ConnectionsFixture(groups.toList())

    private fun cg(
        title: String,
        explanation: String,
        vararg words: String,
    ): ConnectionGroup = ConnectionGroup(title, words.toList(), explanation)

    private data class ConnectionsFixture(
        val groups: List<ConnectionGroup>,
    )

    private val spellingFixtures =
        listOf(
            SpellingFixture(
                letters = "planter",
                centreLetter = 'a',
                words =
                    listOf(
                        word("ant", "A small insect."),
                        word("plan", "An idea for what to do next."),
                        word("plant", "A living thing that grows in soil."),
                        word("plane", "A flying vehicle with wings."),
                        word("planet", "A world that moves around a star."),
                        word("panel", "A flat part of a door or wall."),
                        word("part", "One piece of a whole."),
                        word("later", "At a time after now."),
                        word("learn", "To find out or practise something new."),
                        word("alert", "Ready to notice what is happening."),
                        word("alter", "To change something."),
                        word("rental", "Something borrowed for a time."),
                        word("planter", "A pot or box for growing plants."),
                    ),
            ),
            SpellingFixture(
                letters = "painter",
                centreLetter = 'i',
                words =
                    listOf(
                        word("pin", "A small sharp fastener."),
                        word("pit", "A deep hole."),
                        word("tin", "A light metal container."),
                        word("tip", "The pointed end of something."),
                        word("pair", "Two things that go together."),
                        word("pain", "A hurt feeling in the body."),
                        word("paint", "Coloured liquid used on paper or walls."),
                        word("rain", "Water drops falling from clouds."),
                        word("train", "A vehicle that travels on tracks."),
                        word("print", "To make words or pictures on paper."),
                        word("ripe", "Ready to eat."),
                        word("tire", "To become tired."),
                        word("painter", "A person who paints."),
                    ),
            ),
            SpellingFixture(
                letters = "cabinet",
                centreLetter = 'a',
                words =
                    listOf(
                        word("cab", "A car that carries paying passengers."),
                        word("can", "To be able to do something."),
                        word("cat", "A small pet with whiskers."),
                        word("bat", "A stick used to hit a ball."),
                        word("ban", "To say something is not allowed."),
                        word("ate", "Had food."),
                        word("bean", "A seed used as food."),
                        word("cane", "A long strong plant stem."),
                        word("neat", "Tidy and in order."),
                        word("cabin", "A small simple house."),
                        word("enact", "To make a rule official."),
                        word("antic", "A playful trick or action."),
                        word("cabinet", "A cupboard with shelves or drawers."),
                    ),
            ),
            SpellingFixture(
                letters = "dragnet",
                centreLetter = 'a',
                words =
                    listOf(
                        word("age", "How long someone or something has lived."),
                        word("ant", "A small insect."),
                        word("art", "Pictures, music or other creative work."),
                        word("ate", "Had food."),
                        word("dare", "To be brave enough to try."),
                        word("date", "A day on a calendar."),
                        word("dear", "Loved or special."),
                        word("gate", "A movable opening in a fence."),
                        word("gear", "Equipment for an activity."),
                        word("grade", "A mark or level."),
                        word("grant", "To give or allow."),
                        word("garden", "A place where plants grow."),
                        word("danger", "Something that could cause harm."),
                        word("dragnet", "A wide search for something."),
                    ),
            ),
            SpellingFixture(
                letters = "picture",
                centreLetter = 'i',
                words =
                    listOf(
                        word("ice", "Frozen water."),
                        word("tie", "To fasten with a knot."),
                        word("tip", "The pointed end of something."),
                        word("pit", "A deep hole."),
                        word("ripe", "Ready to eat."),
                        word("pier", "A platform built out over water."),
                        word("tire", "To become tired."),
                        word("trip", "A journey to another place."),
                        word("epic", "Very big or impressive."),
                        word("cite", "To mention as evidence."),
                        word("price", "How much something costs."),
                        word("cutie", "A friendly name for someone sweet."),
                        word("recite", "To say something from memory."),
                        word("picture", "An image or drawing."),
                    ),
            ),
            SpellingFixture(
                letters = "helping",
                centreLetter = 'e',
                words =
                    listOf(
                        word("hen", "A female chicken."),
                        word("pen", "A tool for writing."),
                        word("peg", "A small pin or holder."),
                        word("leg", "A body part used for standing."),
                        word("eel", "A long fish."),
                        word("heel", "The back part of a foot."),
                        word("peel", "To take the skin off fruit."),
                        word("help", "To make something easier for someone."),
                        word("pile", "A stack of things."),
                        word("pine", "A tree with needles."),
                        word("line", "A long thin mark."),
                        word("hinge", "The part that lets a door swing."),
                        word("helping", "Giving support to someone."),
                    ),
            ),
            SpellingFixture(
                letters = "reading",
                centreLetter = 'e',
                words =
                    listOf(
                        word("red", "The colour of a strawberry."),
                        word("den", "A cosy room or animal home."),
                        word("end", "The last part."),
                        word("age", "How long someone or something has lived."),
                        word("ear", "A body part for hearing."),
                        word("read", "To look at words and understand them."),
                        word("dear", "Loved or special."),
                        word("deer", "A gentle wild animal."),
                        word("reed", "A tall plant that grows near water."),
                        word("range", "A set of different amounts or types."),
                        word("eager", "Very ready and excited."),
                        word("garden", "A place where plants grow."),
                        word("danger", "Something that could cause harm."),
                        word("gained", "Got or earned something."),
                        word("reading", "Looking at words and understanding them."),
                    ),
            ),
            SpellingFixture(
                letters = "drawing",
                centreLetter = 'a',
                words =
                    listOf(
                        word("and", "A word that joins ideas."),
                        word("raw", "Not cooked."),
                        word("rag", "A small piece of old cloth."),
                        word("wag", "To move from side to side."),
                        word("wand", "A thin stick in stories or games."),
                        word("rang", "Made a bell sound."),
                        word("drain", "A pipe or hole where water leaves."),
                        word("grain", "A seed used for food."),
                        word("grand", "Large or impressive."),
                        word("daring", "Brave and ready to try."),
                        word("inward", "Moving toward the inside."),
                        word("award", "A prize for good work."),
                        word("drawing", "A picture made with lines."),
                    ),
            ),
            SpellingFixture(
                letters = "kitchen",
                centreLetter = 'i',
                words =
                    listOf(
                        word("kit", "A set of things for a task."),
                        word("tin", "A light metal container."),
                        word("hit", "To strike something."),
                        word("itch", "A tickly feeling on the skin."),
                        word("thin", "Not thick."),
                        word("thick", "Wide from one side to the other."),
                        word("nice", "Kind or pleasant."),
                        word("tick", "A small mark or clicking sound."),
                        word("kite", "A toy flown in the wind."),
                        word("nick", "A small cut."),
                        word("hint", "A small clue."),
                        word("kitten", "A young cat."),
                        word("ethic", "An idea about doing what is right."),
                        word("kitchen", "A room where food is prepared."),
                    ),
            ),
            SpellingFixture(
                letters = "outside",
                centreLetter = 'i',
                words =
                    listOf(
                        word("sit", "To rest on a chair or the ground."),
                        word("tie", "To fasten with a knot."),
                        word("die", "A cube used in games."),
                        word("site", "A place where something is found."),
                        word("side", "One edge or part of something."),
                        word("tide", "The sea moving higher or lower."),
                        word("diet", "The food someone usually eats."),
                        word("edit", "To fix or improve writing."),
                        word("suit", "Clothes that match, or to be right for."),
                        word("suite", "A set of rooms."),
                        word("suited", "Right or fitted for something."),
                        word("duties", "Jobs that need to be done."),
                        word("outside", "Not inside a place."),
                    ),
            ),
            SpellingFixture(
                letters = "flowers",
                centreLetter = 'o',
                words =
                    listOf(
                        word("owe", "To need to give something back."),
                        word("low", "Not high."),
                        word("row", "A line of things."),
                        word("flow", "To move smoothly like water."),
                        word("floor", "The surface you walk on inside."),
                        word("flower", "The colourful part of many plants."),
                        word("flowers", "More than one flower."),
                        word("loose", "Not tight."),
                        word("slower", "Moving with less speed."),
                        word("lower", "Closer to the ground."),
                        word("fowl", "A bird kept for food or eggs."),
                        word("wolf", "A wild animal like a large dog."),
                        word("sore", "Hurting a little."),
                        word("lore", "Stories and knowledge passed along."),
                    ),
            ),
            SpellingFixture(
                letters = "machine",
                centreLetter = 'a',
                words =
                    listOf(
                        word("ace", "A playing card with one symbol."),
                        word("ache", "A steady hurt."),
                        word("each", "Every one by itself."),
                        word("man", "A grown-up male person."),
                        word("can", "To be able to do something."),
                        word("came", "Arrived or moved closer."),
                        word("main", "Most important."),
                        word("mean", "To have a meaning."),
                        word("mane", "Long hair on an animal's neck."),
                        word("name", "A word for a person or thing."),
                        word("chain", "Linked loops of metal or plastic."),
                        word("cinema", "A place where films are shown."),
                        word("machine", "A tool with moving parts."),
                    ),
            ),
            SpellingFixture(
                letters = "holiday",
                centreLetter = 'o',
                words =
                    listOf(
                        word("old", "Not new or young."),
                        word("oil", "A slippery liquid."),
                        word("idol", "A person someone admires a lot."),
                        word("hold", "To keep in your hand or arms."),
                        word("holy", "Special in a religious way."),
                        word("load", "Something carried."),
                        word("loyal", "Faithful and true."),
                        word("alloy", "A mix of metals."),
                        word("yoyo", "A toy that rolls up and down a string."),
                        word("hoody", "A top with a hood."),
                        word("holiday", "A break from school or work."),
                    ),
            ),
            SpellingFixture(
                letters = "climber",
                centreLetter = 'i',
                words =
                    listOf(
                        word("ice", "Frozen water."),
                        word("rib", "A curved bone in the chest."),
                        word("rim", "The edge around something round."),
                        word("mile", "A long distance."),
                        word("lime", "A small green fruit."),
                        word("climb", "To go upward using hands or feet."),
                        word("climber", "A person who climbs."),
                        word("crime", "An action against the law."),
                        word("relic", "An old object kept from the past."),
                        word("crib", "A small bed for a baby."),
                        word("rice", "Small white grains eaten as food."),
                        word("mice", "More than one mouse."),
                    ),
            ),
            SpellingFixture(
                letters = "friends",
                centreLetter = 'i',
                words =
                    listOf(
                        word("fin", "A thin body part that helps fish swim."),
                        word("din", "A loud confused noise."),
                        word("rid", "To clear something away."),
                        word("sir", "A polite word for a man."),
                        word("sin", "A wrong action in some beliefs."),
                        word("fir", "An evergreen tree."),
                        word("find", "To discover something."),
                        word("fine", "Good or well."),
                        word("fire", "Heat and flames."),
                        word("fried", "Cooked in hot oil."),
                        word("fries", "Thin pieces of fried potato."),
                        word("friend", "Someone you like and trust."),
                        word("friends", "More than one friend."),
                    ),
            ),
            SpellingFixture(
                letters = "learnig",
                centreLetter = 'e',
                words =
                    listOf(
                        word("leg", "A body part used for standing."),
                        word("gel", "A soft jelly-like substance."),
                        word("age", "How long someone or something has lived."),
                        word("ear", "A body part for hearing."),
                        word("era", "A period of time."),
                        word("lean", "To rest at an angle."),
                        word("learn", "To find out or practise something new."),
                        word("real", "True or actually existing."),
                        word("near", "Close by."),
                        word("line", "A long thin mark."),
                        word("reign", "The time a king or queen rules."),
                        word("green", "The colour of grass."),
                        word("eager", "Very ready and excited."),
                        word("anger", "A strong upset feeling."),
                        word("learning", "Finding out and practising new things."),
                    ),
            ),
            SpellingFixture(
                letters = "campers",
                centreLetter = 'a',
                words =
                    listOf(
                        word("arm", "A body part from shoulder to hand."),
                        word("are", "A form of the word be."),
                        word("cap", "A soft hat."),
                        word("car", "A road vehicle."),
                        word("map", "A drawing that shows places."),
                        word("mare", "A female horse."),
                        word("care", "Kind attention or help."),
                        word("pace", "Speed of movement."),
                        word("camp", "A place to sleep outdoors."),
                        word("camps", "More than one camp."),
                        word("spare", "Extra or not being used."),
                        word("space", "An empty area or the sky beyond Earth."),
                        word("camper", "A person who camps."),
                        word("campers", "People who camp."),
                    ),
            ),
            SpellingFixture(
                letters = "blanket",
                centreLetter = 'a',
                words =
                    listOf(
                        word("ant", "A small insect."),
                        word("ate", "Had food."),
                        word("bat", "A stick used to hit a ball."),
                        word("ban", "To say something is not allowed."),
                        word("bean", "A seed used as food."),
                        word("bake", "To cook in an oven."),
                        word("lake", "A large area of water."),
                        word("lane", "A narrow road or path."),
                        word("lean", "To rest at an angle."),
                        word("ankle", "The joint above the foot."),
                        word("tank", "A large container for liquid."),
                        word("taken", "Carried or moved away."),
                        word("bleak", "Cold, bare or gloomy."),
                        word("blanket", "A warm cover used on a bed."),
                    ),
            ),
            SpellingFixture(
                letters = "monster",
                centreLetter = 'o',
                words =
                    listOf(
                        word("one", "The number 1."),
                        word("ton", "A very heavy amount."),
                        word("not", "A word that makes a statement negative."),
                        word("toe", "A small part at the end of a foot."),
                        word("too", "Also, or more than needed."),
                        word("soon", "After a short time."),
                        word("moon", "The round object seen in the night sky."),
                        word("more", "A larger amount."),
                        word("some", "An amount that is not exact."),
                        word("sort", "A type or kind."),
                        word("rose", "A flower with a sweet smell."),
                        word("stone", "A small piece of rock."),
                        word("store", "A place where things are kept or sold."),
                        word("storm", "Bad weather with strong wind or rain."),
                        word("monster", "A made-up scary creature from stories."),
                    ),
            ),
            SpellingFixture(
                letters = "travels",
                centreLetter = 'a',
                words =
                    listOf(
                        word("ate", "Had food."),
                        word("are", "A form of the word be."),
                        word("art", "Pictures, music or other creative work."),
                        word("rat", "A small animal with a long tail."),
                        word("star", "A bright object in the night sky."),
                        word("start", "To begin."),
                        word("stale", "No longer fresh."),
                        word("steal", "To take something that is not yours."),
                        word("later", "At a time after now."),
                        word("least", "The smallest amount."),
                        word("alert", "Ready to notice what is happening."),
                        word("alter", "To change something."),
                        word("travel", "To go from one place to another."),
                        word("travels", "Goes from one place to another."),
                    ),
            ),
        )

    private fun word(
        text: String,
        definition: String,
    ): SpellingWordFixture = SpellingWordFixture(text, definition)

    private data class SpellingFixture(
        val letters: String,
        val centreLetter: Char,
        val words: List<SpellingWordFixture>,
    )

    private data class SpellingWordFixture(
        val word: String,
        val definition: String,
    )
}
