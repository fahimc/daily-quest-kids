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
        fileStem: String = "season-preview",
        reportStem: String = "validation",
        requireFullSeason: Boolean = false,
    ): ContentPipelineResult {
        val directories = ContentPipelineDirectories(outputRoot, fileStem, reportStem)
        directories.create()
        val packJson = json.encodeToString(pack)
        val checksum = sha256(packJson)
        val releaseValidation = PuzzlePackValidator().validate(pack, requireFullSeason = requireFullSeason)
        val structuralErrors = releaseValidation.errors.filterNot { it.contains(HUMAN_REVIEW_ERROR) }
        val releaseAudit = ContentReleaseAuditor.audit(pack)
        val releaseBlockers = (releaseValidation.errors + releaseAudit.blockers).distinct()
        val summary =
            ContentPipelineSummary(
                seasonVersion = pack.seasonVersion,
                dayCount = pack.days.size,
                puzzleCount = pack.days.sumOf { it.puzzles.size },
                checksum = checksum,
                passed = structuralErrors.isEmpty(),
                releaseReady = releaseBlockers.isEmpty(),
                categoryCounts = releaseAudit.categoryCounts,
                humanReviewRequiredCount = releaseAudit.humanReviewRequiredCount,
                duplicateContentFingerprintCount = releaseAudit.duplicateContentFingerprintCount,
                errors = structuralErrors,
                releaseBlockers = releaseBlockers,
            )

        directories.packJson.writeText(packJson)
        directories.checksum.writeText("$checksum  $fileStem.json\n")
        directories.machineReport.writeText(json.encodeToString(summary))
        directories.csvReport.writeText(csvReport(pack))
        directories.humanReviewReport.writeText(humanReviewReport(pack))
        directories.htmlReport.writeText(htmlReport(summary))

        return ContentPipelineResult(summary, directories)
    }

    private fun ContentPipelineDirectories.create() {
        listOf(contentSource, puzzlePack, reports).forEach { directory -> directory.createDirectories() }
    }

    private fun csvReport(pack: PuzzlePack): String =
        buildString {
            appendLine("day_index,global_day,puzzle_id,category,difficulty,hints,automated_reviewed,human_reviewed")
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
                            puzzle.review.automatedReviewPassed,
                            puzzle.review.humanReviewed,
                        ).joinToString(","),
                    )
                }
            }
        }

    private fun humanReviewReport(pack: PuzzlePack): String =
        buildString {
            appendLine("day_index,global_day,puzzle_id,category,human_reviewed,reviewer,notes")
            pack.days.forEach { day ->
                day.puzzles.forEach { puzzle ->
                    appendLine(
                        listOf(
                            day.dayIndex,
                            day.globalDayNumber,
                            puzzle.id,
                            puzzle.category.name,
                            puzzle.review.humanReviewed,
                            puzzle.review.reviewer
                                .orEmpty()
                                .csvEscape(),
                            puzzle.review.notes
                                .orEmpty()
                                .csvEscape(),
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
        <p>Automated structural status: ${if (summary.passed) "passed" else "failed"}</p>
        <p>Release status: ${if (summary.releaseReady) "ready" else "blocked"}</p>
        <p>Human review required: ${summary.humanReviewRequiredCount}</p>
        <p>Repeated content fingerprints: ${summary.duplicateContentFingerprintCount}</p>
        <h2>Category Counts</h2>
        <ul>${summary.categoryCounts.entries.joinToString(separator = "") { "<li>${it.key}: ${it.value}</li>" }}</ul>
        <h2>Structural Errors</h2>
        <ul>${summary.errors.joinToString(separator = "") { "<li>${it.escapeHtml()}</li>" }}</ul>
        <h2>Release Blockers</h2>
        <ul>${summary.releaseBlockers.joinToString(separator = "") { "<li>${it.escapeHtml()}</li>" }}</ul>
        </body>
        </html>
        """.trimIndent()

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun String.csvEscape(): String =
        if (contains(",") || contains("\"") || contains("\n")) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private const val HUMAN_REVIEW_ERROR = "requires human review"

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
    val releaseReady: Boolean,
    val categoryCounts: Map<String, Int>,
    val humanReviewRequiredCount: Int,
    val duplicateContentFingerprintCount: Int,
    val errors: List<String>,
    val releaseBlockers: List<String>,
)

data class ContentPipelineDirectories(
    val root: Path,
    val fileStem: String = "season-preview",
    val reportStem: String = "validation",
) {
    val contentSource: Path = root.resolve("content-source")
    val puzzlePack: Path = root.resolve("puzzle-pack")
    val reports: Path = root.resolve("reports")
    val packJson: Path = puzzlePack.resolve("$fileStem.json")
    val checksum: Path = puzzlePack.resolve("$fileStem.sha256")
    val machineReport: Path = reports.resolve(if (reportStem == "validation") "validation.json" else "$reportStem-validation.json")
    val csvReport: Path = reports.resolve(if (reportStem == "validation") "puzzles.csv" else "$reportStem-puzzles.csv")
    val humanReviewReport: Path = reports.resolve(if (reportStem == "validation") "human-review.csv" else "$reportStem-human-review.csv")
    val htmlReport: Path = reports.resolve(if (reportStem == "validation") "validation.html" else "$reportStem-validation.html")
}

fun main(args: Array<String>) {
    val arguments = args.toMutableList()
    val seasonOneCandidate = arguments.remove("--season-one-candidate")
    val strictRelease = arguments.remove("--strict-release")
    val outputRoot =
        if (arguments.isEmpty()) {
            Path.of(".")
        } else {
            Path.of(arguments.joinToString(separator = " "))
        }
    val result =
        if (seasonOneCandidate) {
            ContentPipeline.run(
                pack = SeasonOneCandidateFactory.candidatePack(),
                outputRoot = outputRoot,
                fileStem = "season-one-candidate",
                reportStem = "season-one",
                requireFullSeason = true,
            )
        } else {
            ContentPipeline.run(outputRoot = outputRoot)
        }
    if (!result.summary.passed || (strictRelease && !result.summary.releaseReady)) {
        (result.summary.errors + result.summary.releaseBlockers).distinct().forEach { error -> System.err.println(error) }
        exitProcess(1)
    }
}
