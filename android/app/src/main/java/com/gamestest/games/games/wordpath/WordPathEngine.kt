package com.gamestest.games.games.wordpath

import com.gamestest.games.core.Daily
import com.gamestest.games.core.GameLanguage
import com.gamestest.games.core.TextNormalize
import kotlin.random.Random

/** Theme spec: words (uppercase, base letters) whose lengths sum to rows*cols. */
data class WordPathSpec(
    val theme: String,
    val rows: Int,
    val cols: Int,
    val spanner: String,
    val words: List<String>, // includes [spanner]
)

data class WordPathPuzzle(
    val theme: String,
    val rows: Int,
    val cols: Int,
    val grid: CharArray,                 // row-major letters
    val spanner: String,
    val words: List<String>,
    val placements: Map<String, List<Int>>,
) {
    fun letterAt(cell: Int): Char = grid[cell]
}

/**
 * Themed word-path puzzle (Strands-like, original data + name). Every cell
 * belongs to exactly one theme word — no filler letters. The generator packs
 * the words onto the grid as self-avoiding 8-directional paths.
 *
 * [match] is what the UI calls as the player drags a path: it returns the theme
 * word if the traced cells exactly cover one placement and spell that word.
 */
object WordPathEngine {

    fun daily(epochDay: Long, language: GameLanguage = GameLanguage.ENGLISH): WordPathPuzzle {
        val pool = WordPathData.pool(language)
        val spec = pool[((epochDay % pool.size) + pool.size).toInt() % pool.size]
        for (salt in 0 until 200) {
            generate(spec, Daily.random(epochDay, "wordpath-" + language.code, salt))?.let { return it }
        }
        error("could not pack puzzle for theme ${spec.theme}")
    }

    fun generate(spec: WordPathSpec, rng: Random): WordPathPuzzle? {
        val total = spec.rows * spec.cols
        require(spec.words.sumOf { it.length } == total) { "word lengths must fill the grid" }

        val grid = CharArray(total) { ' ' }
        val placements = LinkedHashMap<String, List<Int>>()
        val order = spec.words.sortedByDescending { it.length }.toMutableList().also { it.shuffle(rng) }
        var steps = 0L

        fun neighbors(cell: Int): List<Int> {
            val r = cell / spec.cols; val c = cell % spec.cols
            val res = ArrayList<Int>(8)
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until spec.rows && nc in 0 until spec.cols) res.add(nr * spec.cols + nc)
            }
            return res
        }

        fun solve(remaining: List<String>): Boolean {
            if (remaining.isEmpty()) return grid.none { it == ' ' }
            if (steps++ > 3_000_000L) return false
            val start = grid.indexOfFirst { it == ' ' }

            for (wi in remaining.indices) {
                val w = remaining[wi]
                val next = remaining.toMutableList().also { it.removeAt(wi) }
                val cells = IntArray(w.length)

                fun extend(cell: Int, depth: Int): Boolean {
                    cells[depth] = cell
                    grid[cell] = w[depth]
                    if (depth == w.length - 1) {
                        placements[w] = cells.toList()
                        if (solve(next)) return true
                        placements.remove(w)
                    } else {
                        for (nb in neighbors(cell)) if (grid[nb] == ' ') {
                            if (extend(nb, depth + 1)) return true
                        }
                    }
                    grid[cell] = ' '
                    return false
                }

                if (extend(start, 0)) return true
            }
            return false
        }

        return if (solve(order)) {
            WordPathPuzzle(spec.theme, spec.rows, spec.cols, grid, spec.spanner, spec.words, LinkedHashMap(placements))
        } else null
    }

    fun pathWord(path: List<Int>, puzzle: WordPathPuzzle): String =
        path.joinToString("") { puzzle.grid[it].toString() }

    /** Theme word if [path] exactly covers a placement and spells it (either direction). */
    fun match(path: List<Int>, puzzle: WordPathPuzzle): String? {
        val set = path.toSet()
        if (set.size != path.size) return null
        val spelled = TextNormalize.normalize(pathWord(path, puzzle))
        for ((word, cells) in puzzle.placements) {
            if (cells.toSet() != set) continue
            val target = TextNormalize.normalize(word)
            if (spelled == target || spelled == target.reversed()) return word
        }
        return null
    }

    fun isSpanner(word: String, puzzle: WordPathPuzzle) = word == puzzle.spanner
}
