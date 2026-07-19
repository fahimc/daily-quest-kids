package com.dailyquestkids.core.design

import com.dailyquestkids.core.model.PuzzleCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class DesignTokenTest {
    @Test
    fun everyCategoryHasDistinctInitialAndStyle() {
        val styles = PuzzleCategory.entries.map { categoryStyle(it) }

        assertEquals(PuzzleCategory.entries.size, styles.map { it.initial }.toSet().size)
    }
}
