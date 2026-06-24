package com.gamestest.games.core

import android.content.Context
import java.time.LocalDate

/**
 * Tiny SharedPreferences-backed store for daily completion + streaks.
 *
 * Kept intentionally simple and dependency-free so it ports cleanly into the host
 * app. Swap for DataStore / your own repository if you prefer.
 */
class GameProgress private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("games_progress", Context.MODE_PRIVATE)

    data class Stats(val streak: Int, val bestSeconds: Int, val doneToday: Boolean, val bestScore: Int)

    fun stats(gameId: String, today: Long = LocalDate.now().toEpochDay()): Stats {
        val last = prefs.getLong(key(gameId, "last"), Long.MIN_VALUE)
        val streak = prefs.getInt(key(gameId, "streak"), 0)
        val best = prefs.getInt(key(gameId, "best"), 0)
        return Stats(
            streak = if (last == today || last == today - 1) streak else 0,
            bestSeconds = best,
            doneToday = last == today,
            bestScore = prefs.getInt(key(gameId, "bestScore"), 0),
        )
    }

    /** For score-based games (2048): keep the best score regardless of solve. */
    fun recordScore(gameId: String, score: Int) {
        if (score > prefs.getInt(key(gameId, "bestScore"), 0)) {
            prefs.edit().putInt(key(gameId, "bestScore"), score).apply()
        }
    }

    /** Record a solve for [today]; updates streak (consecutive days) and best time. */
    fun recordCompletion(gameId: String, elapsedSeconds: Int, today: Long = LocalDate.now().toEpochDay()) {
        val last = prefs.getLong(key(gameId, "last"), Long.MIN_VALUE)
        if (last == today) {
            // already counted today; only improve best time
            improveBest(gameId, elapsedSeconds)
            return
        }
        val prevStreak = prefs.getInt(key(gameId, "streak"), 0)
        val newStreak = if (last == today - 1) prevStreak + 1 else 1
        prefs.edit()
            .putLong(key(gameId, "last"), today)
            .putInt(key(gameId, "streak"), newStreak)
            .apply()
        improveBest(gameId, elapsedSeconds)
    }

    private fun improveBest(gameId: String, seconds: Int) {
        val best = prefs.getInt(key(gameId, "best"), 0)
        if (best == 0 || seconds < best) prefs.edit().putInt(key(gameId, "best"), seconds).apply()
    }

    private fun key(gameId: String, field: String) = "${gameId}_$field"

    companion object {
        @Volatile private var instance: GameProgress? = null
        fun get(context: Context): GameProgress =
            instance ?: synchronized(this) {
                instance ?: GameProgress(context).also { instance = it }
            }
    }
}
