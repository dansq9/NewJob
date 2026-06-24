package com.gamestest.games.core

import java.util.Locale

/**
 * Content language for the word games. The app already localizes its UI; this
 * only selects which puzzle/word data to load. English is the fallback and also
 * serves South Africa + the rest; Portuguese serves Brazil.
 *
 * [assetDir] is the folder under `assets/words/` and `assets/puzzles/`.
 * [keyboard] is the playable alphabet (no accents — matching is accent-insensitive).
 */
enum class GameLanguage(val code: String, val assetDir: String, val keyboard: String) {
    ENGLISH("en", "en", "abcdefghijklmnopqrstuvwxyz"),
    PORTUGUESE("pt", "pt", "abcdefghijklmnopqrstuvwxyz");

    companion object {
        fun from(locale: Locale): GameLanguage =
            if (locale.language.equals("pt", ignoreCase = true)) PORTUGUESE else ENGLISH

        fun current(): GameLanguage = from(Locale.getDefault())
    }
}
