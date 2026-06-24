package app.ascend.games.engine.core

import android.content.Context

/**
 * Loads the per-language word assets (under `assets/words/<lang>/`) into a
 * [WordSource] for the word engines (Guess, Letters). Cached per language.
 *
 * Swap the asset files for a full open dictionary (e.g. ENABLE / dwyl
 * `english-words`, or an open Portuguese list) without any code change.
 */
object WordRepository {

    private val cache = HashMap<String, WordSource>()

    fun forLanguage(context: Context, language: GameLanguage = GameLanguage.current()): WordSource =
        cache.getOrPut(language.code) {
            val dir = "words/${language.assetDir}"
            val answers = read(context, "$dir/guess_answers.txt")
            val valid = (answers + read(context, "$dir/guess_valid.txt")).toSet()
            val dictionary = read(context, "$dir/dictionary.txt").toSet()
            WordSource(answers, valid, dictionary)
        }

    private fun read(context: Context, path: String): List<String> =
        context.assets.open(path).bufferedReader().useLines { seq ->
            seq.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
}
