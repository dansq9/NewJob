package com.gamestest.games.games.pips

import kotlin.random.Random

enum class PipConstraint { NONE, SUM, EQUAL, DIFFERENT }

/** A group of cells with a shared constraint. [target] is used only for SUM. */
data class PipRegion(val id: Int, val cells: List<Int>, val type: PipConstraint, val target: Int)

/** A domino piece (unordered halves, [a] <= [b]). */
data class Domino(val a: Int, val b: Int)

data class PipsPuzzle(
    val rows: Int,
    val cols: Int,
    val regionOf: IntArray,
    val regions: List<PipRegion>,
    val dominoes: List<Domino>,
    val solution: IntArray,                  // pip value per cell
    val solutionTiles: List<Pair<Int, Int>>, // cell pairs each domino covers (for hints/animation)
)

/**
 * Domino-placement logic puzzle (original; not Boggle/Pips trade dress). Place
 * the given dominoes to tile the board so every region's constraint holds:
 *  - SUM: cell values add up to a target
 *  - EQUAL: all cells equal
 *  - DIFFERENT: all cells distinct
 * Generation guarantees a unique solution via a counting solver.
 */
object PipsEngine {
    const val MAX_PIP = 6

    fun generate(rng: Random, rows: Int = 4, cols: Int = 3): PipsPuzzle {
        val size = rows * cols
        require(size % 2 == 0) { "board must have an even number of cells" }

        repeat(6000) {
            val tiles = randomTiling(rng, rows, cols) ?: return@repeat
            val solution = IntArray(size)
            val dominoes = ArrayList<Domino>(tiles.size)
            for ((c1, c2) in tiles) {
                val v1 = rng.nextInt(MAX_PIP + 1)
                val v2 = rng.nextInt(MAX_PIP + 1)
                solution[c1] = v1; solution[c2] = v2
                dominoes.add(Domino(minOf(v1, v2), maxOf(v1, v2)))
            }
            val regionOf = growRegions(rng, rows, cols)
            val regions = buildRegions(rng, regionOf, solution)
            val puzzle = PipsPuzzle(rows, cols, regionOf, regions, dominoes.sortedWith(compareBy({ it.a }, { it.b })), solution, tiles)

            if (Solver(puzzle).countSolutions(cap = 2) == 1) return puzzle
        }
        error("could not generate a unique Pips puzzle")
    }

    // ---- board helpers ----
    private fun orth(cell: Int, rows: Int, cols: Int): List<Int> {
        val r = cell / cols; val c = cell % cols
        val res = ArrayList<Int>(4)
        if (r > 0) res.add(cell - cols)
        if (r < rows - 1) res.add(cell + cols)
        if (c > 0) res.add(cell - 1)
        if (c < cols - 1) res.add(cell + 1)
        return res
    }

    private fun randomTiling(rng: Random, rows: Int, cols: Int): List<Pair<Int, Int>>? {
        val size = rows * cols
        val used = BooleanArray(size)
        val res = ArrayList<Pair<Int, Int>>()
        fun rec(): Boolean {
            val c = used.indexOfFirst { !it }
            if (c == -1) return true
            used[c] = true
            for (n in orth(c, rows, cols).filter { !used[it] }.shuffled(rng)) {
                used[n] = true; res.add(c to n)
                if (rec()) return true
                res.removeAt(res.size - 1); used[n] = false
            }
            used[c] = false
            return false
        }
        return if (rec()) res else null
    }

    private fun growRegions(rng: Random, rows: Int, cols: Int): IntArray {
        val size = rows * cols
        val region = IntArray(size) { -1 }
        var id = 0
        while (true) {
            val seed = region.indexOfFirst { it == -1 }
            if (seed == -1) break
            val target = 1 + rng.nextInt(4) // region size 1..4
            region[seed] = id
            var count = 1
            val frontier = ArrayList(orth(seed, rows, cols).filter { region[it] == -1 })
            while (count < target && frontier.isNotEmpty()) {
                val pick = frontier.removeAt(rng.nextInt(frontier.size))
                if (region[pick] != -1) continue
                region[pick] = id; count++
                frontier.addAll(orth(pick, rows, cols).filter { region[it] == -1 })
            }
            id++
        }
        return region
    }

