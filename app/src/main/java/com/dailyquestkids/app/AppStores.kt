package com.dailyquestkids.app

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailyquestkids.core.common.StreakEngine
import com.dailyquestkids.core.model.DailyFiveProgress
import com.dailyquestkids.puzzle.engine.CrosswordGameEngine
import com.dailyquestkids.puzzle.engine.CrosswordSaveState
import com.dailyquestkids.puzzle.engine.SpellingBGameEngine
import com.dailyquestkids.puzzle.engine.SpellingBSaveState
import com.dailyquestkids.puzzle.engine.SudokuGameEngine
import com.dailyquestkids.puzzle.engine.SudokuSaveState
import com.dailyquestkids.puzzle.engine.WordlyGameEngine
import com.dailyquestkids.puzzle.engine.WordlySaveState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.questDataStore by preferencesDataStore(name = "daily_quest_kids")

class SettingsStore(
    private val context: Context,
) {
    val settings: Flow<QuestSettings> =
        context.questDataStore.data.map { preferences ->
            QuestSettings(
                onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
                soundEnabled = preferences[Keys.SOUND] ?: false,
                hapticsEnabled = preferences[Keys.HAPTICS] ?: true,
                reducedMotion = preferences[Keys.REDUCED_MOTION] ?: false,
                highContrast = preferences[Keys.HIGH_CONTRAST] ?: false,
                largePuzzleText = preferences[Keys.LARGE_PUZZLE_TEXT] ?: false,
                optionalTimer = preferences[Keys.OPTIONAL_TIMER] ?: false,
                mistakeChecking = preferences[Keys.MISTAKE_CHECKING] ?: true,
            )
        }

    suspend fun completeOnboarding() {
        update(Keys.ONBOARDING_COMPLETE, true)
    }

    suspend fun resetOnboarding() {
        update(Keys.ONBOARDING_COMPLETE, false)
    }

    suspend fun updateSound(enabled: Boolean) {
        update(Keys.SOUND, enabled)
    }

    suspend fun updateHaptics(enabled: Boolean) {
        update(Keys.HAPTICS, enabled)
    }

    suspend fun updateReducedMotion(enabled: Boolean) {
        update(Keys.REDUCED_MOTION, enabled)
    }

    suspend fun updateHighContrast(enabled: Boolean) {
        update(Keys.HIGH_CONTRAST, enabled)
    }

    suspend fun updateLargePuzzleText(enabled: Boolean) {
        update(Keys.LARGE_PUZZLE_TEXT, enabled)
    }

    suspend fun updateOptionalTimer(enabled: Boolean) {
        update(Keys.OPTIONAL_TIMER, enabled)
    }

    suspend fun updateMistakeChecking(enabled: Boolean) {
        update(Keys.MISTAKE_CHECKING, enabled)
    }

    private suspend fun update(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        context.questDataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}

class ProgressStore(
    private val context: Context,
) {
    val progress: Flow<StoredProgress> =
        context.questDataStore.data.map { preferences ->
            StoredProgress(
                greatestDayObserved = preferences[Keys.GREATEST_DAY_OBSERVED] ?: 0,
                startedPuzzleIds = preferences[Keys.STARTED_PUZZLES].orEmpty(),
                completedPuzzleIds = preferences[Keys.COMPLETED_PUZZLES].orEmpty(),
                failedPuzzleIds = preferences[Keys.FAILED_PUZZLES].orEmpty(),
                dailyFiveCompletedDays =
                    preferences[Keys.DAILY_FIVE_DAYS]
                        .orEmpty()
                        .mapNotNull { it.toIntOrNull() }
                        .toSet(),
            )
        }

    suspend fun observeDay(dayIndex: Int) {
        context.questDataStore.edit { preferences ->
            val current = preferences[Keys.GREATEST_DAY_OBSERVED] ?: 0
            preferences[Keys.GREATEST_DAY_OBSERVED] = maxOf(current, dayIndex)
        }
    }

    suspend fun markStarted(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            val started = preferences[Keys.STARTED_PUZZLES].orEmpty()
            preferences[Keys.STARTED_PUZZLES] = started + puzzleId
        }
    }

    suspend fun markCompleted(
        puzzleId: String,
        dayIndex: Int,
        todaysPuzzleIds: Set<String>,
    ) {
        context.questDataStore.edit { preferences ->
            val started = preferences[Keys.STARTED_PUZZLES].orEmpty()
            val completed = preferences[Keys.COMPLETED_PUZZLES].orEmpty() + puzzleId
            val failed = preferences[Keys.FAILED_PUZZLES].orEmpty() - puzzleId
            preferences[Keys.STARTED_PUZZLES] = started + puzzleId
            preferences[Keys.COMPLETED_PUZZLES] = completed
            preferences[Keys.FAILED_PUZZLES] = failed

            if (todaysPuzzleIds.isNotEmpty() && todaysPuzzleIds.all { it in completed }) {
                val days = preferences[Keys.DAILY_FIVE_DAYS].orEmpty()
                preferences[Keys.DAILY_FIVE_DAYS] = days + dayIndex.toString()
            }
        }
    }

    suspend fun markFailed(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            val started = preferences[Keys.STARTED_PUZZLES].orEmpty()
            val failed = preferences[Keys.FAILED_PUZZLES].orEmpty()
            preferences[Keys.STARTED_PUZZLES] = started + puzzleId
            preferences[Keys.FAILED_PUZZLES] = failed + puzzleId
        }
    }

    suspend fun resetProgress() {
        context.questDataStore.edit { preferences ->
            preferences.remove(Keys.STARTED_PUZZLES)
            preferences.remove(Keys.COMPLETED_PUZZLES)
            preferences.remove(Keys.FAILED_PUZZLES)
            preferences.remove(Keys.DAILY_FIVE_DAYS)
            preferences.remove(Keys.GREATEST_DAY_OBSERVED)
            preferences
                .asMap()
                .keys
                .filter { it.name.startsWith(Keys.WORDLY_STATE_PREFIX) }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<String>)
                }
            preferences
                .asMap()
                .keys
                .filter { it.name.startsWith(Keys.SPELLING_STATE_PREFIX) }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<String>)
                }
            preferences
                .asMap()
                .keys
                .filter { it.name.startsWith(Keys.CROSSWORD_STATE_PREFIX) }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<String>)
                }
            preferences
                .asMap()
                .keys
                .filter { it.name.startsWith(Keys.SUDOKU_STATE_PREFIX) }
                .forEach { key ->
                    @Suppress("UNCHECKED_CAST")
                    preferences.remove(key as Preferences.Key<String>)
                }
        }
    }

    fun dailyFiveProgress(progress: StoredProgress): DailyFiveProgress =
        progress.dailyFiveCompletedDays
            .sorted()
            .fold(
                DailyFiveProgress(
                    currentStreak = 0,
                    bestStreak = 0,
                    perfectDayCount = 0,
                    longestHistoricalStreak = 0,
                    completedDayIndices = emptySet(),
                ),
            ) { current, dayIndex ->
                StreakEngine.recordDailyFiveCompletion(current, dayIndex)
            }
}

