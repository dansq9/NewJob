package com.gamestest.games.games.guess

import com.gamestest.games.core.TextNormalize
import com.gamestest.games.core.WordSource

/** Per-letter feedback. Ordered by precedence for keyboard aggregation. */
enum class LetterState { ABSENT, PRESENT, CORRECT }

data class GuessEvaluation(val guess: String, val states: List<LetterState>) {
    val solved: Boolean get() = states.all { it == LetterState.CORRECT }
}

/**
 * Word-guessing engine (original; no NYT word lists or share format).
 *
 * Matching is accent-insensitive so Portuguese answers (e.g. "dança") accept
 * unaccented guesses ("danca") while the UI can still reveal the accented
 * answer. Evaluation exposes per-letter [LetterState]s so the screen can run
 * staggered tile-flip animations.
 */
object GuessEngine {
    const val LENGTH = 5
    const val MAX_GUESSES = 6

    fun dailyAnswer(epochDay: Long, answers: List<String>): String {
        require(answers.isNotEmpty()) { "no answer words" }
        val idx = ((epochDay % answers.size) + answers.size).toInt() % answers.size
        return answers[idx]
    }

    fun isAcceptable(guess: String, source: WordSource): Boolean =
        guess.length == LENGTH && source.isValidGuess(guess)

    /** Two-pass evaluation with correct duplicate-letter handling. */
    fun evaluate(guess: String, answer: String): GuessEvaluation {
        val g = TextNormalize.normalize(guess)
        val a = TextNormalize.normalize(answer)
        require(g.length == a.length) { "length mismatch" }

        val states = MutableList(g.length) { LetterState.ABSENT }
        val remaining = HashMap<Char, Int>()
        for (i in a.indices) {
            if (g[i] == a[i]) states[i] = LetterState.CORRECT
            else remaining[a[i]] = (remaining[a[i]] ?: 0) + 1
        }
        for (i in g.indices) {
            if (states[i] == LetterState.CORRECT) continue
            val c = g[i]
            val left = remaining[c] ?: 0
            if (left > 0) { states[i] = LetterState.PRESENT; remaining[c] = left - 1 }
        }
        return GuessEvaluation(guess, states)
    }

    /** Best-known state per typed letter, for coloring the on-screen keyboard. */
    fun keyboardStates(evaluations: List<GuessEvaluation>): Map<Char, LetterState> {
        val map = HashMap<Char, LetterState>()
        for (e in evaluations) {
            val norm = TextNormalize.normalize(e.guess)
            for (i in norm.indices) {
                val c = norm[i]
                val s = e.states[i]
                val cur = map[c]
                if (cur == null || s.ordinal > cur.ordinal) map[c] = s
            }
        }
        return map
    }
}
