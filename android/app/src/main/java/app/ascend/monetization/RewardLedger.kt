package app.ascend.monetization

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent per-day counters for rewarded unlocks: how many free allowances and
 * how many ad-backed grants each placement has used today (CLAUDE.md rule 5 caps).
 *
 * Stored as one JSON blob in DataStore and reset whenever the calendar day rolls
 * over (the stored [State.day] != today). Counters survive process death so a user
 * can't farm extra unlocks by relaunching.
 */
@Singleton
class RewardLedger @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    @Serializable
    data class State(
        val day: String = "",
        val rewarded: Map<String, Int> = emptyMap(),
        val free: Map<String, Int> = emptyMap(),
        val appOpen: Int = 0,
    )

    private val key = stringPreferencesKey("reward_ledger")
    private val json = Json { ignoreUnknownKeys = true }

    private fun today(): String = LocalDate.now().toString()

    /** Today's ledger (an empty one if the stored day has rolled over). */
    suspend fun snapshot(): State {
        val today = today()
        val raw = dataStore.data.first()[key]
        val parsed = raw?.let { runCatching { json.decodeFromString<State>(it) }.getOrNull() } ?: State()
        return if (parsed.day == today) parsed else State(day = today)
    }

    fun rewardedToday(state: State, placementId: String): Int = state.rewarded[placementId] ?: 0
    fun freeToday(state: State, placementId: String): Int = state.free[placementId] ?: 0
    fun totalRewardedToday(state: State): Int = state.rewarded.values.sum()
    fun appOpenToday(state: State): Int = state.appOpen

    /** Record one free allowance use for [placementId] (atomic read-modify-write). */
    suspend fun incrementFree(placementId: String) = mutate { s ->
        s.copy(free = s.free + (placementId to (s.free[placementId] ?: 0) + 1))
    }

    /** Record one ad-backed grant for [placementId] (atomic read-modify-write). */
    suspend fun incrementRewarded(placementId: String) = mutate { s ->
        s.copy(rewarded = s.rewarded + (placementId to (s.rewarded[placementId] ?: 0) + 1))
    }

    /** Record one app-open impression today (atomic read-modify-write). */
    suspend fun incrementAppOpen() = mutate { s -> s.copy(appOpen = s.appOpen + 1) }

    private suspend fun mutate(transform: (State) -> State) {
        val today = today()
        dataStore.edit { prefs ->
            val cur = prefs[key]?.let { runCatching { json.decodeFromString<State>(it) }.getOrNull() } ?: State()
            val base = if (cur.day == today) cur else State(day = today)
            prefs[key] = json.encodeToString(transform(base))
        }
    }
}
