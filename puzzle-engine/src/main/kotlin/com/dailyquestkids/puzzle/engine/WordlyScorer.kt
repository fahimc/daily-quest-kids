package com.dailyquestkids.puzzle.engine

object WordlyScorer {
    fun score(
        solution: String,
        guess: String,
    ): List<LetterScore> {
        require(solution.length == WORD_LENGTH) { "Solution must be $WORD_LENGTH letters." }
        require(guess.length == WORD_LENGTH) { "Guess must be $WORD_LENGTH letters." }

        val normalisedSolution = solution.lowercase()
        val normalisedGuess = guess.lowercase()
        val scores = MutableList(WORD_LENGTH) { LetterScore(normalisedGuess[it], TileState.ABSENT) }
        val remaining = mutableMapOf<Char, Int>()

        normalisedSolution.forEachIndexed { index, solutionChar ->
            val guessChar = normalisedGuess[index]
            if (guessChar == solutionChar) {
                scores[index] = LetterScore(guessChar, TileState.CORRECT)
            } else {
                remaining[solutionChar] = remaining.getOrDefault(solutionChar, 0) + 1
            }
        }

        normalisedGuess.forEachIndexed { index, guessChar ->
            if (scores[index].state == TileState.CORRECT) return@forEachIndexed
            val count = remaining.getOrDefault(guessChar, 0)
            if (count > 0) {
                scores[index] = LetterScore(guessChar, TileState.PRESENT)
                remaining[guessChar] = count - 1
            }
        }

        return scores
    }

    private const val WORD_LENGTH = 5
}

data class LetterScore(
    val letter: Char,
    val state: TileState,
)

enum class TileState {
    EMPTY,
    CORRECT,
    PRESENT,
    ABSENT,
}
