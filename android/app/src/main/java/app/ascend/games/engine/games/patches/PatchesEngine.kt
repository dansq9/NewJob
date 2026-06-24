package app.ascend.games.engine.games.patches

/**
 * Patches — colored nonogram / picture-reveal.
 *
 * NOTE: This is our interpretation. LinkedIn's "Patches" exact ruleset isn't
 * fully pinned down here, so we implement the closest well-defined daily logic
 * puzzle: a multi-color nonogram. Each row/column lists the runs of colored cells
 * (length + color) and the player paints the grid to reveal the picture. The art
 * and palette below are easy to swap for whatever the final spec turns out to be.
 *
 * [solution] is length rows*cols, each value a palette index (0 = empty/background).
 * Clues are ordered from left-to-right (rows) and top-to-bottom (columns).
 */
data class Clue(val length: Int, val colorIndex: Int)

data class PatchesPuzzle(
    val title: String,
    val rows: Int,
    val cols: Int,
    val palette: List<Long>,          // ARGB colors; index 0 = background
    val solution: IntArray,
    val rowClues: List<List<Clue>>,
    val colClues: List<List<Clue>>
)

private data class Picture(
    val title: String,
    val palette: List<Long>,          // colors for legend chars, in order
    val legend: String,               // chars matching palette colors, e.g. "BY"
    val art: List<String>             // '.' = background, other chars index into legend
)

object PatchesEngine {

    fun forDay(epochDay: Long): PatchesPuzzle {
        val idx = ((epochDay % PICTURES.size) + PICTURES.size).toInt() % PICTURES.size
        return build(PICTURES[idx])
    }

    fun byIndex(i: Int): PatchesPuzzle = build(PICTURES[i.mod(PICTURES.size)])

    val count: Int get() = PICTURES.size

    private fun build(pic: Picture): PatchesPuzzle {
        val rows = pic.art.size
        val cols = pic.art[0].length
        require(pic.art.all { it.length == cols }) { "ragged art in ${pic.title}" }

        // Palette index 0 is background; legend chars map to 1..n.
        val palette = ArrayList<Long>().apply {
            add(0x00000000L)            // transparent background
            addAll(pic.palette)
        }
        val solution = IntArray(rows * cols)
        for (r in 0 until rows) for (c in 0 until cols) {
            val ch = pic.art[r][c]
            solution[r * cols + c] = if (ch == '.') 0 else pic.legend.indexOf(ch) + 1
        }

        val rowClues = (0 until rows).map { r -> runs((0 until cols).map { solution[r * cols + it] }) }
        val colClues = (0 until cols).map { c -> runs((0 until rows).map { solution[it * cols + c] }) }
        return PatchesPuzzle(pic.title, rows, cols, palette, solution, rowClues, colClues)
    }

    /** Collapse a line of color indices into ordered runs of same non-zero color. */
    private fun runs(line: List<Int>): List<Clue> {
        val out = ArrayList<Clue>()
        var i = 0
        while (i < line.size) {
            val v = line[i]
            if (v == 0) { i++; continue }
            var j = i
            while (j < line.size && line[j] == v) j++
            out.add(Clue(j - i, v))
            i = j
        }
        return out
    }

    // Job-themed 9x9 pixel art. '.' = empty.
    private val PICTURES = listOf(
        Picture(
            title = "Briefcase",
            palette = listOf(0xFF8D5524L, 0xFFFFC107L),
            legend = "BY",
            art = listOf(
                "...YYY...",
                "...Y.Y...",
                ".BBBBBBB.",
                ".BBBBBBB.",
                ".BBBYBBB.",
                ".BBBYBBB.",
                ".BBBBBBB.",
                ".BBBBBBB.",
                ".........",
            )
        ),
        Picture(
            title = "Star",
            palette = listOf(0xFFFFC107L),
            legend = "Y",
            art = listOf(
                "....Y....",
                "....Y....",
                "...YYY...",
                "YYYYYYYYY",
                ".YYYYYYY.",
                "..YYYYY..",
                "..YY.YY..",
                ".YY...YY.",
                ".........",
            )
        ),
        Picture(
            title = "Coffee",
            palette = listOf(0xFF6F4E37L, 0xFFB0BEC5L),
            legend = "CG",
            art = listOf(
                ".........",
                "..C.C.C..",
                "..C.C.C..",
                ".GGGGGG..",
                ".GCCCCG.G",
                ".GCCCCG.G",
                ".GCCCCG.G",
                ".GGGGGG..",
                ".........",
            )
        ),
        Picture(
            title = "Light Bulb",
            palette = listOf(0xFFFFC107L, 0xFF90A4AEL),
            legend = "YG",
            art = listOf(
                "...YYY...",
                "..YYYYY..",
                ".YYYYYYY.",
                ".YYYYYYY.",
                ".YYYYYYY.",
                "..YYYYY..",
                "...GGG...",
                "...GGG...",
                "....G....",
            )
        ),
        Picture(
            title = "Rocket",
            palette = listOf(0xFFE53935L, 0xFFB0BEC5L, 0xFFFFC107L),
            legend = "RGY",
            art = listOf(
                "....G....",
                "...GGG...",
                "..GGGGG..",
                "..GRRRG..",
                "..GRRRG..",
                "..GGGGG..",
                ".G.GGG.G.",
                "...YYY...",
                "...Y.Y...",
            )
        ),
    )
}