class WordlyProgressStore(
    private val context: Context,
) {
    fun stateFor(puzzleId: String): Flow<WordlySaveState?> =
        context.questDataStore.data.map { preferences ->
            preferences[Keys.wordlyState(puzzleId)]?.let { payload ->
                runCatching { WordlyGameEngine.decode(payload) }.getOrNull()
            }
        }

    suspend fun save(state: WordlySaveState) {
        context.questDataStore.edit { preferences ->
            preferences[Keys.wordlyState(state.puzzleId)] = WordlyGameEngine.encode(state)
        }
    }

    suspend fun clear(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            preferences.remove(Keys.wordlyState(puzzleId))
        }
    }
}

class SpellingProgressStore(
    private val context: Context,
) {
    fun stateFor(puzzleId: String): Flow<SpellingBSaveState?> =
        context.questDataStore.data.map { preferences ->
            preferences[Keys.spellingState(puzzleId)]?.let { payload ->
                runCatching { SpellingBGameEngine.decode(payload) }.getOrNull()
            }
        }

    suspend fun save(state: SpellingBSaveState) {
        context.questDataStore.edit { preferences ->
            preferences[Keys.spellingState(state.puzzleId)] = SpellingBGameEngine.encode(state)
        }
    }

    suspend fun clear(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            preferences.remove(Keys.spellingState(puzzleId))
        }
    }
}

