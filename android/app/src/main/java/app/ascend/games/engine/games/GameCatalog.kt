package app.ascend.games.engine.games

/**
 * The nine Brain Games shown in the hub (matches the design mock).
 *
 * [id] is the stable storage/seed key. Higher-trademark-risk word games
 * (Wordle/Spelling Bee/Strands-likes) are intentionally not surfaced here;
 * their engines remain in the repo but unused.
 */
enum class GameId(
    val id: String,
    val title: String,
    val blurb: String,
    val rules: List<String>,
) {
    PATCHES("patches", "Patches", "Reveal the hidden picture", listOf(
        "Tap a cell to fill it, tap again for an X, once more to clear.",
        "The numbers show the runs of filled cells in each row and column.",
        "Fill the right cells to reveal today's picture.",
    )),
    SUDOKU("sudoku", "Mini Sudoku", "Fill the 6 by 6 grid", listOf(
        "Every row, column and 2 by 3 box must hold the numbers 1 to 6.",
        "Tap a cell, then tap a number to place it.",
        "Bold numbers are fixed clues.",
    )),
    ZIP("zip", "Trail", "Trace 1 to 7 in one path", listOf(
        "Draw one path that fills every cell.",
        "Pass through the numbers in order, 1 then 2 then 3.",
        "Drag across cells, or drag back to undo.",
    )),
    QUEENS("queens", "Stars", "One per row, column, colour", listOf(
        "Place one star in every row, every column and every colour region.",
        "Stars can never touch, not even diagonally.",
        "Tap once for an X, again to place a star.",
    )),
    TANGO("tango", "Eclipse", "Balance suns and moons", listOf(
        "Fill the grid with suns and moons.",
        "Each row and column needs three of each, with no more than two alike in a row.",
        "The = sign joins equal cells, the x sign joins opposites.",
    )),
    G2048("g2048", "2048", "Merge tiles up to 2048", listOf(
        "Swipe up, down, left or right to slide all tiles.",
        "Two tiles with the same number merge into one.",
        "Reach the 2048 tile to win the day.",
    )),
    CLUSTERS("clusters", "Clusters", "Group the sixteen words", listOf(
        "Find four hidden groups of four related words.",
        "Tap four words, then press Submit.",
        "Solve all four groups to finish.",
    )),
    CROSSWORD("crossword", "Mini Cross", "A five by five crossword", listOf(
        "Tap a square, then type using the keyboard.",
        "Tap a square twice to switch across or down.",
        "Fill every square correctly to finish.",
    )),
    LIGHTSOUT("lightsout", "Lights Out", "Switch off every light", listOf(
        "Tap a tile to flip it and its neighbours.",
        "Turn every light off to win.",
        "Plan ahead, each tap affects the others.",
    ));

    companion object {
        val all: List<GameId> get() = entries
        fun byId(id: String): GameId? = entries.firstOrNull { it.id == id }
    }
}
