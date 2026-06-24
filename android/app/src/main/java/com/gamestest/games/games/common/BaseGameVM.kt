package com.gamestest.games.games.common

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamestest.games.core.GameProgress
import com.gamestest.games.games.GameId
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

data class ControlButton(val label: String, val enabled: Boolean, val onClick: () -> Unit)

/**
 * Shared per-game state: a daily-seeded immutable [state] of type [T] with
 * undo/redo, a one-second timer, win/lose flags, modal toggles, and streak
 * recording. Each game subclasses with its own state type + interactions.
 */
abstract class BaseGameVM<T>(app: Application, val game: GameId) : AndroidViewModel(app) {

    protected val today: Long = LocalDate.now().toEpochDay()
    protected val progress: GameProgress = GameProgress.get(app)

    protected abstract fun initialState(): T
    protected abstract fun isSolved(s: T): Boolean

    var state: T by mutableStateOf(initialStateSafe()); private set
    private fun initialStateSafe(): T = initialState()

    private val history = ArrayDeque<T>()
    private val future = ArrayDeque<T>()
    val canUndo: Boolean get() = history.isNotEmpty()
    val canRedo: Boolean get() = future.isNotEmpty()

    var elapsed by mutableIntStateOf(0); private set
    var running by mutableStateOf(true); private set
    var won by mutableStateOf(false); private set
    var lost by mutableStateOf(false); protected set
    var rulesOpen by mutableStateOf(false)
    var shareOpen by mutableStateOf(false)
    var rank by mutableIntStateOf(0); private set

    init {
        viewModelScope.launch {
            while (true) { delay(1000); if (running && !won && !lost) elapsed++ }
        }
    }

    /** Replace state, pushing the previous onto the undo stack; auto-wins if solved. */
    protected open fun commit(next: T) {
        history.addLast(state)
        future.clear()
        state = next
        if (!won && isSolved(next)) win()
    }

    /** Replace state without undo history (selection-only changes). */
    protected fun set(next: T) { state = next }

    fun undo() {
        if (history.isEmpty()) return
        future.addLast(state)
        state = history.removeLast()
        won = false; lost = false
    }

    fun redo() {
        if (future.isEmpty()) return
        history.addLast(state)
        state = future.removeLast()
        if (isSolved(state)) win()
    }

    open fun reset() {
        state = initialState()
        history.clear(); future.clear()
        won = false; lost = false; running = true
    }

    open fun hint() {}

    protected fun win() {
        if (won) return
        won = true
        running = false
        progress.recordCompletion(game.id, elapsed)
        rank = maxOf(1, (elapsed * 2.4).roundToInt() % 780 + 14)
    }

    protected fun markLost() { lost = true; running = false }

    fun stats(): GameProgress.Stats = progress.stats(game.id)
}
