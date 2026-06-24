package app.ascend.games.engine.games.twenty48

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

data class T2048(val tiles: List<Int>, val score: Int, val seed: Int)

class Twenty48VM(app: Application) : BaseGameVM<T2048>(app, GameId.G2048) {

    override fun initialState(): T2048 {
        val tiles = IntArray(16)
        var seed = Daily.seed(today, "g2048").toInt()
        seed = spawn(tiles, seed)
        seed = spawn(tiles, seed)
        return T2048(tiles.toList(), 0, seed)
    }

    override fun isSolved(s: T2048): Boolean = s.tiles.any { it >= Game2048.WIN_TILE }

    fun move(dir: Dir) {
        if (won || lost) return
        val res = Game2048.slide(state.tiles.toIntArray(), dir)
        if (!res.moved) return
        val arr = res.board
        val seed = spawn(arr, state.seed)
        val score = state.score + res.gained
        commit(T2048(arr.toList(), score, seed))
        progress.recordScore(game.id, score)
        if (!isSolved(state) && !Game2048.canMove(arr)) markLost()
    }

    private fun rnd(seed: Int): Double {
        var t = seed
        t = t xor (t shl 13); t = t xor (t ushr 17); t = t xor (t shl 5)
        return ((t.toLong() and 0xffffffffL).toDouble()) / 4294967296.0
    }

    private fun spawn(tiles: IntArray, seed: Int): Int {
        val empties = (0 until 16).filter { tiles[it] == 0 }
        if (empties.isEmpty()) return seed
        val cell = empties[(rnd(seed) * empties.size).toInt().coerceIn(0, empties.size - 1)]
        tiles[cell] = if (rnd(seed + 1) < 0.9) 2 else 4
        return seed + 2
    }
}

@Composable
fun Twenty48Screen(onBack: () -> Unit, vm: Twenty48VM = viewModel()) {
    GameShell(
        vm = vm,
        controls = listOf(
            ControlButton("Undo", vm.canUndo, vm::undo),
            ControlButton("Restart", true, vm::reset),
        ),
        hint = vm.game.rules[1],
        onBack = onBack,
        scoreHeader = { ScoreHeader(vm) },
        winTitle = "You hit 2048!",
        loseScoreText = "You scored ${vm.state.score} points today",
        shareGrid = { Text("2048", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp) },
    ) {
        Board(vm)
    }
}

@Composable
private fun ScoreHeader(vm: Twenty48VM) {
    Row(Modifier.width(310.dp).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ScoreBox(Modifier.weight(1f), "SCORE", "${vm.state.score}")
        ScoreBox(Modifier.weight(1f), "BEST", "${maxOf(vm.stats().bestScore, vm.state.score)}")
    }
}

@Composable
private fun ScoreBox(modifier: Modifier, label: String, value: String) {
    Column(
        modifier.background(Brain.Card, RoundedCornerShape(12.dp)).padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Brain.Muted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Text(value, color = Brain.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
    }
}

private val TILE: Map<Int, Pair<Color, Color>> = mapOf(
    0 to (Color(0xFFE9EBF0) to Color.Transparent),
    2 to (Color(0xFFEEF2FB) to Color(0xFF42526B)),
    4 to (Color(0xFFDCE7FB) to Color(0xFF2F4A78)),
    8 to (Color(0xFFBCD2F5) to Color(0xFF1C3E74)),
    16 to (Color(0xFF92B6EF) to Color.White),
    32 to (Color(0xFF5E8FE0) to Color.White),
    64 to (Color(0xFF356FD2) to Color.White),
    128 to (Color(0xFF1F57BD) to Color.White),
    256 to (Color(0xFF17479E) to Color.White),
    512 to (Color(0xFF123C86) to Color.White),
    1024 to (Color(0xFF0E2F6B) to Color.White),
    2048 to (Color(0xFF1E8E3E) to Color.White),
)

@Composable
private fun Board(vm: Twenty48VM) {
    val cell = 70.dp
    Column(
        modifier = Modifier
            .background(Color(0xFFBBADA0), RoundedCornerShape(12.dp))
            .padding(10.dp)
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { ch, d -> dx += d.x; dy += d.y; ch.consume() },
                    onDragEnd = {
                        if (maxOf(abs(dx), abs(dy)) >= 24f) {
                            val dir = if (abs(dx) > abs(dy)) (if (dx > 0) Dir.RIGHT else Dir.LEFT)
                            else (if (dy > 0) Dir.DOWN else Dir.UP)
                            vm.move(dir)
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (r in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (c in 0 until 4) {
                    val v = vm.state.tiles[r * 4 + c]
                    val (bg, fg) = TILE[v] ?: (Color(0xFF1E8E3E) to Color.White)
                    Box(
                        Modifier.size(cell).background(bg, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (v != 0) Text(
                            "$v", color = fg, fontWeight = FontWeight.ExtraBold,
                            fontSize = if (v < 100) 32.sp else if (v < 1000) 26.sp else 20.sp
                        )
                    }
                }
            }
        }
    }
}
