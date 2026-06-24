package app.ascend.games.engine.games.lightsout

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.ascend.games.engine.games.common.gameViewModel
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell

class LightsOutVM(app: Application) : BaseGameVM<List<Boolean>>(app, GameId.LIGHTSOUT) {
    override fun initialState(): List<Boolean> = LightsOutEngine.daily(today).toList()
    override fun isSolved(s: List<Boolean>): Boolean = s.none { it }

    fun tap(i: Int) {
        val arr = state.toBooleanArray()
        LightsOutEngine.press(arr, i)
        commit(arr.toList())
    }
}

@Composable
fun LightsOutScreen(onBack: () -> Unit, vm: LightsOutVM = gameViewModel()) {
    GameShell(
        vm = vm,
        controls = listOf(
            ControlButton("Undo", vm.canUndo, vm::undo),
            ControlButton("Reset", true, vm::reset),
        ),
        hint = vm.game.rules[1],
        onBack = onBack,
        shareGrid = { MiniGrid() },
    ) {
        Board(vm)
    }
}

@Composable
private fun Board(vm: LightsOutVM) {
    val cell = 56.dp
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (r in 0 until LightsOutEngine.SIZE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (c in 0 until LightsOutEngine.SIZE) {
                    val i = r * LightsOutEngine.SIZE + c
                    val on = vm.state[i]
                    Box(
                        Modifier
                            .size(cell)
                            .background(if (on) Brain.Blue else Color(0xFFDFE3EA), RoundedCornerShape(12.dp))
                            .clickable { vm.tap(i) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(5) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(5) {
                    Box(Modifier.size(12.dp).background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp)))
                }
            }
        }
    }
}
