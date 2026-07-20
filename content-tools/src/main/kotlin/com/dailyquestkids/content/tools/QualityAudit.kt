package com.dailyquestkids.content.tools

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.readText
import kotlin.io.path.writeText

object QualityAudit {
    fun run(root: Path): QualityAuditReport {
        val checks =
            listOf(
                sourceContains(root, "app/src/main/AndroidManifest.xml", "android.permission.INTERNET").copy(
                    passed = !sourceText(root, "app/src/main/AndroidManifest.xml").contains("android.permission.INTERNET"),
                    details = "Manifest must not request internet access.",
                ),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/DailyQuestApp.kt", "High contrast"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/DailyQuestApp.kt", "Reduced motion"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/DailyQuestApp.kt", "Large puzzle text"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/DailyQuestApp.kt", "Parent information"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/DailyQuestApp.kt", "How to play"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/ShareCardRenderer.kt", "cleanupOldShareFiles"),
                sourceContains(root, "app/src/main/java/com/dailyquestkids/app/ShareCardRenderer.kt", "WIDTH = 1080"),
                reportContains(root, "reports/full-season-simulation.json", "\"passed\": true"),
                reportContains(root, "reports/season-one-validation.json", "\"passed\": true"),
                reportContains(root, "reports/season-one-validation.json", "\"releaseReady\": false"),
                sourceContains(root, "app/build.gradle.kts", "versionName"),
            )
        val manualBlockers =
            listOf(
                "Manual TalkBack traversal approval is still required.",
                "Final compact-phone, standard-phone and tablet VRT baseline approval is still required.",
                "Hardware macrobenchmark approval for cold start, navigation, sharing and memory is still required.",
                "Phase 14 human content review and repeated-content replacement remain release blockers.",
            )
        return QualityAuditReport(
            generatedAtHint = generatedAtHint(root),
            passed = checks.all { it.passed },
            automatedChecks = checks,
            manualBlockers = manualBlockers,
        )
    }

    fun writeReport(
        report: QualityAuditReport,
        outputRoot: Path,
    ) {
        val reports = outputRoot.resolve("reports")
        reports.createDirectories()
        reports.resolve("quality-audit.json").writeText(json.encodeToString(report))
        reports.resolve("quality-audit.html").writeText(html(report))
    }

    private fun sourceContains(
        root: Path,
        relativePath: String,
        needle: String,
    ): QualityAuditCheck {
        val text = sourceText(root, relativePath)
        return QualityAuditCheck(
            id = relativePath.substringAfterLast('/').substringBeforeLast('.') + "-contains-${needle.slug()}",
            passed = text.contains(needle),
            details = "$relativePath should contain `$needle`.",
        )
    }

    private fun reportContains(
        root: Path,
        relativePath: String,
        needle: String,
    ): QualityAuditCheck {
        val text = sourceText(root, relativePath)
        return QualityAuditCheck(
            id = relativePath.substringAfterLast('/').substringBeforeLast('.') + "-contains-${needle.slug()}",
            passed = text.contains(needle),
            details = "$relativePath should contain `$needle`.",
        )
    }

    private fun sourceText(
        root: Path,
        relativePath: String,
    ): String {
        val path = root.resolve(relativePath)
        return if (path.exists()) path.readText() else ""
    }

    private fun generatedAtHint(root: Path): String =
        runCatching { root.resolve("reports/full-season-simulation.json").getLastModifiedTime().toString() }
            .getOrElse { "unknown" }

    private fun String.slug(): String =
        lowercase()
            .filter { it.isLetterOrDigit() || it == '-' }
            .take(36)

    private fun html(report: QualityAuditReport): String =
        """
        <!doctype html>
        <html lang="en">
        <head><meta charset="utf-8"><title>Daily Quest Kids Quality Audit</title></head>
        <body>
        <h1>Quality Audit</h1>
        <p>Status: ${if (report.passed) "automated checks passed" else "automated checks failed"}</p>
        <h2>Automated Checks</h2>
        <ul>${report.automatedChecks.joinToString(separator = "") { "<li>${it.id}: ${it.passed}</li>" }}</ul>
        <h2>Manual Blockers</h2>
        <ul>${report.manualBlockers.joinToString(separator = "") { "<li>$it</li>" }}</ul>
        </body>
        </html>
        """.trimIndent()

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }
}

@Serializable
data class QualityAuditReport(
    val generatedAtHint: String,
    val passed: Boolean,
    val automatedChecks: List<QualityAuditCheck>,
    val manualBlockers: List<String>,
)

@Serializable
data class QualityAuditCheck(
    val id: String,
    val passed: Boolean,
    val details: String,
)
