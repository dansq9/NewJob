package app.ascend.games.engine.games.sudoku

import kotlin.random.Random

/**
 * 6x6 Mini Sudoku. Digits 1..6, boxes are 2 rows x 3 cols.
 *
 * [givens] and [solution] are length 36, row-major. 0 means empty in [givens].
 */
data class SudokuPuzzle(
    val givens: IntArray,
    val solution: IntArray
) {
    companion object {
        const val N = 6
        const val BOX_H = 2 // box height (rows)
        const val BOX_W = 3 // box width (cols)
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is SudokuPuzzle &&
            givens.contentEquals(other.givens) && solution.contentEquals(other.solution))

    override fun hashCode(): Int = givens.contentHashCode() * 31 + solution.contentHashCode()
}

object SudokuEngine {
    private const val N = SudokuPuzzle.N

    /**
     * Generate a Mini Sudoku with a unique solution.
     * @param targetGivens lower bound on clues to leave; fewer = harder.
     */
    fun generate(rng: Random, targetGivens: Int = 12): SudokuPuzzle {
        val solution = IntArray(N * N)
        require(fill(solution, 0, rng)) { "failed to fill sudoku grid" }

        val givens = solution.copyOf()
        val cells = (0 until N * N).toMutableList().also { it.shuffle(rng) }
        var remaining = N * N
        for (c in cells) {
            if (remaining <= targetGivens) break
            val backup = givens[c]
            givens[c] = 0
            // Keep the hole only if the puzzle is still uniquely solvable.
            if (countSolutions(givens.copyOf(), 2) != 1) {
                givens[c] = backup
            } else {
                remaining--
            }
        }
        return SudokuPuzzle(givens, solution)
    }

    private fun fill(g: IntArray, idx: Int, rng: Random): Boolean {
        if (idx == N * N) return true
        if (g[idx] != 0) return fill(g, idx + 1, rng)
        val candidates = (1..N).toMutableList().also { it.shuffle(rng) }
        for (v in candidates) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                if (fill(g, idx + 1, rng)) return true
                g[idx] = 0
            }
        }
        return false
    }

    /** True if [v] may be placed at [idx] without breaking row/col/box rules. */
    fun canPlace(g: IntArray, idx: Int, v: Int): Boolean {
        val r = idx / N
        val c = idx % N
        for (i in 0 until N) {
            if (g[r * N + i] == v) return false
            if (g[i * N + c] == v) return false
        }
        val br = (r / SudokuPuzzle.BOX_H) * SudokuPuzzle.BOX_H
        val bc = (c / SudokuPuzzle.BOX_W) * SudokuPuzzle.BOX_W
        for (dr in 0 until SudokuPuzzle.BOX_H) for (dc in 0 until SudokuPuzzle.BOX_W) {
            if (g[(br + dr) * N + (bc + dc)] == v) return false
        }
        return true
    }

    /** Counts solutions of [g] (0 = empty) up to [cap]; used for uniqueness checks. */
    private fun countSolutions(g: IntArray, cap: Int): Int {
        val idx = g.indexOf(0)
        if (idx == -1) return 1
        var count = 0
        for (v in 1..N) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                count += countSolutions(g, cap)
                g[idx] = 0
                if (count >= cap) return count
            }
        }
        return count
    }
}
