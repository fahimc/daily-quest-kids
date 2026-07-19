package com.dailyquestkids.core.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SerializationTest {
    private val json =
        Json {
            classDiscriminator = "type"
            encodeDefaults = true
        }

    @Test
    fun puzzlePackRoundTripPreservesSealedPuzzleTypes() {
        val pack =
            PuzzlePack(
                schemaVersion = 1,
                seasonVersion = "sample",
                seasonStartDateUtc = "2026-09-01",
                checksum = "sample-checksum",
                days = listOf(SampleModels.dayOne()),
            )

        val encoded = json.encodeToString(pack)
        val decoded = json.decodeFromString<PuzzlePack>(encoded)

        assertEquals(pack, decoded)
    }
}

private object SampleModels {
    fun dayOne(): DailyPuzzleSet =
        DailyPuzzleSet(
            dayIndex = 0,
            globalDayNumber = 1,
            puzzles =
                listOf(
                    WordlyPuzzle(
                        id = "wordly-001",
                        difficulty = Difficulty.STARTER,
                        curriculumTags = listOf("english:spelling"),
                        hints = listOf(Hint(1, "A kind action")),
                        review = ReviewMetadata(automatedReviewPassed = true, humanReviewed = false),
                        solution = "share",
                        validGuesses = listOf("share"),
                        definition = "To let someone else use or enjoy something.",
                        exampleSentence = "We can share the crayons.",
                    ),
                ),
        )
}