class CrosswordProgressStore(
    private val context: Context,
) {
    fun stateFor(puzzleId: String): Flow<CrosswordSaveState?> =
        context.questDataStore.data.map { preferences ->
            preferences[Keys.crosswordState(puzzleId)]?.let { payload ->
                runCatching { CrosswordGameEngine.decode(payload) }.getOrNull()
            }
        }

    suspend fun save(state: CrosswordSaveState) {
        context.questDataStore.edit { preferences ->
            preferences[Keys.crosswordState(state.puzzleId)] = CrosswordGameEngine.encode(state)
        }
    }

    suspend fun clear(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            preferences.remove(Keys.crosswordState(puzzleId))
        }
    }
}

class SudokuProgressStore(
    private val context: Context,
) {
    fun stateFor(puzzleId: String): Flow<SudokuSaveState?> =
        context.questDataStore.data.map { preferences ->
            preferences[Keys.sudokuState(puzzleId)]?.let { payload ->
                runCatching { SudokuGameEngine.decode(payload) }.getOrNull()
            }
        }

    suspend fun save(state: SudokuSaveState) {
        context.questDataStore.edit { preferences ->
            preferences[Keys.sudokuState(state.puzzleId)] = SudokuGameEngine.encode(state)
        }
    }

    suspend fun clear(puzzleId: String) {
        context.questDataStore.edit { preferences ->
            preferences.remove(Keys.sudokuState(puzzleId))
        }
    }
}

private object Keys {
    val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    val SOUND = booleanPreferencesKey("sound_enabled")
    val HAPTICS = booleanPreferencesKey("haptics_enabled")
    val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val LARGE_PUZZLE_TEXT = booleanPreferencesKey("large_puzzle_text")
    val OPTIONAL_TIMER = booleanPreferencesKey("optional_timer")
    val MISTAKE_CHECKING = booleanPreferencesKey("mistake_checking")
    val GREATEST_DAY_OBSERVED = intPreferencesKey("greatest_day_observed")
    val STARTED_PUZZLES = stringSetPreferencesKey("started_puzzle_ids")
    val COMPLETED_PUZZLES = stringSetPreferencesKey("completed_puzzle_ids")
    val FAILED_PUZZLES = stringSetPreferencesKey("failed_puzzle_ids")
    val DAILY_FIVE_DAYS = stringSetPreferencesKey("daily_five_days")
    const val WORDLY_STATE_PREFIX = "wordly_state_"
    const val SPELLING_STATE_PREFIX = "spelling_state_"
    const val CROSSWORD_STATE_PREFIX = "crossword_state_"
    const val SUDOKU_STATE_PREFIX = "sudoku_state_"

    fun wordlyState(puzzleId: String): Preferences.Key<String> = stringPreferencesKey("$WORDLY_STATE_PREFIX$puzzleId")

    fun spellingState(puzzleId: String): Preferences.Key<String> = stringPreferencesKey("$SPELLING_STATE_PREFIX$puzzleId")

    fun crosswordState(puzzleId: String): Preferences.Key<String> = stringPreferencesKey("$CROSSWORD_STATE_PREFIX$puzzleId")

    fun sudokuState(puzzleId: String): Preferences.Key<String> = stringPreferencesKey("$SUDOKU_STATE_PREFIX$puzzleId")
}
