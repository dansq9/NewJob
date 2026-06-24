package app.ascend.games.engine.core

/**
 * Pure-Kotlin word data passed into the word-based engines (Guess, Letters).
 *
 * Keeping this an injected value object — rather than reading Android assets
 * inside the engines — means the engines stay portable and unit-testable, and
 * the actual word lists can be swapped (e.g. for a full open dictionary like
 * ENABLE / dwyl `english-words`) without touching engine code.
 *
 * No NYT word lists are used. [answers] is the curated daily-target pool;
 * [valid] is the (super)set of words accepted as guesses; [dictionary] is the
 * general word set used by Letters.
 */
class WordSource(
    val answers: List<String>,
    valid: Set<String>,
    dictionary: Set<String>,
) {
    private val validSet: Set<String> = valid.mapTo(HashSet()) { it.lowercase() }
    private val dictSet: Set<String> = dictionary.mapTo(HashSet()) { it.lowercase() }

    fun isValidGuess(word: String): Boolean = word.lowercase() in validSet
    fun inDictionary(word: String): Boolean = word.lowercase() in dictSet
    val dictionary: Set<String> get() = dictSet
}
