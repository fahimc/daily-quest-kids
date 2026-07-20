package com.dailyquestkids.puzzle.engine

import com.dailyquestkids.core.model.ConnectionsPuzzle
import com.dailyquestkids.core.testing.FixturePackFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionsGameEngineTest {
    private val puzzle = firstConnectionsPuzzle()

    @Test
    fun initialStateShowsSixteenTilesAndNoSolvedGroups() {
        val state = ConnectionsGameEngine.initial(puzzle)

        assertEquals(16, ConnectionsGameEngine.tiles(puzzle, state).size)
        assertTrue(ConnectionsGameEngine.solvedGroups(puzzle, state).isEmpty())
        assertEquals(0, state.mistakeCount)
    }

    @Test
    fun exactGroupSubmissionSolvesGroupAndRemovesTiles() {
        val state = selectWords(ConnectionsGameEngine.initial(puzzle), puzzle.groups.first().words)
        val result = ConnectionsGameEngine.submit(puzzle, state)

        assertEquals(ConnectionsMessage.GROUP_SOLVED, result.message)
        assertEquals(listOf(puzzle.groups.first().title), result.state.solvedGroupTitles)
        assertEquals(12, ConnectionsGameEngine.tiles(puzzle, result.state).size)
        assertTrue(result.state.selectedWords.isEmpty())
    }

    @Test
    fun incompleteAndIncorrectGroupsAreRejected() {
        val incomplete = ConnectionsGameEngine.toggleTile(puzzle, ConnectionsGameEngine.initial(puzzle), puzzle.groups[0].words[0])
        val incompleteResult = ConnectionsGameEngine.submit(puzzle, incomplete)
        val mixedWords = listOf(puzzle.groups[0].words[0], puzzle.groups[1].words[0], puzzle.groups[2].words[0], puzzle.groups[3].words[0])
        val mixed = selectWords(ConnectionsGameEngine.initial(puzzle), mixedWords)
        val mixedResult = ConnectionsGameEngine.submit(puzzle, mixed)

        assertEquals(ConnectionsMessage.SELECT_FOUR, incompleteResult.message)
        assertEquals(ConnectionsMessage.INCORRECT, mixedResult.message)
        assertEquals(1, mixedResult.state.mistakeCount)
        assertTrue(mixedResult.state.lastMistakeWords.contains(mixedWords.first()))
    }

    @Test
    fun mistakeLimitFailsPuzzle() {
        var state = ConnectionsGameEngine.initial(puzzle)
        val mixedWords = listOf(puzzle.groups[0].words[0], puzzle.groups[1].words[0], puzzle.groups[2].words[0], puzzle.groups[3].words[0])
        repeat(5) {
            state = ConnectionsGameEngine.submit(puzzle, selectWords(state, mixedWords)).state
        }

        assertTrue(state.isFailed)
        assertEquals(5, state.mistakeCount)
        assertEquals(ConnectionsMessage.FAILED, ConnectionsGameEngine.submit(puzzle, state).message)
    }

    @Test
    fun hintsSelectPairRevealTitleAndCanRevealGroup() {
        var state = ConnectionsGameEngine.initial(puzzle)
        val first = ConnectionsGameEngine.revealHint(puzzle, state)
        state = first.state
        val second = ConnectionsGameEngine.revealHint(puzzle, state)
        state = second.state
        val third = ConnectionsGameEngine.revealHint(puzzle, state)
        state = third.state
        val confirm = ConnectionsGameEngine.revealHint(puzzle, state)
        val revealed = ConnectionsGameEngine.revealHint(puzzle, confirm.state, confirmReveal = true)

        assertEquals(ConnectionsMessage.HINT_REVEALED, first.message)
        assertEquals(2, second.state.selectedWords.size)
        assertNotNull(third.state.highlightedGroupTitle)
        assertEquals(ConnectionsMessage.CONFIRM_REVEAL, confirm.message)
        assertEquals(ConnectionsMessage.GROUP_REVEALED, revealed.message)
        assertEquals(1, revealed.state.solvedGroupTitles.size)
    }

    @Test
    fun shufflePreservesSelectionAndRemainingWords() {
        val initial = ConnectionsGameEngine.initial(puzzle)
        val firstWord =
            puzzle
                .groups
                .first()
                .words
                .first()
        val selected = ConnectionsGameEngine.toggleTile(puzzle, initial, firstWord)
        val shuffled = ConnectionsGameEngine.shuffle(puzzle, selected)

        assertEquals(selected.selectedWords, shuffled.selectedWords)
        assertNotEquals(initial.tileOrder, shuffled.tileOrder)
        assertEquals(
            ConnectionsGameEngine.tiles(puzzle, selected).map { it.word }.toSet(),
            ConnectionsGameEngine.tiles(puzzle, shuffled).map { it.word }.toSet(),
        )
    }

    @Test
    fun saveRestoreRoundTripKeepsState() {
        val state = selectWords(ConnectionsGameEngine.initial(puzzle), puzzle.groups.first().words)
        val solved = ConnectionsGameEngine.submit(puzzle, state).state
        val decoded = ConnectionsGameEngine.decode(ConnectionsGameEngine.encode(solved))

        assertEquals(solved, decoded)
    }

    @Test
    fun completionEmitsOnceAndShareCardDoesNotLeakAnswers() {
        val completed = completedState()
        val event = ConnectionsGameEngine.pendingCompletion(completed)
        val acknowledged = ConnectionsGameEngine.acknowledgeCompletion(completed)
        val share =
            ConnectionsGameEngine.shareCard(
                puzzle = puzzle,
                state = acknowledged,
                utcDate = "2026-07-20",
                currentStreak = 3,
                bestStreak = 5,
            )

        assertTrue(completed.isCompleted)
        assertNotNull(event)
        assertEquals(null, ConnectionsGameEngine.pendingCompletion(acknowledged))
        assertFalse(ShareSafety.leaksForbiddenPayload(share))
        assertTrue(share.visibleResultPattern.contains("Connections 001"))
        assertTrue(share.visibleResultPattern.contains("Groups 4/4"))
        assertTrue(share.visibleResultPattern.contains("Mistakes 0/5"))
        assertTrue(share.visibleResultPattern.contains("Hints 0"))
        assertTrue(share.visibleResultPattern.contains("Streak 3"))
    }

    @Test
    fun phasePreviewIncludesAtLeastTwentyReviewedConnectionsFixtures() {
        val connections =
            FixturePackFactory
                .phasePreviewPack()
                .days
                .map { day -> day.puzzles.filterIsInstance<ConnectionsPuzzle>().single() }

        assertTrue(connections.size >= 20)
        assertTrue(connections.all { it.review.humanReviewed })
        assertTrue(connections.map { it.groups.first().title }.toSet().size >= 20)
        assertTrue(connections.all { puzzle -> puzzle.groups.flatMap { it.words }.size == 16 })
    }

    private fun completedState(): ConnectionsSaveState {
        var state = ConnectionsGameEngine.initial(puzzle)
        puzzle.groups.forEach { group ->
            state = ConnectionsGameEngine.submit(puzzle, selectWords(state, group.words)).state
        }
        return state
    }

    private fun selectWords(
        state: ConnectionsSaveState,
        words: List<String>,
    ): ConnectionsSaveState {
        var next = state
        words.forEach { word ->
            next = ConnectionsGameEngine.toggleTile(puzzle, next, word)
        }
        return next
    }

    private fun firstConnectionsPuzzle(): ConnectionsPuzzle =
        FixturePackFactory
            .phasePreviewPack()
            .days
            .first()
            .puzzles
            .filterIsInstance<ConnectionsPuzzle>()
            .single()
}
