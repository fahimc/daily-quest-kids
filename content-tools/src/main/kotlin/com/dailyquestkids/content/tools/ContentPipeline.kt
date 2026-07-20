package com.dailyquestkids.content.tools

import com.dailyquestkids.core.model.PuzzlePack
import com.dailyquestkids.core.testing.FixturePackFactory
import com.dailyquestkids.puzzle.validator.PuzzlePackValidator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.system.exitProcess

object ContentPipeline {
    fun run(
        pack: PuzzlePack = FixturePackFactory.phasePreviewPack(),
        outputRoot: Path,
    ): ContentPipelineResult {
        val directories = ContentPipelineDirectories(outputRoot)
        directories.create()
        val packJson = json.encodeToString(pack)
        val checksum = sha256(packJson)
        val validation = PuzzlePackValidator().validate(pack)
        val summary =
            ContentPipelineSummary(
                seasonVersion = pack.seasonVersion,
                dayCount = pack.days.size,
                puzzleCount = pack.days.sumOf { it.puzzles.size },
                checksum = checksum,
                passed = validation.passed,
                errors = validation.errors,
            )

        directories.packJson.writeText(packJson)
        directories.checksum.writeText("$checksum  season-preview.json\n")
        directories.machineReport.writeText(json.encodeToString(summary))
        directories.csvReport.writeText(csvReport(pack))
        directories.htmlReport.writeText(htmlReport(summary))

        return ContentPipelineResult(summary, directories)
    }

    private fun ContentPipelineDirectories.create() {
        listOf(contentSource, puzzlePack, reports).forEach { directory -> directory.createDirectories() }
    }

    private fun csvReport(pack: PuzzlePack): String =
        buildString {
            appendLine("day_index,global_day,puzzle_id,category,difficulty,hints,human_reviewed")
            pack.days.forEach { day ->
                day.puzzles.forEach { puzzle ->
                    appendLine(
                        listOf(
                            day.dayIndex,
                            day.globalDayNumber,
                            puzzle.id,
                            puzzle.category.name,
                            puzzle.difficulty.name,
                            puzzle.hints.size,
                            puzzle.review.humanReviewed,
                        ).joinToString(","),
                    )
                }
            }
        }

    private fun htmlReport(summary: ContentPipelineSummary): String =
        """
        <!doctype html>
        <html lang="en">
        <head><meta charset="utf-8"><title>Daily Quest Kids Content Report</title></head>
        <body>
        <h1>Daily Quest Kids Content Report</h1>
        <p>Season: ${summary.seasonVersion}</p>
        <p>Days: ${summary.dayCount}</p>
        <p>Puzzles: ${summary.puzzleCount}</p>
        <p>Checksum: ${summary.checksum}</p>
        <p>Status: ${if (summary.passed) "passed" else "failed"}</p>
        <ul>${summary.errors.joinToString(separator = "") { "<li>${it.escapeHtml()}</li>" }}</ul>
        </body>
        </html>
        """.trimIndent()

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }
}

data class ContentPipelineResult(
    val summary: ContentPipelineSummary,
    val directories: ContentPipelineDirectories,
)

@kotlinx.serialization.Serializable
data class ContentPipelineSummary(
    val seasonVersion: String,
    val dayCount: Int,
    val puzzleCount: Int,
    val checksum: String,
    val passed: Boolean,
    val errors: List<String>,
)

data class ContentPipelineDirectories(
    val root: Path,
) {
    val contentSource: Path = root.resolve("content-source")
    val puzzlePack: Path = root.resolve("puzzle-pack")
    val reports: Path = root.resolve("reports")
    val packJson: Path = puzzlePack.resolve("season-preview.json")
    val checksum: Path = puzzlePack.resolve("season-preview.sha256")
    val machineReport: Path = reports.resolve("validation.json")
    val csvReport: Path = reports.resolve("puzzles.csv")
    val htmlReport: Path = reports.resolve("validation.html")
}

fun main(args: Array<String>) {
    val outputRoot =
        if (args.isEmpty()) {
            Path.of(".")
        } else {
            Path.of(args.joinToString(separator = " "))
        }
    val result = ContentPipeline.run(outputRoot = outputRoot)
    if (!result.summary.passed) {
        result.summary.errors.forEach { error -> System.err.println(error) }
        exitProcess(1)
    }
}
