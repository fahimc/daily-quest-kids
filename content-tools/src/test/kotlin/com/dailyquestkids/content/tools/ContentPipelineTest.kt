package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.exists
import kotlin.io.path.readText

class ContentPipelineTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun pipelineProducesDeterministicPackAndReports() {
        val firstRoot = temporaryFolder.newFolder("first").toPath()
        val secondRoot = temporaryFolder.newFolder("second").toPath()

        val first = ContentPipeline.run(outputRoot = firstRoot)
        val second = ContentPipeline.run(outputRoot = secondRoot)

        assertTrue(first.summary.passed)
        assertEquals(first.summary.checksum, second.summary.checksum)
        assertEquals(first.directories.packJson.readText(), second.directories.packJson.readText())
        assertTrue(first.directories.machineReport.exists())
        assertTrue(
            first.directories.csvReport
                .readText()
                .contains("day_index,global_day,puzzle_id"),
        )
        assertTrue(
            first.directories.htmlReport
                .readText()
                .contains("Content Report"),
        )
    }

    @Test
    fun invalidContentFailsReport() {
        val pack = FixturePackFactory.oneDayPack()
        val day = pack.days.single()
        val duplicateIdPuzzle = day.puzzles[1].copyWithId(day.puzzles.first().id)
        val invalidPack =
            pack.copy(
                days =
                    listOf(
                        day.copy(
                            puzzles = listOf(day.puzzles.first(), duplicateIdPuzzle) + day.puzzles.drop(2),
                        ),
                    ),
            )

        val result = ContentPipeline.run(pack = invalidPack, outputRoot = temporaryFolder.newFolder("invalid").toPath())

        assertFalse(result.summary.passed)
        assertTrue(result.summary.errors.any { it.contains("duplicate puzzle id") })
        assertTrue(
            result.directories.machineReport
                .readText()
                .contains("\"passed\": false"),
        )
    }
}

private fun Puzzle.copyWithId(id: String): Puzzle =
    when (this) {
        is WordlyPuzzle -> copy(id = id)
        is SpellingBeePuzzle -> copy(id = id)
        is CrosswordPuzzle -> copy(id = id)
        is SudokuPuzzle -> copy(id = id)
        is ConnectionsPuzzle -> copy(id = id)
    }
