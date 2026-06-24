package com.gamestest.games.games.zip

import kotlin.random.Random

/**
 * Zip. Draw a single path that fills every cell exactly once and passes through
 * the numbered checkpoints in ascending order (1, 2, 3, ...).
 *
 * [numbers] is length rows*cols, 0 = no checkpoint.
 * [walls] holds blocked edges between adjacent cells (encoded via [wallKey]).
 * [solution] lists cell indices in visiting order (length rows*cols).
 */
data class ZipPuzzle(
    val rows: Int,
    val cols: Int,
    val numbers: IntArray,
    val walls: Set<Long>,
    val solution: IntArray
) {
    val checkpointCount: Int get() = numbers.count { it > 0 }

    companion object {
        fun wallKey(a: Int, b: Int): Long {
            val lo = minOf(a, b); val hi = maxOf(a, b)
            return lo.toLong() * 1_000_000L + hi
        }
    }

    fun hasWall(a: Int, b: Int) = walls.contains(wallKey(a, b))
}

object ZipEngine {

    fun generate(rng: Random, rows: Int = 6, cols: Int = 6, checkpoints: Int = 8): ZipPuzzle {
        val total = rows * cols
        val path = hamiltonian(rng, rows, cols) ?: error("no hamiltonian path")

        val numbers = IntArray(total)
        val k = checkpoints.coerceIn(2, total)
        // Evenly spaced checkpoints along the path, including both endpoints.
        for (i in 0 until k) {
            val pos = (i * (total - 1)) / (k - 1)
            numbers[path[pos]] = i + 1
        }

        // Edges used by the intended solution must never be walled.
        val solutionEdges = HashSet<Long>()
        for (i in 0 until total - 1) solutionEdges.add(ZipPuzzle.wallKey(path[i], path[i + 1]))

        // Insert walls to kill alternate solutions until the puzzle is unique.
        val walls = HashSet<Long>()
        repeat(total * 4) {
            val alt = findAlternate(rows, cols, numbers, walls, path) ?: return@repeat
            var cut = false
            for (i in 0 until total - 1) {
                val key = ZipPuzzle.wallKey(alt[i], alt[i + 1])
                if (key !in solutionEdges && key !in walls) { walls.add(key); cut = true; break }
            }
            if (!cut) return@repeat // alternate shares all edges with solution (shouldn't happen)
        }
        return ZipPuzzle(rows, cols, numbers, walls, path)
    }

    /** Find a full valid path different from [intended], or null if none. */
    private fun findAlternate(
        rows: Int, cols: Int, numbers: IntArray, walls: Set<Long>, intended: IntArray
    ): IntArray? {
        val total = rows * cols
        val visited = BooleanArray(total)
        val path = IntArray(total)
        val start = numbers.indexOfFirst { it == 1 }
        val checkpointCount = numbers.count { it > 0 }
        var steps = 0L
        var result: IntArray? = null

        fun neighbors(cell: Int): List<Int> {
            val r = cell / cols; val c = cell % cols
            val res = ArrayList<Int>(4)
            if (r > 0) res.add(cell - cols)
            if (r < rows - 1) res.add(cell + cols)
            if (c > 0) res.add(cell - 1)
            if (c < cols - 1) res.add(cell + 1)
            return res.filter { ZipPuzzle.wallKey(cell, it) !in walls }
        }

        fun dfs(cell: Int, depth: Int, nextCp: Int): Boolean {
            steps++
            if (steps > 3_000_000L) return false
            visited[cell] = true
            path[depth] = cell
            val cp = numbers[cell]
            var expecting = nextCp
            if (cp != 0) {
                if (cp != nextCp) { visited[cell] = false; return false }
                expecting = nextCp + 1
            }
            if (depth == total - 1) {
                if (cp == checkpointCount && !path.contentEquals(intended)) { result = path.copyOf(); return true }
                visited[cell] = false
                return false
            }
            for (nb in neighbors(cell)) {
                if (!visited[nb] && dfs(nb, depth + 1, expecting)) return true
            }
            visited[cell] = false
            return false
        }

        if (start >= 0) dfs(start, 0, 1)
        return result
    }

    /** Randomized Hamiltonian path via DFS with a Warnsdorff-style ordering. */
    private fun hamiltonian(rng: Random, rows: Int, cols: Int): IntArray? {
        val total = rows * cols
        val visited = BooleanArray(total)
        val path = IntArray(total)
        var steps = 0L

        fun neighbors(cell: Int): List<Int> {
            val r = cell / cols; val c = cell % cols
            val res = ArrayList<Int>(4)
            if (r > 0) res.add(cell - cols)
            if (r < rows - 1) res.add(cell + cols)
            if (c > 0) res.add(cell - 1)
            if (c < cols - 1) res.add(cell + 1)
            return res
        }

        fun dfs(cell: Int, depth: Int): Boolean {
            steps++
            if (steps > 5_000_000L) return false
            visited[cell] = true
            path[depth] = cell
            if (depth == total - 1) return true
            val nbrs = neighbors(cell).filter { !visited[it] }.toMutableList()
            nbrs.shuffle(rng)
            // Prefer the most-constrained next cell (fewest onward options).
            nbrs.sortBy { n -> neighbors(n).count { !visited[it] } }
            for (nb in nbrs) {
                if (!visited[nb] && dfs(nb, depth + 1)) return true
            }
            visited[cell] = false
            return false
        }

        for (attempt in 0 until total) {
            visited.fill(false)
            steps = 0
            val start = (attempt + rng.nextInt(total)) % total
            if (dfs(start, 0)) return path
        }
        return null
    }

    /**
     * Counts valid solutions (paths) up to [cap]. Used in tests to confirm a
     * generated puzzle is actually solvable (and to gauge uniqueness).
     */
    fun countSolutions(puzzle: ZipPuzzle, cap: Int = 2): Int {
        val total = puzzle.rows * puzzle.cols
        val visited = BooleanArray(total)
        val start = puzzle.numbers.indexOfFirst { it == 1 }
        var found = 0

        fun neighbors(cell: Int): List<Int> {
            val r = cell / puzzle.cols; val c = cell % puzzle.cols
            val res = ArrayList<Int>(4)
            if (r > 0) res.add(cell - puzzle.cols)
            if (r < puzzle.rows - 1) res.add(cell + puzzle.cols)
            if (c > 0) res.add(cell - 1)
            if (c < puzzle.cols - 1) res.add(cell + 1)
            return res.filter { !puzzle.hasWall(cell, it) }
        }

        fun dfs(cell: Int, depth: Int, nextCp: Int) {
            if (found >= cap) return
            visited[cell] = true
            val cp = puzzle.numbers[cell]
            var expecting = nextCp
            if (cp != 0) {
                if (cp != nextCp) { visited[cell] = false; return } // out-of-order checkpoint
                expecting = nextCp + 1
            }
            if (depth == total - 1) {
                if (cp == puzzle.checkpointCount) found++
                visited[cell] = false
                return
            }
            for (nb in neighbors(cell)) {
                if (!visited[nb]) dfs(nb, depth + 1, expecting)
                if (found >= cap) break
            }
            visited[cell] = false
        }

        if (start >= 0) dfs(start, 0, 1)
        return found
    }
}
