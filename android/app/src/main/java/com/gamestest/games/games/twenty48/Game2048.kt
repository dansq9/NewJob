package com.gamestest.games.games.twenty48

import kotlin.random.Random

enum class Dir { UP, DOWN, LEFT, RIGHT }

/**
 * Pure-Kotlin 2048 engine on a 4x4 board (row-major, 0 = empty).
 *
 * [slide] performs a single move without spawning a tile; the caller spawns
 * afterward only if the board actually moved. Spawning is driven by a [Random]
 * so a daily seed yields the same tile stream for everyone.
 */
object Game2048 {
    const val SIZE = 4
    const val WIN_TILE = 2048

    data class SlideResult(val board: IntArray, val moved: Boolean, val gained: Int)

    fun emptyBoard(): IntArray = IntArray(SIZE * SIZE)

    /** Place a 2 (90%) or 4 (10%) on a random empty cell. Returns false if full. */
    fun spawn(board: IntArray, rng: Random): Boolean {
        val empties = (0 until SIZE * SIZE).filter { board[it] == 0 }
        if (empties.isEmpty()) return false
        board[empties[rng.nextInt(empties.size)]] = if (rng.nextInt(10) == 0) 4 else 2
        return true
    }

    fun slide(board: IntArray, dir: Dir): SlideResult {
        val res = board.copyOf()
        var moved = false
        var gained = 0
        for (line in lines(dir)) {
            val vals = ArrayList<Int>(SIZE)
            for (idx in line) if (res[idx] != 0) vals.add(res[idx])
            val merged = ArrayList<Int>(SIZE)
            var i = 0
            while (i < vals.size) {
                if (i + 1 < vals.size && vals[i] == vals[i + 1]) {
                    val m = vals[i] * 2; merged.add(m); gained += m; i += 2
                } else {
                    merged.add(vals[i]); i++
                }
            }
            while (merged.size < line.size) merged.add(0)
            for (k in line.indices) {
                if (res[line[k]] != merged[k]) moved = true
                res[line[k]] = merged[k]
            }
        }
        return SlideResult(res, moved, gained)
    }

    /** Cell indices for each line, ordered toward the direction of travel (front first). */
    private fun lines(dir: Dir): List<IntArray> = buildList {
        when (dir) {
            Dir.LEFT -> for (r in 0 until SIZE) add(IntArray(SIZE) { r * SIZE + it })
            Dir.RIGHT -> for (r in 0 until SIZE) add(IntArray(SIZE) { r * SIZE + (SIZE - 1 - it) })
            Dir.UP -> for (c in 0 until SIZE) add(IntArray(SIZE) { it * SIZE + c })
            Dir.DOWN -> for (c in 0 until SIZE) add(IntArray(SIZE) { (SIZE - 1 - it) * SIZE + c })
        }
    }

    /** Any move available? */
    fun canMove(board: IntArray): Boolean {
        if (board.any { it == 0 }) return true
        for (dir in Dir.entries) if (slide(board, dir).moved) return true
        return false
    }

    fun hasTile(board: IntArray, value: Int): Boolean = board.any { it >= value }

    fun maxTile(board: IntArray): Int = board.max()
}
