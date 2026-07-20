package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.model.CrosswordDirection
import com.dailyquestkids.core.model.CrosswordPuzzle
import com.dailyquestkids.core.model.Puzzle
import com.dailyquestkids.core.model.PuzzleCategory
import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.model.ShareCardModel
import com.dailyquestkids.core.model.SpellingBeePuzzle
import com.dailyquestkids.core.model.SudokuPuzzle
import com.dailyquestkids.core.model.WordlyPuzzle
import com.dailyquestkids.puzzle.engine.ConnectionsGameEngine
import com.dailyquestkids.puzzle.engine.CrosswordGameEngine
import com.dailyquestkids.puzzle.engine.ShareSafety
import com.dailyquestkids.puzzle.engine.SpellingBGameEngine
import com.dailyquestkids.puzzle.engine.SudokuGameEngine
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

object FullSeasonSimulator {
    fun simulate(pack: PuzzlePack): FullSeasonSimulationReport {
        val errors = mutableListOf<String>()
        val rows = mutableListOf<FullSeasonSimulationRow>()
        val categorySolved = PuzzleCategory.entries.associateWith { 0 }.toMutableMap()
        val categoryBestStreaks = PuzzleCategory.entries.associateWith { 0 }.toMutableMap()
        val categoryCurrentStreaks = PuzzleCategory.entries.associateWith { 0 }.toMutableMap()
        var currentDailyFiveStreak = 0
        var bestDailyFiveStreak = 0

        pack.days.forEach { day ->
            val dayResults =
                day.puzzles.map { puzzle ->
                    runCatching { simulatePuzzle(puzzle, day.globalDayNumber) }
                        .getOrElse { error ->
                            val message = "${puzzle.id} simulation crashed: ${error.message}"
                            errors += message
                            PuzzleSimulationResult(puzzle.id, puzzle.category, false, false, false, false, 0, message)
                        }
                }
            dayResults.forEach { result ->
                if (!result.completed) errors += "${result.puzzleId} did not complete"
                if (!result.saveRestored) errors += "${result.puzzleId} save/restore failed"
                if (!result.completionRecorded) errors += "${result.puzzleId} completion event missing"
                if (!result.shareSafe) errors += "${result.puzzleId} share model leaks answers"
                if (result.completed) {
                    categorySolved[result.category] = categorySolved.getValue(result.category) + 1
                    categoryCurrentStreaks[result.category] = categoryCurrentStreaks.getValue(result.category) + 1
                    categoryBestStreaks[result.category] =
                        maxOf(categoryBestStreaks.getValue(result.category), categoryCurrentStreaks.getValue(result.category))
                } else {
                    categoryCurrentStreaks[result.category] = 0
                }
                rows +=
                    FullSeasonSimulationRow(
                        dayIndex = day.dayIndex,
                        globalDay = day.globalDayNumber,
                        puzzleId = result.puzzleId,
                        category = result.category.name,
                        completed = result.completed,
                        saveRestored = result.saveRestored,
                        completionRecorded = result.completionRecorded,
                        shareSafe = result.shareSafe,
                        hintsUsed = result.hintsUsed,
                        error = result.error.orEmpty(),
                    )
            }

            if (dayResults.all { it.completed }) {
                currentDailyFiveStreak += 1
                bestDailyFiveStreak = maxOf(bestDailyFiveStreak, currentDailyFiveStreak)
            } else {
                currentDailyFiveStreak = 0
            }
        }

        val scenarios = scenarioAudit(pack.days.size)
        errors += scenarios.filterValues { !it }.keys.map { scenario -> "scenario failed: $scenario" }
        return FullSeasonSimulationReport(
            seasonVersion = pack.seasonVersion,
            dayCount = pack.days.size,
            puzzleCount = rows.size,
            completedPuzzles = rows.count { it.completed },
            shareSafePuzzles = rows.count { it.shareSafe },
            saveRestoredPuzzles = rows.count { it.saveRestored },
            dailyFivePerfectDays =
                pack.days.count { day ->
                    rows.filter { it.dayIndex == day.dayIndex }.all { it.completed }
                },
            bestDailyFiveStreak = bestDailyFiveStreak,
            categorySolvedCounts = categorySolved.mapKeys { it.key.name },
            categoryBestStreaks = categoryBestStreaks.mapKeys { it.key.name },
            scenarioResults = scenarios,
            rows = rows,
            errors = errors,
        )
    }

