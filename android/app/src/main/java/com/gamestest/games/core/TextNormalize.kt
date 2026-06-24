package com.gamestest.games.core

import java.text.Normalizer

/**
 * Diacritic-insensitive text helpers, mainly for Portuguese content
 * (ç, ã, õ, á, é, í, ó, ú, â, ê, ô …).
 *
 * Matching in the word games is done on a normalized, accent-stripped, lowercase
 * form, while the original (accented) word is kept for display. [normalizeChar]
 * maps a single character to its base letter so normalization preserves length
 * and per-index comparisons stay aligned (e.g. "ção" -> "cao", same length).
 */
object TextNormalize {

    fun normalizeChar(c: Char): Char {
        val nfd = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
        // first code point of the decomposition is the base letter
        return nfd[0].lowercaseChar()
    }

    fun normalize(word: String): String = buildString(word.length) {
        for (c in word) append(normalizeChar(c))
    }

    /** Distinct base letters in a word (used by Letters to find 7-letter sets). */
    fun distinctLetters(word: String): Set<Char> = word.mapTo(HashSet()) { normalizeChar(it) }
}
