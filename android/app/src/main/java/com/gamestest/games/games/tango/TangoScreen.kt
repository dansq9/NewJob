package com.gamestest.games.games.tango

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamestest.games.core.Daily
import com.gamestest.games.games.GameId
import com.gamestest.games.games.common.BaseGameVM
import com.gamestest.games.games.common.Brain
import com.gamestest.games.games.common.ControlButton
import com.gamestest.games.games.common.GameShell

class TangoVM(app: Application) : BaseGameVM<List<Int>>(app, GameId.TANGO) {
    val size = 6
    private val puzzle by lazy { TangoEngine.generate(Daily.random(today, "tango"), size) }
    val locked by lazy { (0 until size * size).filter { puzzle.givens[it] != -1 }.toSet() }
    private val solution by lazy { puzzle.solution.toList() }

    override fun initialState(): List<Int> = puzzle.givens.toList()
    override fun isSolved(s: List<Int>): Boolean = s == solution

    fun hEdge(r: Int, c: Int): Edge = puzzle.hEdge(r, c)
    fun vEdge(r: Int, c: Int): Edge = puzzle.vEdge(r, c)

    fun tap(i: Int) {
        if (i in locked) return
        commit(state.toMutableList().also { it[i] = when (it[i]) { -1 -> 0; 0 -> 1; else -> -1 } })
    }

    override fun hint() {
        val t = state.indexOfFirst { it == -1 }
        if (t < 0) return
        commit(state.toMutableList().also { it[t] = solution[t] })
    }

    fun conflicts(): Set<Int> {
        val s = size; val g = state; val bad = HashSet<Int>()
        fun at(r: Int, c: Int) = g[r * s + c]
        for (r in 0 until s) for (c in 0 until s - 2) {
            val a = at(r, c); if (a != -1 && a == at(r, c + 1) && a == at(r, c + 2)) { bad.add(r * s + c); bad.add(r * s + c + 1); bad.add(r * s + c + 2) }
        }
        for (c in 0 until s) for (r in 0 until s - 2) {
            val a = at(r, c); if (a != -1 && a == at(r + 1, c) && a == at(r + 2, c)) { bad.add(r * s + c); bad.add((r + 1) * s + c); bad.add((r + 2) * s + c) }
        }
        for (r in 0 until s) for (v in 0..1) if ((0 until s).count { at(r, it) == v } > s / 2) (0 until s).forEach { if (at(r, it) == v) bad.add(r * s + it) }
        for (c in 0 until s) for (v in 0..1) if ((0 until s).count { at(it, c) == v } > s / 2) (0 until s).forEach { if (at(it, c) == v) bad.add(it * s + c) }
        return bad
    }
}

private const val S = 48
private const val G = 4
private const val P = S + G

@Composable
fun TangoScreen(onBack: () -> Unit, vm: TangoVM = viewModel()) {
    GameShell(
        vm = vm,
        controls = listOf(
            ControlButton("Undo", vm.canUndo, vm::undo),
            ControlButton("Redo", vm.canRedo, vm::redo),
            ControlButton("Hint", true, vm::hint),
            ControlButton("Reset", true, vm::reset),
        ),
        hint = vm.game.rules[1],
        onBack = onBack,
        shareGrid = { Text("☽", color = Color.White, fontSize = 60.sp) },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: TangoVM) {
    val n = vm.size
    val conflicts = vm.conflicts()
    val boardSize: Dp = (S * n + G * (n - 1)).dp
    Box(Modifier.size(boardSize)) {
        for (i in 0 until n * n) {
            val v = vm.state[i]
            val locked = i in vm.locked
            Box(
                Modifier
                    .offset(x = ((i % n) * P).dp, y = ((i / n) * P).dp)
                    .size(S.dp)
                    .background(
                        if (i in conflicts) Color(0xFFFFCDD2) else if (locked) Color(0xFFECEEF1) else Brain.Card,
                        RoundedCornerShape(11.dp)
                    )
                    .border(1.5.dp, if (locked) Color(0xFFDCDFE4) else Brain.Border, RoundedCornerShape(11.dp))
                    .clickable { vm.tap(i) },
                contentAlignment = Alignment.Center
            ) {
                when (v) {
                    0 -> Box(Modifier.size(30.dp).background(Brain.Sun, CircleShape))
                    1 -> Box(Modifier.size(30.dp).background(Brain.Moon, CircleShape))
                }
            }
        }
        for (r in 0 until n) for (c in 0 until n - 1) {
            if (vm.hEdge(r, c) != Edge.NONE) EdgeBadge(vm.hEdge(r, c), x = (c * P + S + G / 2).dp, y = (r * P + S / 2).dp)
        }
        for (r in 0 until n - 1) for (c in 0 until n) {
            if (vm.vEdge(r, c) != Edge.NONE) EdgeBadge(vm.vEdge(r, c), x = (c * P + S / 2).dp, y = (r * P + S + G / 2).dp)
        }
    }
}

@Composable
private fun EdgeBadge(edge: Edge, x: Dp, y: Dp) {
    val d = 18.dp
    Box(
        Modifier.offset(x = x - d / 2, y = y - d / 2).size(d)
            .background(Brain.Card, CircleShape).border(1.5.dp, Color(0xFFCFD3DA), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(if (edge == Edge.EQUAL) "=" else "×", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Brain.ChipInk)
    }
}
