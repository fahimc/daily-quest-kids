package com.dailyquestkids.content.tools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class QualityAuditTest {
    private val root: Path = repoRoot()

    @Test
    fun qualityAuditPassesAutomatedChecksAndKeepsManualBlockersExplicit() {
        val report = QualityAudit.run(root)

        assertTrue(report.passed)
        assertTrue(report.automatedChecks.all { it.passed })
        assertTrue(report.manualBlockers.any { it.contains("TalkBack") })
        assertTrue(report.manualBlockers.any { it.contains("human content review") })
        assertFalse(report.manualBlockers.isEmpty())
    }

    @Test
    fun qualityAuditWritesReports() {
        val report = QualityAudit.run(root)

        QualityAudit.writeReport(report, root)

        assertTrue(root.resolve("reports/quality-audit.json").exists())
        assertTrue(root.resolve("reports/quality-audit.html").readText().contains("Quality Audit"))
    }
}

private fun repoRoot(): Path {
    var current = Path.of("").toAbsolutePath().normalize()
    while (!current.resolve("settings.gradle.kts").exists()) {
        current = current.parent
    }
    return current
}
