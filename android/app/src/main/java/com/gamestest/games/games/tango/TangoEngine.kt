package com.gamestest.games.games.tango

import kotlin.random.Random

/** Constraint shown on the edge between two adjacent cells. */
enum class Edge { NONE, EQUAL, DIFF }

/**
 * Tango. Fill a [size]x[size] grid with two symbols (0 = Sun, 1 = Moon) so that:
 *  - every row and column has an equal count of each symbol,
 *  - no three of the same symbol are consecutive (row or column),
 *  - `=` edges join equal cells and `x` edges join different cells.
 *
 * [givens] is length size*size, -1 = empty.
 * [hEdges] has size*(size-1) entries; edge between (r,c)-(r,c+1) is at r*(size-1)+c.
 * [vEdges] has (size-1)*size entries; edge between (r,c)-(r+1,c) is at r*size+c.
 */
data class TangoPuzzle(
    val size: Int,
    val givens: IntArray,
    val hEdges: Array<Edge>,
    val vEdges: Array<Edge>,
    val solution: IntArray
) {
    fun hEdge(r: Int, c: Int) = hEdges[r * (size - 1) + c]
    fun vEdge(r: Int, c: Int) = vEdges[r * size + c]
}

object TangoEngine {

    fun generate(rng: Random, size: Int = 6): TangoPuzzle {
        require(size % 2 == 0) { "Tango size must be even" }
        val solution = solveFull(rng, size) ?: error("no tango solution")

        val hEdges = Array(size * (size - 1)) { Edge.NONE }
        val vEdges = Array((size - 1) * size) { Edge.NONE }

        // Seed a sparse set of edge constraints drawn from the solution.
        for (r in 0 until size) for (c in 0 until size - 1) {
            if (rng.nextInt(100) < 16) {
                hEdges[r * (size - 1) + c] =
                    if (solution[r * size + c] == solution[r * size + c + 1]) Edge.EQUAL else Edge.DIFF
            }
        }
        for (r in 0 until size - 1) for (c in 0 until size) {
            if (rng.nextInt(100) < 16) {
                vEdges[r * size + c] =
                    if (solution[r * size + c] == solution[(r + 1) * size + c]) Edge.EQUAL else Edge.DIFF
            }
        }

        // Start from the full solution and remove cells while the puzzle stays unique.
        val givens = solution.copyOf()
        val cells = (0 until size * size).toMutableList().also { it.shuffle(rng) }
        for (cell in cells) {
            val backup = givens[cell]
            givens[cell] = -1
            if (count(size, givens.copyOf(), hEdges, vEdges, 2) != 1) {
                givens[cell] = backup
            }
        }
        return TangoPuzzle(size, givens, hEdges, vEdges, solution)
    }

    /** Generate a random complete, valid board (no edge constraints). */
    private fun solveFull(rng: Random, size: Int): IntArray? {
        val g = IntArray(size * size) { -1 }
        val noEdgesH = Array(size * (size - 1)) { Edge.NONE }
        val noEdgesV = Array((size - 1) * size) { Edge.NONE }
        fun rec(idx: Int): Boolean {
            if (idx == size * size) return true
            for (v in if (rng.nextBoolean()) intArrayOf(0, 1) else intArrayOf(1, 0)) {
                if (valid(size, g, noEdgesH, noEdgesV, idx, v)) {
                    g[idx] = v
                    if (rec(idx + 1)) return true
                    g[idx] = -1
                }
            }
            return false
        }
        return if (rec(0)) g else null
    }

    /** Count solutions up to [cap] honoring givens, edges, balance, and no-triples. */
    private fun count(size: Int, g: IntArray, h: Array<Edge>, v: Array<Edge>, cap: Int): Int {
        val idx = g.indexOf(-1)
        if (idx == -1) return 1
        var cnt = 0
        for (value in 0..1) {
            if (valid(size, g, h, v, idx, value)) {
                g[idx] = value
                cnt += count(size, g, h, v, cap)
                g[idx] = -1
                if (cnt >= cap) return cnt
            }
        }
        return cnt
    }

    /**
     * Is placing [value] at [idx] consistent with everything decided so far?
     * Cells are filled row-major, so we only check already-filled neighbors
     * (left/up) and backward-looking triples; forward conflicts surface later.
     */
    fun valid(size: Int, g: IntArray, h: Array<Edge>, v: Array<Edge>, idx: Int, value: Int): Boolean {
        val r = idx / size
        val c = idx % size

        // No three-in-a-row, looking backward.
        if (c >= 2 && g[idx - 1] == value && g[idx - 2] == value) return false
        if (r >= 2 && g[idx - size] == value && g[idx - 2 * size] == value) return false

        // Balance: never exceed half of each symbol per row/column.
        val half = size / 2
        var rowCount = 0
        for (cc in 0 until size) if (g[r * size + cc] == value) rowCount++
        if (rowCount + 1 > half) return false
        var colCount = 0
        for (rr in 0 until size) if (g[rr * size + c] == value) colCount++
        if (colCount + 1 > half) return false

        // Edge constraints with filled left/up neighbors.
        if (c > 0) {
            val left = g[idx - 1]
            when (h[r * (size - 1) + (c - 1)]) {
                Edge.EQUAL -> if (left != -1 && left != value) return false
                Edge.DIFF -> if (left != -1 && left == value) return false
                Edge.NONE -> {}
            }
        }
        if (r > 0) {
            val up = g[idx - size]
            when (v[(r - 1) * size + c]) {
                Edge.EQUAL -> if (up != -1 && up != value) return false
                Edge.DIFF -> if (up != -1 && up == value) return false
                Edge.NONE -> {}
            }
        }
        return true
    }
}
