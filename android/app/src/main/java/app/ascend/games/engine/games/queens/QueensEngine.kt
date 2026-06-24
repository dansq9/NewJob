package app.ascend.games.engine.games.queens

import kotlin.math.abs
import kotlin.random.Random

/**
 * Queens (LinkedIn-style).
 *
 * Place exactly one queen in every row, every column, and every colored region,
 * with no two queens in touching cells (including diagonally). Queens MAY share a
 * long diagonal as long as they are not adjacent.
 *
 * [region] is length n*n, row-major, each entry a region id in 0 until n.
 * [solution] maps each row to the column of its queen.
 */
data class QueensPuzzle(
    val n: Int,
    val region: IntArray,
    val solution: IntArray
) {
    fun regionAt(r: Int, c: Int) = region[r * n + c]

    override fun equals(other: Any?): Boolean =
        this === other || (other is QueensPuzzle && n == other.n &&
            region.contentEquals(other.region) && solution.contentEquals(other.solution))

    override fun hashCode(): Int = (n * 31 + region.contentHashCode()) * 31 + solution.contentHashCode()
}

object QueensEngine {

    /** Generate a uniquely-solvable Queens board of size [n]. */
    fun generate(rng: Random, n: Int = 8): QueensPuzzle {
        repeat(60_000) {
            val sol = randomPlacement(rng, n) ?: return@repeat
            var region = growRegions(rng, n, sol)
            // Try to repair toward uniqueness by re-cutting cells that enable
            // alternate solutions before giving up on this placement.
            repeat(40) {
                if (countSolutions(n, region, IntArray(n) { -1 }, 0,
                        BooleanArray(n), BooleanArray(n), cap = 2) == 1
                ) {
                    return QueensPuzzle(n, region, sol)
                }
                val alt = findAlternate(n, region, sol) ?: return@repeat
                if (!recut(rng, n, region, sol, alt)) return@repeat
            }
        }
        // Extremely unlikely fallback: a valid (maybe non-unique) board.
        val sol = randomPlacement(rng, n) ?: IntArray(n) { it }
        return QueensPuzzle(n, growRegions(rng, n, sol), sol)
    }

    /**
     * Find a solution other than [sol], if one exists. Returns row->col or null.
     */
    private fun findAlternate(n: Int, region: IntArray, sol: IntArray): IntArray? {
        val cols = IntArray(n) { -1 }
        fun rec(r: Int, colUsed: BooleanArray, regUsed: BooleanArray): Boolean {
            if (r == n) return !cols.contentEquals(sol)
            for (c in 0 until n) {
                if (colUsed[c]) continue
                val reg = region[r * n + c]
                if (regUsed[reg]) continue
                if (r > 0 && abs(cols[r - 1] - c) <= 1) continue
                cols[r] = c; colUsed[c] = true; regUsed[reg] = true
                if (rec(r + 1, colUsed, regUsed)) return true
                colUsed[c] = false; regUsed[reg] = false
            }
            return false
        }
        return if (rec(0, BooleanArray(n), BooleanArray(n))) cols.copyOf() else null
    }

    /**
     * Break an alternate solution by moving a non-queen cell it relies on into a
     * neighbouring region, keeping every region contiguous and queen-covered.
     */
    private fun recut(rng: Random, n: Int, region: IntArray, sol: IntArray, alt: IntArray): Boolean {
        val rows = (0 until n).filter { alt[it] != sol[it] }.shuffled(rng)
        for (r in rows) {
            val cell = r * n + alt[r]
            if (cell == r * n + sol[r]) continue
            val from = region[cell]
            // candidate target regions: those owning an orthogonal neighbour
            val r0 = cell / n; val c0 = cell % n
            val targets = listOf(r0 - 1 to c0, r0 + 1 to c0, r0 to c0 - 1, r0 to c0 + 1)
                .filter { (nr, nc) -> nr in 0 until n && nc in 0 until n }
                .map { (nr, nc) -> region[nr * n + nc] }
                .filter { it != from }
                .distinct().shuffled(rng)
            for (t in targets) {
                region[cell] = t
                if (regionContiguous(n, region, from) && coversQueen(n, region, sol)) return true
                region[cell] = from
            }
        }
        return false
    }

    private fun coversQueen(n: Int, region: IntArray, sol: IntArray): Boolean =
        (0 until n).map { region[it * n + sol[it]] }.toSet().size == n

