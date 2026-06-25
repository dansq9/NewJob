package app.ascend.games.engine.games.patches

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.games.engine.games.common.gameViewModel
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell

/** 8x8 single-colour picture puzzles ('#' = filled). */
private val PICTURES = listOf(
    listOf("..####..", "..#..#..", "########", "########", "###..###", "###..###", "########", "########"),
    listOf(".##..##.", "########", "########", "########", ".######.", "..####..", "...##...", "........"),
    listOf("..####..", ".#....#.", "#.#..#.#", "#......#", "#.#..#.#", "#..##..#", ".#....#.", "..####.."),
)

private fun pictureFor(day: Long): IntArray {
    val pic = PICTURES[((day % PICTURES.size) + PICTURES.size).toInt() % PICTURES.size]
    return IntArray(64) { if (pic[it / 8][it % 8] == '#') 1 else 0 }
}

private fun runs(line: List<Int>): List<Int> {
    val out = ArrayList<Int>(); var n = 0
    for (v in line) { if (v == 1) n++ else if (n > 0) { out.add(n); n = 0 } }
    if (n > 0) out.add(n)
    return if (out.isEmpty()) listOf(0) else out
}

class PatchesVM(app: Application) : BaseGameVM<List<Int>>(app, GameId.PATCHES) {
    private val solution by lazy { pictureFor(today) }
    val rowClues by lazy { (0 until 8).map { r -> runs((0 until 8).map { solution[r * 8 + it] }) } }
    val colClues by lazy { (0 until 8).map { c -> runs((0 until 8).map { solution[it * 8 + c] }) } }

    override fun initialState(): List<Int> = List(64) { 0 }
    override fun isSolved(s: List<Int>): Boolean = (0 until 64).all { (solution[it] == 1) == (s[it] == 1) }

    fun tap(i: Int) = commit(state.toMutableList().also { it[i] = (it[i] + 1) % 3 })

    override fun hint() {
        val t = (0 until 64).firstOrNull { solution[it] == 1 && state[it] != 1 } ?: return
        commit(state.toMutableList().also { it[t] = 1 })
    }
}

@Composable
fun PatchesScreen(onBack: () -> Unit, vm: PatchesVM = gameViewModel()) {
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
        shareGrid = { ShareGrid(vm) },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: PatchesVM) {
    val s = 30.dp; val clue = 58.dp
    Column(Modifier.background(Brain.Card, RoundedCornerShape(8.dp)).padding(2.dp)) {
        // column clues row
        Row {
            Box(Modifier.size(clue))
            for (c in 0 until 8) {
                Column(
                    Modifier.width(s).height(clue).padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) { vm.colClues[c].forEach { Text("$it", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brain.ChipInk) } }
            }
        }
        for (r in 0 until 8) {
            Row {
                Row(
                    Modifier.width(clue).height(s).padding(end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) { vm.rowClues[r].forEach { Text("$it", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brain.ChipInk) } }
                for (c in 0 until 8) {
                    val i = r * 8 + c
                    val v = vm.state[i]
                    Box(
                        Modifier.size(s)
                            .background(if (v == 1) Brain.Blue else Brain.Card)
                            .border(0.5.dp, Brain.Border)
                            .clickable { vm.tap(i) },
                        contentAlignment = Alignment.Center
                    ) { if (v == 2) Text("✕", color = Color(0xFFAAB0B8), fontWeight = FontWeight.Black, fontSize = 14.sp) }
                }
            }
        }
    }
}

@Composable
private fun ShareGrid(vm: PatchesVM) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0 until 8) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in 0 until 8) {
                val on = vm.isSolvedCell(r * 8 + c)
                Box(Modifier.size(13.dp).background(if (on) Color.White else Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp)))
            }
        }
    }
}

private fun PatchesVM.isSolvedCell(i: Int): Boolean = state[i] == 1
