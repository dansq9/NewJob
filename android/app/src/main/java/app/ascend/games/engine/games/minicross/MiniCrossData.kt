package app.ascend.games.engine.games.minicross

import app.ascend.games.engine.core.GameLanguage

/**
 * Original mini crosswords. PT currently falls back to EN until Portuguese grids
 * are authored (the engine fully supports PT — only the data is pending).
 */
object MiniCrossData {

    fun pool(language: GameLanguage): List<MiniCrossSpec> = when (language) {
        GameLanguage.PORTUGUESE -> if (PT.isNotEmpty()) PT else EN
        GameLanguage.ENGLISH -> EN
    }

    private val EN = listOf(
        // 5x5 open word square (rows and columns are the same five words).
        MiniCrossSpec(
            rows = listOf("HEART", "EMBER", "ABUSE", "RESIN", "TREND"),
            acrossClues = listOf(
                "Organ that pumps blood",
                "Glowing remnant of a fire",
                "To mistreat",
                "Sticky secretion from pine trees",
                "General direction things are moving",
            ),
            downClues = listOf(
                "Symbol of love",
                "Smoldering coal",
                "Improper use",
                "Base for some varnishes",
                "The latest fashion",
            ),
        ),
        // TODO: add more valid grids (every across & down run must be a real word).
    )

    private val PT = emptyList<MiniCrossSpec>()
}
