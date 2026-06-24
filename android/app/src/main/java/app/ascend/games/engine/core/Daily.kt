package app.ascend.games.engine.core

import kotlin.random.Random

/**
 * Deterministic daily seeding.
 *
 * Every device that opens the same game on the same calendar day derives the
 * exact same puzzle, which is what makes "LinkedIn-style" daily puzzles + streaks
 * possible without any backend. Swap [epochDay] for a server-provided value later
 * if you want server-authoritative dailies.
 */
object Daily {

    /** A stable 64-bit seed for a given day + game name. */
    fun seed(epochDay: Long, game: String): Long {
        var h = epochDay * 0x9E3779B97F4A7C15uL.toLong()
        for (c in game) h = h * 31 + c.code
        // final avalanche so adjacent days/games look unrelated
        var x = h
        x = x xor (x ushr 30); x *= -0x40a7b892e31b1a47L
        x = x xor (x ushr 27); x *= -0x6b2fb644ecceee15L
        x = x xor (x ushr 31)
        return x
    }

    /**
     * A [Random] for a day/game. [salt] lets a generator retry with a fresh but
     * still-deterministic stream when its first attempt fails (e.g. no unique puzzle).
     */
    fun random(epochDay: Long, game: String, salt: Int = 0): Random =
        Random(seed(epochDay, game) + salt * 0x100000001B3L)
}
