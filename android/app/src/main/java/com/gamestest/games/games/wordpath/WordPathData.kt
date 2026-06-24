package com.gamestest.games.games.wordpath

import com.gamestest.games.core.GameLanguage

/**
 * Original themed word sets. Each spec's word lengths must sum to rows*cols so
 * the grid is fully covered (no filler letters). Grid letters are uppercase,
 * base (accent-free) forms.
 */
object WordPathData {

    fun pool(language: GameLanguage): List<WordPathSpec> = when (language) {
        GameLanguage.PORTUGUESE -> PT
        GameLanguage.ENGLISH -> EN
    }

    private val EN = listOf(
        WordPathSpec("Fruits", 5, 5, "PINEAPPLE",
            listOf("PINEAPPLE", "CHERRY", "MANGO", "LEMON")),
        WordPathSpec("Animals", 5, 5, "ELEPHANT",
            listOf("ELEPHANT", "GIRAFFE", "TIGER", "HORSE")),
        WordPathSpec("Colors", 4, 6, "TURQUOISE",
            listOf("TURQUOISE", "LAVENDER", "MAGENTA")),
    )

    private val PT = listOf(
        WordPathSpec("Frutas", 4, 6, "ABACAXI",
            listOf("ABACAXI", "MORANGO", "MANGA", "LIMAO")),
        WordPathSpec("Animais", 4, 6, "ELEFANTE",
            listOf("ELEFANTE", "GIRAFA", "TIGRE", "GANSO")),
    )
}
