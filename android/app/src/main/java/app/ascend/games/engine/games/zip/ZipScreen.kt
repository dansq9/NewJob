package app.ascend.games.engine.games.zip

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ascend.games.engine.core.Daily
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell
import kotlin.math.abs

class ZipVM(app: Application) : BaseGameVM<List<Int>>(app, GameId.ZIP) {
    val rows = 6; val cols = 6
    private val puzzle by lazy { ZipEngine.generate(Daily.random(today, "zip"), rows, cols, 7) }
    val numbers by lazy { puzzle.numbers }
    private val startCell by lazy { numbers.indexOfFirst { it == 1 } }

    override fun initialState(): List<Int> = listOf(startCell)
    override fun isSolved(s: List<Int>): Boolean = s.size == rows * cols

    fun hasWall(a: Int, b: Int) = puzzle.hasWall(a, b)
    private fun neighbor(a: Int, b: Int): Boolean =
        abs(a / cols - b / cols) + abs(a % cols - b % cols) == 1

    fun extend(i: Int) {
        if (i !in 0 until rows * cols) return
        val path = state
        val last = path.last()
        if (i == last) return
        if (path.size >= 2 && i == path[path.size - 2]) { commit(path.dropLast(1)); return }
        if (i !in path && neighbor(last, i) && !hasWall(last, i)) {
            val cn = numbers[i]
            if (cn != 0) {
                val mx = path.filter { numbers[it] > 0 }.maxOfOrNull { numbers[it] } ?: 0
                if (cn != mx + 1) return
            }
            commit(path + i)
        }
    }

    override fun hint() {
        val sol = puzzleSolution()
        val p = state
        if (p.size >= rows * cols) return
        if (p.indices.all { p[it] == sol[it] }) commit(p + sol[p.size])
    }

    private fun puzzleSolution(): IntArray = puzzle.solution

    override fun reset() { super.reset() }
}

@Composable
fun ZipScreen(onBack: () -> Unit, vm: ZipVM = viewModel()) {
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
        shareGrid = { ShareIcon() },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: ZipVM) {
    val rows = vm.rows; val cols = vm.cols
    val border = Brain.Border
    val pathFill = Color(0xFFDBE6FB)
    Box(
        Modifier
            .width(300.dp)
            .aspectRatio(1f)
            .pointerInput(Unit) {
                fun cellAt(o: Offset): Int {
                    val cw = size.width / cols; val ch = size.height / rows
                    val c = (o.x / cw).toInt().coerceIn(0, cols - 1)
                    val r = (o.y / ch).toInt().coerceIn(0, rows - 1)
                    return r * cols + c
                }
                detectDragGestures(
                    onDragStart = { vm.extend(cellAt(it)) },
                    onDrag = { ch, _ -> vm.extend(cellAt(ch.position)) }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    val cw = size.width / cols; val ch = size.height / rows
                    val c = (o.x / cw).toInt().coerceIn(0, cols - 1)
                    val r = (o.y / ch).toInt().coerceIn(0, rows - 1)
                    vm.extend(r * cols + c)
                }
            }
    ) {
        Canvas(Modifier.width(300.dp).aspectRatio(1f)) {
            val cw = size.width / cols; val ch = size.height / rows
            val pad = cw * 0.06f
            fun center(cell: Int) = Offset((cell % cols + 0.5f) * cw, (cell / cols + 0.5f) * ch)
            val inPath = vm.state.toHashSet()

            for (cell in 0 until rows * cols) {
                val r = cell / cols; val c = cell % cols
                val isCp = vm.numbers[cell] != 0
                drawRoundRect(
                    color = if (cell in inPath && !isCp) pathFill else Brain.Card,
                    topLeft = Offset(c * cw + pad, r * ch + pad),
                    size = Size(cw - 2 * pad, ch - 2 * pad),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = border,
                    topLeft = Offset(c * cw + pad, r * ch + pad),
                    size = Size(cw - 2 * pad, ch - 2 * pad),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f),
                    style = Stroke(width = 2f)
                )
            }

            if (vm.state.size > 1) {
                val sw = minOf(cw, ch) * 0.28f
                for (i in 0 until vm.state.size - 1) {
                    drawLine(Brain.Blue.copy(alpha = 0.85f), center(vm.state[i]), center(vm.state[i + 1]), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }

            // walls
            for (cell in 0 until rows * cols) {
                val r = cell / cols; val c = cell % cols
                if (c < cols - 1 && vm.hasWall(cell, cell + 1))
                    drawLine(Brain.Ink, Offset((c + 1) * cw, r * ch + pad), Offset((c + 1) * cw, (r + 1) * ch - pad), minOf(cw, ch) * 0.10f)
                if (r < rows - 1 && vm.hasWall(cell, cell + cols))
                    drawLine(Brain.Ink, Offset(c * cw + pad, (r + 1) * ch), Offset((c + 1) * cw - pad, (r + 1) * ch), minOf(cw, ch) * 0.10f)
            }

            // checkpoint numbers
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
                textSize = minOf(cw, ch) * 0.36f
            }
            for (cell in 0 until rows * cols) {
                val num = vm.numbers[cell]; if (num == 0) continue
                val ctr = center(cell)
                drawCircle(Brain.Blue, radius = minOf(cw, ch) * 0.3f, center = ctr)
                drawContext.canvas.nativeCanvas.drawText("$num", ctr.x, ctr.y - (paint.descent() + paint.ascent()) / 2, paint)
            }
        }
    }
}

@Composable
private fun ShareIcon() {
    Canvas(Modifier.width(60.dp).aspectRatio(1f).padding(8.dp)) {
        val s = size.minDimension
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(s * 0.2f, s * 0.8f)
            lineTo(s * 0.2f, s * 0.4f)
            cubicTo(s * 0.2f, s * 0.2f, s * 0.5f, s * 0.2f, s * 0.5f, s * 0.4f)
            cubicTo(s * 0.5f, s * 0.6f, s * 0.75f, s * 0.6f, s * 0.8f, s * 0.45f)
        }
        drawPath(p, Color.White, style = Stroke(width = s * 0.09f, cap = StrokeCap.Round))
    }
}