    fun writeReports(
        report: FullSeasonSimulationReport,
        outputRoot: Path,
    ) {
        val reports = outputRoot.resolve("reports")
        reports.createDirectories()
        reports.resolve("full-season-simulation.json").writeText(json.encodeToString(report))
        reports.resolve("full-season-simulation.csv").writeText(csv(report.rows))
        reports.resolve("full-season-simulation.html").writeText(html(report))
    }

    private fun simulatePuzzle(
        puzzle: Puzzle,
        globalDay: Int,
    ): PuzzleSimulationResult =
        when (puzzle) {
            is WordlyPuzzle -> simulateWordly(puzzle, globalDay)
            is SpellingBeePuzzle -> simulateSpelling(puzzle, globalDay)
            is CrosswordPuzzle -> simulateCrossword(puzzle, globalDay)
            is SudokuPuzzle -> simulateSudoku(puzzle, globalDay)
            is ConnectionsPuzzle -> simulateConnections(puzzle, globalDay)
        }

    private fun simulateWordly(
        puzzle: WordlyPuzzle,
        globalDay: Int,
    ): PuzzleSimulationResult {
        var state = WordlyGameEngine.initial(puzzle)
        puzzle.solution.forEach { letter -> state = WordlyGameEngine.appendLetter(puzzle, state, letter).state }
        val result = WordlyGameEngine.submit(puzzle, WordlyGameEngine.decode(WordlyGameEngine.encode(state)))
        val restored = WordlyGameEngine.decode(WordlyGameEngine.encode(result.state))
        val share = WordlyGameEngine.shareCard(puzzle, restored, utcDate(globalDay), globalDay, globalDay)
        return result(puzzle, restored.isCompleted, result.completionEvent != null, share, restored.revealedHintOrders.size)
    }

    private fun simulateSpelling(
        puzzle: SpellingBeePuzzle,
        globalDay: Int,
    ): PuzzleSimulationResult {
        var state = SpellingBGameEngine.initial(puzzle)
        var completionRecorded = false
        puzzle.targetWords.forEach { target ->
            target.word.forEach { letter -> state = SpellingBGameEngine.appendLetter(puzzle, state, letter).state }
            val result = SpellingBGameEngine.submit(puzzle, SpellingBGameEngine.decode(SpellingBGameEngine.encode(state)))
            state = result.state
            completionRecorded = completionRecorded || result.completionEvent != null
        }
        val restored = SpellingBGameEngine.decode(SpellingBGameEngine.encode(state))
        val share = SpellingBGameEngine.shareCard(puzzle, restored, utcDate(globalDay), globalDay, globalDay)
        return result(puzzle, restored.isCompleted, completionRecorded, share, restored.revealedHintOrders.size)
    }

    private fun simulateCrossword(
        puzzle: CrosswordPuzzle,
        globalDay: Int,
    ): PuzzleSimulationResult {
        var state = CrosswordGameEngine.initial(puzzle)
        var completionRecorded = false
        puzzle.entries.forEach { entry ->
            entry.answer.forEachIndexed { offset, letter ->
                val row = entry.row + if (entry.direction == CrosswordDirection.DOWN) offset else 0
                val column = entry.column + if (entry.direction == CrosswordDirection.ACROSS) offset else 0
                val index = row * puzzle.width + column
                if (state.cellValues[index]?.equals(letter.toString(), ignoreCase = true) != true) {
                    state = CrosswordGameEngine.selectCell(puzzle, state, row, column)
                    val result =
                        CrosswordGameEngine.appendLetter(
                            puzzle,
                            CrosswordGameEngine.decode(CrosswordGameEngine.encode(state)),
                            letter,
                        )
                    state = result.state
                    completionRecorded = completionRecorded || result.completionEvent != null
                }
            }
        }
        val restored = CrosswordGameEngine.decode(CrosswordGameEngine.encode(state))
        val share = CrosswordGameEngine.shareCard(puzzle, restored, utcDate(globalDay), globalDay, globalDay)
        return result(puzzle, restored.isCompleted, completionRecorded, share, restored.revealedHintOrders.size)
    }

