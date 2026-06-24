package com.gamestest.games.games.grouping

import com.gamestest.games.core.GameLanguage
import kotlin.random.Random

/** A category and its four members. [level] 0 (easiest) .. 3 (trickiest) for color coding. */
data class GroupingCategory(val name: String, val members: List<String>, val level: Int)

data class GroupingPuzzle(val categories: List<GroupingCategory>) {
    val words: List<String> = categories.flatMap { it.members }
    fun categoryOf(word: String): GroupingCategory? = categories.firstOrNull { word in it.members }
}

enum class GroupOutcome { CORRECT, ONE_AWAY, WRONG }

data class GroupGuess(val outcome: GroupOutcome, val category: GroupingCategory?)

/**
 * Word-grouping engine (original puzzle data; not NYT's). 16 words form four
 * hidden groups of four. [evaluate] reports CORRECT / ONE_AWAY / WRONG so the UI
 * can pop the solved row or shake on a near miss.
 */
object GroupingEngine {
    const val GROUP_SIZE = 4
    const val GROUP_COUNT = 4
    const val MAX_MISTAKES = 4

    fun daily(epochDay: Long, language: GameLanguage = GameLanguage.ENGLISH): GroupingPuzzle {
        val pool = GroupingData.pool(language)
        val idx = ((epochDay % pool.size) + pool.size).toInt() % pool.size
        return pool[idx]
    }

    /** Deterministic shuffle of the 16 words for a given day. */
    fun shuffledWords(puzzle: GroupingPuzzle, epochDay: Long): List<String> =
        puzzle.words.shuffled(Random(epochDay * 1000003L + 17))

    fun evaluate(selection: List<String>, puzzle: GroupingPuzzle): GroupGuess {
        require(selection.size == GROUP_SIZE) { "select exactly $GROUP_SIZE" }
        val sel = selection.toSet()
        puzzle.categories.firstOrNull { it.members.toSet() == sel }?.let {
            return GroupGuess(GroupOutcome.CORRECT, it)
        }
        // "one away" = exactly three of the four belong to a single category
        val near = puzzle.categories.firstOrNull { cat -> sel.count { it in cat.members } == 3 }
        return if (near != null) GroupGuess(GroupOutcome.ONE_AWAY, near)
        else GroupGuess(GroupOutcome.WRONG, null)
    }
}
