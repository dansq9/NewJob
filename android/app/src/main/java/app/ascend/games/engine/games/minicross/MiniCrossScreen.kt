package app.ascend.games.engine.games.minicross

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.ascend.games.engine.core.GameLanguage
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell

data class CrossState(val grid: List<Char>, val sel: Int, val across: Boolean)

class MiniCrossVM(app: Application) : BaseGameVM<CrossState>(app, GameId.CROSSWORD) {
    private val puzzle by lazy { MiniCrossEngine.daily(today, GameLanguage.ENGLISH) }
    val size by lazy { puzzle.size }
    private val solution by lazy { puzzle.solution }
    val numbers by lazy {
        val m = HashMap<Int, Int>()
        for (cl in puzzle.clues) m[cl.row * puzzle.size + cl.col] = cl.number
        m
    }

    fun isBlock(i: Int) = puzzle.isBlock(i)

    override fun initialState(): CrossState {
        val grid = (0 until puzzle.size * puzzle.size).map { if (puzzle.isBlock(it)) '#' else ' ' }
        val first = grid.indexOfFirst { it != '#' }
        return CrossState(grid, first, true)
    }

    override fun isSolved(s: CrossState): Boolean =
        s.grid.indices.all { isBlock(it) || s.grid[it] == solution[it] }

    fun tap(i: Int) {
        if (isBlock(i)) return
        set(if (state.sel == i) state.copy(across = !state.across) else state.copy(sel = i))
    }

    fun type(ch: Char) {
        val s = state
        if (isBlock(s.sel)) return
        val grid = s.grid.toMutableList(); grid[s.sel] = ch
        val next = advance(s.sel, s.across)
        set(s.copy(grid = grid, sel = next))
        if (isSolved(state)) win()
    }

    fun backspace() {
        val s = state
        val grid = s.grid.toMutableList()
        if (grid[s.sel] != ' ') {
            grid[s.sel] = ' '; set(s.copy(grid = grid))
        } else {
            val prev = retreat(s.sel, s.across)
            if (prev != s.sel) { grid[prev] = ' '; set(s.copy(grid = grid, sel = prev)) }
        }
    }

    override fun hint() {
        val s = state
        var t = s.sel
        if (s.grid[t] == solution[t]) t = s.grid.indices.firstOrNull { !isBlock(it) && s.grid[it] != solution[it] } ?: return
        val grid = s.grid.toMutableList(); grid[t] = solution[t]
        set(s.copy(grid = grid, sel = t))
        if (isSolved(state)) win()
    }

    fun currentClue(): Pair<String, String> {
        val s = state; val n = puzzle.size
        val r = s.sel / n; val c = s.sel % n
        for (cl in puzzle.clues) {
            if (s.across && cl.dir == CrossDir.ACROSS && cl.row == r && c in cl.col until cl.col + cl.length)
                return "${cl.number} Across" to cl.clue
            if (!s.across && cl.dir == CrossDir.DOWN && cl.col == c && r in cl.row until cl.row + cl.length)
                return "${cl.number} Down" to cl.clue
        }
        return "" to ""
    }

    private fun advance(i: Int, across: Boolean): Int {
        val n = puzzle.size; val r = i / n; val c = i % n
        val j = if (across) (if (c < n - 1) i + 1 else i) else (if (r < n - 1) i + n else i)
        return if (isBlock(j)) i else j
    }

    private fun retreat(i: Int, across: Boolean): Int {
        val n = puzzle.size; val r = i / n; val c = i % n
        val j = if (across) (if (c > 0) i - 1 else i) else (if (r > 0) i - n else i)
        return if (isBlock(j)) i else j
    }
}

@Composable
fun MiniCrossScreen(onBack: () -> Unit, vm: MiniCrossVM = gameViewModel()) {
    GameShell(
        vm = vm,
        controls = listOf(
            ControlButton("Hint", true, vm::hint),
            ControlButton("Reset", true, vm::reset),
        ),
        hint = vm.game.rules[1],
        onBack = onBack,
        shareGrid = { ShareGrid() },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: MiniCrossVM) {
    val n = vm.size
    val s = vm.state
    val (clueLabel, clueText) = vm.currentClue()
    val r = s.sel / n; val c = s.sel % n
    val inWord = HashSet<Int>().apply {
        if (s.across) for (cc in 0 until n) add(r * n + cc) else for (rr in 0 until n) add(rr * n + c)
    }
    val cell = 56.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.width(cell * n).background(Brain.Ink, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(clueLabel, color = Color(0xFF9BBCF0), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text(clueText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.width(cell * n).background(Brain.Ink, RoundedCornerShape(6.dp)).padding(2.dp)) {
            for (rr in 0 until n) {
                Row {
                    for (cc in 0 until n) {
                        val i = rr * n + cc
                        if (vm.isBlock(i)) {
                            Box(Modifier.size(cell).background(Brain.Ink))
                        } else {
                            val sel = s.sel == i
                            Box(
                                Modifier.size(cell).padding(0.5.dp)
                                    .background(if (sel) Color(0xFFFFE39C) else if (i in inWord) Brain.BlueSoft else Brain.Card)
                                    .clickable { vm.tap(i) },
                                contentAlignment = Alignment.Center
                            ) {
                                vm.numbers[i]?.let {
                                    Text("$it", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Brain.Muted, modifier = Modifier.align(Alignment.TopStart).padding(start = 3.dp, top = 1.dp))
                                }
                                val ch = s.grid[i]
                                if (ch != ' ') Text("$ch", fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = Brain.Ink)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Keyboard(vm, cell * n)
    }
}

@Composable
private fun Keyboard(vm: MiniCrossVM, width: androidx.compose.ui.unit.Dp) {
    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
    Column(Modifier.width(width), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEachIndexed { ri, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (ch in row) {
                    Box(
                        Modifier.weight(1f).height(42.dp).background(Brain.Card, RoundedCornerShape(7.dp)).clickable { vm.type(ch) },
                        contentAlignment = Alignment.Center
                    ) { Text("$ch", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brain.Ink) }
                }
                if (ri == 2) Box(
                    Modifier.weight(1.6f).height(42.dp).background(Color(0xFFDFE3EA), RoundedCornerShape(7.dp)).clickable { vm.backspace() },
                    contentAlignment = Alignment.Center
                ) { Text("⌫", fontSize = 16.sp, color = Brain.Ink) }
            }
        }
    }
}

@Composable
private fun ShareGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (r in 0 until 5) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            for (c in 0 until 5) Box(Modifier.size(11.dp).background(Color.White, RoundedCornerShape(2.dp)))
        }
    }
}