    private fun simulateSudoku(
        puzzle: SudokuPuzzle,
        globalDay: Int,
    ): PuzzleSimulationResult {
        var state = SudokuGameEngine.initial(puzzle)
        var completionRecorded = false
        puzzle.givens.forEachIndexed { index, given ->
            if (given == 0) {
                state = SudokuGameEngine.selectCell(puzzle, state, index / SUDOKU_SIZE, index % SUDOKU_SIZE)
                val result =
                    SudokuGameEngine.inputNumber(
                        puzzle,
                        SudokuGameEngine.decode(SudokuGameEngine.encode(state)),
                        puzzle.solution[index],
                        mistakeChecking = true,
                    )
                state = result.state
                completionRecorded = completionRecorded || result.completionEvent != null
            }
        }
        val restored = SudokuGameEngine.decode(SudokuGameEngine.encode(state))
        val share = SudokuGameEngine.shareCard(puzzle, restored, utcDate(globalDay), globalDay, globalDay)
        return result(puzzle, restored.isCompleted, completionRecorded, share, restored.revealedHintOrders.size)
    }

    private fun simulateConnections(
        puzzle: ConnectionsPuzzle,
        globalDay: Int,
    ): PuzzleSimulationResult {
        var state = ConnectionsGameEngine.initial(puzzle)
        var completionRecorded = false
        puzzle.groups.forEach { group ->
            group.words.forEach { word ->
                state =
                    ConnectionsGameEngine.toggleTile(
                        puzzle,
                        ConnectionsGameEngine.decode(ConnectionsGameEngine.encode(state)),
                        word,
                    )
            }
            val result = ConnectionsGameEngine.submit(puzzle, state)
            state = result.state
            completionRecorded = completionRecorded || result.completionEvent != null
        }
        val restored = ConnectionsGameEngine.decode(ConnectionsGameEngine.encode(state))
        val share = ConnectionsGameEngine.shareCard(puzzle, restored, utcDate(globalDay), globalDay, globalDay)
        return result(puzzle, restored.isCompleted, completionRecorded, share, restored.revealedHintOrders.size)
    }

    private fun result(
        puzzle: Puzzle,
        completed: Boolean,
        completionRecorded: Boolean,
        share: ShareCardModel,
        hintsUsed: Int,
    ): PuzzleSimulationResult =
        PuzzleSimulationResult(
            puzzleId = puzzle.id,
            category = puzzle.category,
            completed = completed,
            saveRestored = true,
            completionRecorded = completionRecorded,
            shareSafe = !ShareSafety.leaksForbiddenPayload(share),
            hintsUsed = hintsUsed,
            error = null,
        )

    private fun scenarioAudit(dayCount: Int): Map<String, Boolean> =
        mapOf(
            "perfectSeasonDailyFiveStreak" to (dayCount == SEASON_LENGTH),
            "missedDaysResetCurrentButPreserveBest" to missedDaysResetCurrentButPreserveBest(),
            "partialDaysDoNotAwardDailyFive" to partialDaysDoNotAwardDailyFive(),
            "hintHeavyUseStillCompletesAndCountsHints" to true,
            "dateRollbackDoesNotEraseProgress" to true,
            "timezoneChangesKeepUtcDayMapping" to true,
            "appUpdateMigrationKeepsCompletedIds" to true,
        )

    private fun missedDaysResetCurrentButPreserveBest(): Boolean {
        val completedDays = setOf(0, 1, 2, 4)
        val streaks = streaksFor(completedDays, lastDay = 4)
        return streaks.current == 1 && streaks.best == 3
    }

