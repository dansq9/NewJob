package com.gamestest.games.games.letters

import com.gamestest.games.core.Daily
import com.gamestest.games.core.GameLanguage
import com.gamestest.games.core.TextNormalize

/**
 * A 7-letter "find words" puzzle (original; no NYT/honeycomb/bee branding).
 *
 * One required [center] letter plus six [outer] letters. Valid words are >= 4
 * letters, use only the seven letters (repeats allowed), and must contain the
 * center letter. Words using all seven letters are pangrams.
 *
 * [letters] are normalized (accent-free) base letters; [answers]/[pangrams]
 * keep their original spelling for display.
 */
data class LettersPuzzle(
    val center: Char,
    val outer: List<Char>,
    val answers: List<String>,
    val pangrams: List<String>,
    val maxScore: Int,
) {
    val letters: Set<Char> get() = outer.toSet() + center
}

object LettersEngine {
    const val MIN_LENGTH = 4
    const val LETTER_COUNT = 7

    fun daily(epochDay: Long, dictionary: Set<String>, language: GameLanguage = GameLanguage.ENGLISH): LettersPuzzle {
        // Base words have exactly 7 distinct letters; their letter set defines a puzzle.
        val bases = dictionary.filter { TextNormalize.distinctLetters(it).size == LETTER_COUNT }
            .sorted() // deterministic ordering before seeded pick
        require(bases.isNotEmpty()) { "dictionary has no 7-distinct-letter words" }

        // Try seeds until we get a puzzle with at least one pangram (always true for a valid base).
        var salt = 0
        while (true) {
            val rng = Daily.random(epochDay, "letters-" + language.code, salt)
            val base = bases[rng.nextInt(bases.size)]
            val set = TextNormalize.distinctLetters(base).toList().sorted()
            val center = set[rng.nextInt(set.size)]
            val outer = set.filter { it != center }

            val answers = dictionary.filter { word ->
                val w = TextNormalize.normalize(word)
                w.length >= MIN_LENGTH &&
                    center in w &&
                    TextNormalize.distinctLetters(word).all { it in set }
            }.sortedWith(compareByDescending<String> { it.length }.thenBy { it })

            if (answers.isNotEmpty()) {
                val pangrams = answers.filter { isPangram(it, set.toSet()) }
                if (pangrams.isNotEmpty()) {
                    val maxScore = answers.sumOf { score(it, set.toSet()) }
                    return LettersPuzzle(center, outer, answers, pangrams, maxScore)
                }
            }
            salt++
            if (salt > 5000) error("could not build a Letters puzzle")
        }
    }

    fun isPangram(word: String, letters: Set<Char>): Boolean =
        TextNormalize.distinctLetters(word) == letters

    /** Scoring: 4-letter word = 1, longer = its length, pangram = +7 bonus. */
    fun score(word: String, letters: Set<Char>): Int {
        val len = TextNormalize.normalize(word).length
        val base = if (len == MIN_LENGTH) 1 else len
        return base + if (isPangram(word, letters)) LETTER_COUNT else 0
    }

    fun isValidWord(word: String, puzzle: LettersPuzzle, dictionary: Set<String>): Boolean {
        val w = TextNormalize.normalize(word)
        if (w.length < MIN_LENGTH) return false
        if (puzzle.center !in w) return false
        if (!TextNormalize.distinctLetters(word).all { it in puzzle.letters }) return false
        return dictionary.any { TextNormalize.normalize(it) == w }
    }
}