    private fun regionContiguous(n: Int, region: IntArray, reg: Int): Boolean {
        val cells = (0 until n * n).filter { region[it] == reg }
        if (cells.isEmpty()) return false
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>().apply { add(cells[0]) }
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            if (!seen.add(c)) continue
            val r0 = c / n; val c0 = c % n
            for ((nr, nc) in listOf(r0 - 1 to c0, r0 + 1 to c0, r0 to c0 - 1, r0 to c0 + 1))
                if (nr in 0 until n && nc in 0 until n && region[nr * n + nc] == reg) stack.add(nr * n + nc)
        }
        return seen.size == cells.size
    }

    /**
     * A valid queen placement: a column permutation where consecutive rows differ
     * by >= 2 columns (that is the only way two one-per-row queens can touch).
     */
    private fun randomPlacement(rng: Random, n: Int): IntArray? {
        val cols = IntArray(n) { -1 }
        val used = BooleanArray(n)
        fun place(r: Int): Boolean {
            if (r == n) return true
            val order = (0 until n).toMutableList().also { it.shuffle(rng) }
            for (c in order) {
                if (used[c]) continue
                if (r > 0 && abs(cols[r - 1] - c) < 2) continue
                cols[r] = c; used[c] = true
                if (place(r + 1)) return true
                used[c] = false; cols[r] = -1
            }
            return false
        }
        return if (place(0)) cols else null
    }

    /**
     * Grow [n] contiguous regions by randomized flood fill, each seeded at one
     * queen so every region contains exactly one queen.
     */
    private fun growRegions(rng: Random, n: Int, sol: IntArray): IntArray {
        val region = IntArray(n * n) { -1 }
        val frontier = ArrayList<Int>()          // packed cell*n + regionId
        fun idx(r: Int, c: Int) = r * n + c
        fun addNeighbors(cell: Int, reg: Int) {
            val r = cell / n; val c = cell % n
            val nb = intArrayOf(
                if (r > 0) idx(r - 1, c) else -1,
                if (r < n - 1) idx(r + 1, c) else -1,
                if (c > 0) idx(r, c - 1) else -1,
                if (c < n - 1) idx(r, c + 1) else -1,
            )
            for (ni in nb) if (ni >= 0 && region[ni] == -1) frontier.add(ni * n + reg)
        }
        for (r in 0 until n) { region[idx(r, sol[r])] = r }
        for (r in 0 until n) addNeighbors(idx(r, sol[r]), r)
        while (frontier.isNotEmpty()) {
            val packed = frontier.removeAt(rng.nextInt(frontier.size))
            val cell = packed / n
            val reg = packed % n
            if (region[cell] != -1) continue
            region[cell] = reg
            addNeighbors(cell, reg)
        }
        return region
    }

    /** Count valid solutions of a region layout, placing one queen per row top-down. */
    private fun countSolutions(
        n: Int, region: IntArray, cols: IntArray, r: Int,
        colUsed: BooleanArray, regUsed: BooleanArray, cap: Int
    ): Int {
        if (r == n) return 1
        var count = 0
        for (c in 0 until n) {
            if (colUsed[c]) continue
            val reg = region[r * n + c]
            if (regUsed[reg]) continue
            if (r > 0 && abs(cols[r - 1] - c) <= 1) continue // adjacent to row above
            cols[r] = c; colUsed[c] = true; regUsed[reg] = true
            count += countSolutions(n, region, cols, r + 1, colUsed, regUsed, cap)
            colUsed[c] = false; regUsed[reg] = false
            if (count >= cap) return count
        }
        return count
    }

    /** Public solver used by the UI to validate / give hints. Returns row->col or null. */
    fun solve(n: Int, region: IntArray): IntArray? {
        val cols = IntArray(n) { -1 }
        return if (findOne(n, region, cols, 0, BooleanArray(n), BooleanArray(n))) cols else null
    }

    private fun findOne(
        n: Int, region: IntArray, cols: IntArray, r: Int,
        colUsed: BooleanArray, regUsed: BooleanArray
    ): Boolean {
        if (r == n) return true
        for (c in 0 until n) {
            if (colUsed[c]) continue
            val reg = region[r * n + c]
            if (regUsed[reg]) continue
            if (r > 0 && abs(cols[r - 1] - c) <= 1) continue
            cols[r] = c; colUsed[c] = true; regUsed[reg] = true
            if (findOne(n, region, cols, r + 1, colUsed, regUsed)) return true
            colUsed[c] = false; regUsed[reg] = false
        }
        return false
    }
}
