package com.gamestest.games.games.minicross

import com.gamestest.games.core.GameLanguage
import com.gamestest.games.core.TextNormalize

enum class CrossDir { ACROSS, DOWN }

data class CrossClue(
    val number: Int,
    val dir: CrossDir,
    val row: Int,
    val col: Int,
    val length: Int,
    val answer: String,
    val clue: String,
)

data class MiniCrossPuzzle(
    val size: Int,
    val solution: CharArray,        // row-major; ' ' = block
    val blocks: Set<Int>,
    val clues: List<CrossClue>,
) {
    fun isBlock(cell: Int) = cell in blocks
    fun cell(row: Int, col: Int) = row * size + col
}

/** Raw authored puzzle: grid rows ('.' = block) + clue texts in numbering order. */
data class MiniCrossSpec(
    val rows: List<String>,
    val acrossClues: List<String>,
    val downClues: List<String>,
)

/**
 * Small crossword engine (original grids/clues; no NYT content or "Mini" branding).
 * Numbering and across/down entries are derived from the grid so authoring a
 * puzzle is just a grid + ordered clue lists.
 */
object MiniCrossEngine {

    fun daily(epochDay: Long, language: GameLanguage = GameLanguage.ENGLISH): MiniCrossPuzzle {
        val pool = MiniCrossData.pool(language)
        val spec = pool[((epochDay % pool.size) + pool.size).toInt() % pool.size]
        return build(spec)
    }

    fun build(spec: MiniCrossSpec): MiniCrossPuzzle {
        val size = spec.rows.size
        require(spec.rows.all { it.length == size }) { "grid must be square" }

        val solution = CharArray(size * size) { ' ' }
        val blocks = HashSet<Int>()
        for (r in 0 until size) for (c in 0 until size) {
            val ch = spec.rows[r][c]
            if (ch == '.') blocks.add(r * size + c) else solution[r * size + c] = ch.uppercaseChar()
        }
        fun isBlock(r: Int, c: Int) = r !in 0 until size || c !in 0 until size || (r * size + c) in blocks

        val across = ArrayList<CrossClue>()
        val down = ArrayList<CrossClue>()
        var number = 0
        var ai = 0
        var di = 0
        for (r in 0 until size) for (c in 0 until size) {
            if (isBlock(r, c)) continue
            val startsAcross = isBlock(r, c - 1) && !isBlock(r, c + 1)
            val startsDown = isBlock(r - 1, c) && !isBlock(r + 1, c)
            if (!startsAcross && !startsDown) continue
            number++
            if (startsAcross) {
                val sb = StringBuilder()
                var cc = c
                while (!isBlock(r, cc)) { sb.append(solution[r * size + cc]); cc++ }
                across.add(CrossClue(number, CrossDir.ACROSS, r, c, sb.length, sb.toString(),
                    spec.acrossClues.getOrElse(ai) { "" }))
                ai++
            }
            if (startsDown) {
                val sb = StringBuilder()
                var rr = r
                while (!isBlock(rr, c)) { sb.append(solution[rr * size + c]); rr++ }
                down.add(CrossClue(number, CrossDir.DOWN, r, c, sb.length, sb.toString(),
                    spec.downClues.getOrElse(di) { "" }))
                di++
            }
        }
        return MiniCrossPuzzle(size, solution, blocks, across + down)
    }

    /** True if every non-block cell matches the solution (accent-insensitive). */
    fun isComplete(entries: CharArray, puzzle: MiniCrossPuzzle): Boolean {
        for (i in puzzle.solution.indices) {
            if (i in puzzle.blocks) continue
            val e = entries.getOrNull(i) ?: return false
            if (e == ' ') return false
            if (TextNormalize.normalizeChar(e) != TextNormalize.normalizeChar(puzzle.solution[i])) return false
        }
        return true
    }
}