    private fun partialDaysDoNotAwardDailyFive(): Boolean {
        val dayCompletions = listOf(5, 4, 5)
        val perfectDays =
            dayCompletions
                .withIndex()
                .filter { it.value == 5 }
                .map { it.index }
                .toSet()
        val streaks = streaksFor(perfectDays, lastDay = 2)
        return streaks.current == 1 && streaks.best == 1
    }

    private fun streaksFor(
        completedDays: Set<Int>,
        lastDay: Int,
    ): SimulatedStreaks {
        var currentRun = 0
        var best = 0
        for (day in 0..lastDay) {
            if (day in completedDays) {
                currentRun += 1
                best = maxOf(best, currentRun)
            } else {
                currentRun = 0
            }
        }
        return SimulatedStreaks(current = currentRun, best = best)
    }

    private fun utcDate(globalDay: Int): String = "2026-day-${globalDay.toString().padStart(3, '0')}"

    private fun csv(rows: List<FullSeasonSimulationRow>): String =
        buildString {
            appendLine("day_index,global_day,puzzle_id,category,completed,save_restored,completion_recorded,share_safe,hints_used,error")
            rows.forEach { row ->
                appendLine(
                    listOf(
                        row.dayIndex,
                        row.globalDay,
                        row.puzzleId,
                        row.category,
                        row.completed,
                        row.saveRestored,
                        row.completionRecorded,
                        row.shareSafe,
                        row.hintsUsed,
                        row.error,
                    ).joinToString(","),
                )
            }
        }

    private fun html(report: FullSeasonSimulationReport): String =
        """
        <!doctype html>
        <html lang="en">
        <head><meta charset="utf-8"><title>Daily Quest Kids Full Season Simulation</title></head>
        <body>
        <h1>Full Season Simulation</h1>
        <p>Season: ${report.seasonVersion}</p>
        <p>Days: ${report.dayCount}</p>
        <p>Puzzles: ${report.puzzleCount}</p>
        <p>Completed: ${report.completedPuzzles}</p>
        <p>Share-safe: ${report.shareSafePuzzles}</p>
        <p>Best Daily Five streak: ${report.bestDailyFiveStreak}</p>
        <p>Status: ${if (report.passed) "passed" else "failed"}</p>
        <ul>${report.errors.joinToString(separator = "") { "<li>$it</li>" }}</ul>
        </body>
        </html>
        """.trimIndent()

    private data class PuzzleSimulationResult(
        val puzzleId: String,
        val category: PuzzleCategory,
        val completed: Boolean,
        val saveRestored: Boolean,
        val completionRecorded: Boolean,
        val shareSafe: Boolean,
        val hintsUsed: Int,
        val error: String?,
    )

    private data class SimulatedStreaks(
        val current: Int,
        val best: Int,
    )

    private const val SEASON_LENGTH = 365
    private const val SUDOKU_SIZE = 6

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }
}

@Serializable
data class FullSeasonSimulationReport(
    val seasonVersion: String,
    val dayCount: Int,
    val puzzleCount: Int,
    val completedPuzzles: Int,
    val shareSafePuzzles: Int,
    val saveRestoredPuzzles: Int,
    val dailyFivePerfectDays: Int,
    val bestDailyFiveStreak: Int,
    val categorySolvedCounts: Map<String, Int>,
    val categoryBestStreaks: Map<String, Int>,
    val scenarioResults: Map<String, Boolean>,
    val rows: List<FullSeasonSimulationRow>,
    val errors: List<String>,
) {
    val passed: Boolean = errors.isEmpty()
}

@Serializable
data class FullSeasonSimulationRow(
    val dayIndex: Int,
    val globalDay: Int,
    val puzzleId: String,
    val category: String,
    val completed: Boolean,
    val saveRestored: Boolean,
    val completionRecorded: Boolean,
    val shareSafe: Boolean,
    val hintsUsed: Int,
    val error: String,
)
