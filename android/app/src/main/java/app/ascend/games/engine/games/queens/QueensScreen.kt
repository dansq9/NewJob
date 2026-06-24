package app.ascend.games.engine.games.queens

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ascend.games.engine.core.Daily
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell
import kotlin.math.abs

private val REGION_COLORS = listOf(
    Color(0xFFFBE4E8), Color(0xFFFDECD9), Color(0xFFE3F2E7),
    Color(0xFFDEF0F2), Color(0xFFE6EBFB), Color(0xFFEFE6FB),
)

class QueensVM(app: Application) : BaseGameVM<List<Int>>(app, GameId.QUEENS) {
    val n = 6
    private val puzzle by lazy { QueensEngine.generate(Daily.random(today, "queens6"), n) }
    val region by lazy { puzzle.region }

    override fun initialState(): List<Int> = List(n * n) { 0 }
    override fun isSolved(s: List<Int>): Boolean = valid(s)

    fun tap(i: Int) = commit(state.toMutableList().also { it[i] = (it[i] + 1) % 3 })

    override fun hint() {
        val placed = state.withIndex().filter { it.value == 2 }.map { it.index }.toSet()
        val t = (0 until n).map { it * n + puzzle.solution[it] }.firstOrNull { it !in placed } ?: return
        commit(state.toMutableList().also { it[t] = 2 })
    }

    fun conflicts(): Set<Int> {
        val qs = state.indices.filter { state[it] == 2 }
        val bad = HashSet<Int>()
        for (a in qs.indices) for (b in a + 1 until qs.size) {
            val i = qs[a]; val j = qs[b]
            val r1 = i / n; val c1 = i % n; val r2 = j / n; val c2 = j % n
            if (r1 == r2 || c1 == c2 || region[i] == region[j] || (abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1)) {
                bad.add(i); bad.add(j)
            }
        }
        return bad
    }

    private fun valid(cells: List<Int>): Boolean {
        val qs = cells.indices.filter { cells[it] == 2 }
        if (qs.size != n) return false
        val rows = HashSet<Int>(); val cols = HashSet<Int>(); val regs = HashSet<Int>()
        for (i in qs) {
            if (!rows.add(i / n) || !cols.add(i % n) || !regs.add(region[i])) return false
        }
        for (a in qs.indices) for (b in a + 1 until qs.size) {
            val r1 = qs[a] / n; val c1 = qs[a] % n; val r2 = qs[b] / n; val c2 = qs[b] % n
            if (abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1) return false
        }
        return true
    }
}

@Composable
fun QueensScreen(onBack: () -> Unit, vm: QueensVM = viewModel()) {
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
        shareGrid = { Text("★", color = Color.White, fontSize = 64.sp) },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: QueensVM) {
    val n = vm.n
    val conflicts = vm.conflicts()
    val grid = Color.Black.copy(alpha = 0.07f)
    val frame = Brain.Ink
    Box(Modifier.width(306.dp).aspectRatio(1f)) {
        Column(Modifier.fillMaxWidth()) {
            for (r in 0 until n) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until n) {
                        val i = r * n + c
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .background(REGION_COLORS[vm.region[i]])
                                .clickable { vm.tap(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            when (vm.state[i]) {
                                1 -> Box(Modifier.size(14.dp).rotate(45f).border(2.5.dp, Color(0xFF7C8089), RoundedCornerShape(3.dp)))
                                2 -> Text("★", fontSize = 30.sp, color = if (i in conflicts) Brain.Red else Brain.Blue)
                            }
                        }
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            val cw = size.width / n
            for (i in 1 until n) {
                drawLine(grid, Offset(i * cw, 0f), Offset(i * cw, size.height), 1f)
                drawLine(grid, Offset(0f, i * cw), Offset(size.width, i * cw), 1f)
            }
            val w = 6f
            drawLine(frame, Offset(0f, 0f), Offset(size.width, 0f), w)
            drawLine(frame, Offset(0f, size.height), Offset(size.width, size.height), w)
            drawLine(frame, Offset(0f, 0f), Offset(0f, size.height), w)
            drawLine(frame, Offset(size.width, 0f), Offset(size.width, size.height), w)
        }
    }
}
