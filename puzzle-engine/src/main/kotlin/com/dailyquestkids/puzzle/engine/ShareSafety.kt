package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ShareCardModel

object ShareSafety {
    fun leaksForbiddenPayload(model: ShareCardModel): Boolean {
        val visible =
            listOf(
                model.brand,
                model.utcDate,
                model.cardType.name,
                model.visibleResultPattern,
                model.hintsUsed.toString(),
                model.currentStreak.toString(),
                model.bestStreak.toString(),
            ).joinToString(separator = "\n").lowercase()

        return model.forbiddenPayloads
            .filter { it.isNotBlank() }
            .any { forbidden -> forbidden.lowercase() in visible }
    }
}