    private fun buildRegions(rng: Random, regionOf: IntArray, solution: IntArray): List<PipRegion> {
        val byId = regionOf.indices.groupBy { regionOf[it] }
        return byId.entries.sortedBy { it.key }.map { (id, cells) ->
            val vals = cells.map { solution[it] }
            val type: PipConstraint
            val target: Int
            when {
                cells.size == 1 -> { type = PipConstraint.SUM; target = vals[0] }
                vals.distinct().size == 1 && rng.nextInt(100) < 45 -> { type = PipConstraint.EQUAL; target = 0 }
                vals.distinct().size == vals.size && rng.nextInt(100) < 40 -> { type = PipConstraint.DIFFERENT; target = 0 }
                else -> { type = PipConstraint.SUM; target = vals.sum() }
            }
            PipRegion(id, cells.sorted(), type, target)
        }
    }

    /** Counting solver over tilings + value orientations honoring region constraints. */
    private class Solver(val puzzle: PipsPuzzle) {
        val size = puzzle.rows * puzzle.cols
        val value = IntArray(size) { -1 }
        val counts = IntArray((MAX_PIP + 1) * (MAX_PIP + 1))
        // region trackers
        val rFilled = IntArray(puzzle.regions.size)
        val rSum = IntArray(puzzle.regions.size)
        val rPresent = Array(puzzle.regions.size) { IntArray(MAX_PIP + 1) }
        var steps = 0L

        init {
            for (d in puzzle.dominoes) counts[pid(d.a, d.b)]++
        }

        private fun pid(a: Int, b: Int): Int {
            val lo = minOf(a, b); val hi = maxOf(a, b); return lo * (MAX_PIP + 1) + hi
        }

        private fun assign(cell: Int, v: Int): Boolean {
            val r = puzzle.regionOf[cell]
            val reg = puzzle.regions[r]
            when (reg.type) {
                PipConstraint.EQUAL -> for (k in 0..MAX_PIP) if (k != v && rPresent[r][k] > 0) return false
                PipConstraint.DIFFERENT -> if (rPresent[r][v] > 0) return false
                PipConstraint.SUM -> {
                    val newSum = rSum[r] + v
                    val remaining = reg.cells.size - (rFilled[r] + 1)
                    if (newSum > reg.target) return false
                    if (reg.target > newSum + remaining * MAX_PIP) return false
                }
                PipConstraint.NONE -> {}
            }
            rPresent[r][v]++; rSum[r] += v; rFilled[r]++
            return true
        }

        private fun unassign(cell: Int, v: Int) {
            val r = puzzle.regionOf[cell]
            rPresent[r][v]--; rSum[r] -= v; rFilled[r]--
        }

        fun countSolutions(cap: Int): Int {
            if (steps++ > 5_000_000L) return cap // bail -> treat as non-unique
            val start = value.indexOfFirst { it == -1 }
            if (start == -1) return 1
            var total = 0
            val neighbors = PipsEngine.orth(start, puzzle.rows, puzzle.cols).filter { value[it] == -1 }
            for (n in neighbors) {
                for (lo in 0..MAX_PIP) for (hi in lo..MAX_PIP) {
                    val id = lo * (MAX_PIP + 1) + hi
                    if (counts[id] == 0) continue
                    val orients = if (lo == hi) listOf(lo to hi) else listOf(lo to hi, hi to lo)
                    for ((vs, vn) in orients) {
                        if (!assign(start, vs)) continue
                        if (assign(n, vn)) {
                            counts[id]--; value[start] = vs; value[n] = vn
                            total += countSolutions(cap)
                            value[start] = -1; value[n] = -1; counts[id]++
                            unassign(n, vn)
                        }
                        unassign(start, vs)
                        if (total >= cap) { return total }
                    }
                }
            }
            return total
        }
    }
}
