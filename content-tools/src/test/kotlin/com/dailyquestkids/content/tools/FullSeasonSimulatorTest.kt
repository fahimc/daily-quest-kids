package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.PuzzleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.path.exists
import kotlin.io.path.readText

class FullSeasonSimulatorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun simulatorCompletesEveryCandidatePuzzleThroughEngines() {
        val report = FullSeasonSimulator.simulate(SeasonOneCandidateFactory.candidatePack())

        assertTrue(report.passed)
        assertEquals(365, report.dayCount)
        assertEquals(1_825, report.puzzleCount)
        assertEquals(1_825, report.completedPuzzles)
        assertEquals(1_825, report.shareSafePuzzles)
        assertEquals(1_825, report.saveRestoredPuzzles)
        assertEquals(365, report.dailyFivePerfectDays)
        assertEquals(365, report.bestDailyFiveStreak)
        PuzzleCategory.entries.forEach { category ->
            assertEquals(365, report.categorySolvedCounts[category.name])
            assertEquals(365, report.categoryBestStreaks[category.name])
        }
        assertTrue(report.scenarioResults.values.all { it })
    }

    @Test
    fun simulatorWritesMachineCsvAndHtmlReports() {
        val root = temporaryFolder.newFolder("simulation").toPath()
        val report = FullSeasonSimulator.simulate(SeasonOneCandidateFactory.candidatePack())

        FullSeasonSimulator.writeReports(report, root)

        assertTrue(root.resolve("reports/full-season-simulation.json").exists())
        assertTrue(root.resolve("reports/full-season-simulation.csv").readText().contains("completion_recorded"))
        assertTrue(root.resolve("reports/full-season-simulation.html").readText().contains("Status: passed"))
    }
}
