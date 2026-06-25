package app.ascend.games.engine.games.sudoku

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.games.engine.games.common.gameViewModel
import app.ascend.games.engine.core.Daily
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell

data class SudokuState(val cells: List<Int>, val sel: Int)

class SudokuVM(app: Application) : BaseGameVM<SudokuState>(app, GameId.SUDOKU) {
    // lazy: BaseGameVM builds initialState() during its own construction, before
    // these subclass fields would otherwise be initialized.
    private val puzzle by lazy { SudokuEngine.generate(Daily.random(today, "sudoku")) }
    val isGiven by lazy { BooleanArray(36) { puzzle.givens[it] != 0 } }
    private val solution by lazy { puzzle.solution.toList() }

    override fun initialState() = SudokuState(puzzle.givens.toList(), -1)
    override fun isSolved(s: SudokuState) = s.cells == solution

    fun select(i: Int) { if (!isGiven[i]) set(state.copy(sel = i)) }

    fun place(n: Int) {
        val i = state.sel
        if (i < 0 || isGiven[i]) return
        commit(state.copy(cells = state.cells.toMutableList().also { it[i] = n }))
    }

    override fun hint() {
        var t = state.sel
        if (t < 0 || state.cells[t] != 0) t = state.cells.indexOfFirst { it == 0 }
        if (t < 0) return
        commit(state.copy(cells = state.cells.toMutableList().also { it[t] = solution[t] }, sel = t))
    }

    fun conflicts(): Set<Int> {
        val cells = state.cells
        val bad = HashSet<Int>()
        fun scan(idx: List<Int>) {
            val seen = HashMap<Int, Int>()
            for (i in idx) {
                val v = cells[i]; if (v == 0) continue
                val p = seen[v]; if (p != null) { bad.add(p); bad.add(i) } else seen[v] = i
            }
        }
        for (r in 0 until 6) scan((0 until 6).map { r * 6 + it })
        for (c in 0 until 6) scan((0 until 6).map { it * 6 + c })
        for (br in 0 until 3) for (bc in 0 until 2) {
            val idx = ArrayList<Int>()
            for (dr in 0 until 2) for (dc in 0 until 3) idx.add((br * 2 + dr) * 6 + (bc * 3 + dc))
            scan(idx)
        }
        return bad
    }
}

@Composable
fun SudokuScreen(onBack: () -> Unit, vm: SudokuVM = gameViewModel()) {
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
        shareGrid = { Text("6", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp) },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Board(vm)
            Spacer16()
            NumberPad(vm)
        }
    }
}

@Composable
private fun Spacer16() = Box(Modifier.size(18.dp))

@Composable
private fun Board(vm: SudokuVM) {
    val conflicts = vm.conflicts()
    val thin = Color(0xFFE7E9EC)
    val thick = Color(0xFF16181C)
    Column(
        Modifier
            .width(306.dp)
            .aspectRatio(1f)
            .background(Brain.Card, RoundedCornerShape(10.dp))
            .drawBehind {
                val cw = size.width / 6
                for (i in 0..6) {
                    val major = i % 3 == 0
                    drawLine(if (major) thick else thin, Offset(i * cw, 0f), Offset(i * cw, size.height), if (major) 3f else 1f)
                }
                for (j in 0..6) {
                    val major = j % 2 == 0
                    drawLine(if (major) thick else thin, Offset(0f, j * cw), Offset(size.width, j * cw), if (major) 3f else 1f)
                }
            }
    ) {
        for (r in 0 until 6) {
            Row(Modifier.weight(1f)) {
                for (c in 0 until 6) {
                    val i = r * 6 + c
                    val v = vm.state.cells[i]
                    val sel = vm.state.sel == i
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (sel) Brain.Sel else Color.Transparent)
                            .clickable { vm.select(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (v != 0) Text(
                            "$v",
                            fontSize = 22.sp,
                            fontWeight = if (vm.isGiven[i]) FontWeight.Black else FontWeight.Bold,
                            color = when {
                                i in conflicts -> Brain.Red
                                vm.isGiven[i] -> Brain.Ink
                                else -> Brain.Blue
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberPad(vm: SudokuVM) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        for (n in 1..6) {
            Box(
                Modifier.size(width = 40.dp, height = 48.dp)
                    .background(Brain.BlueSoft, RoundedCornerShape(11.dp))
                    .clickable { vm.place(n) },
                contentAlignment = Alignment.Center
            ) { Text("$n", color = Brain.Blue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
        }
        Box(
            Modifier.size(width = 40.dp, height = 48.dp)
                .background(Brain.Chip, RoundedCornerShape(11.dp))
                .clickable { vm.place(0) },
            contentAlignment = Alignment.Center
        ) { Text("⌫", color = Brain.ChipInk, fontSize = 18.sp) }
    }
}
