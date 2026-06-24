package app.ascend.games.engine.games.clusters

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.games.engine.games.common.gameViewModel
import app.ascend.games.engine.core.GameLanguage
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.common.BaseGameVM
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.common.ControlButton
import app.ascend.games.engine.games.common.GameShell
import app.ascend.games.engine.games.grouping.GroupingEngine
import kotlin.random.Random

private val GROUP_COLORS = listOf(Color(0xFFF0A93A), Color(0xFF3F7FE0), Color(0xFF1E8E3E), Color(0xFF8A64C8))

data class ClustersState(val order: List<Int>, val selected: List<Int>, val solved: List<Int>, val mistakes: Int)

class ClustersVM(app: Application) : BaseGameVM<ClustersState>(app, GameId.CLUSTERS) {
    private val puzzle by lazy { GroupingEngine.daily(today, GameLanguage.current()) }
    // flat[pos] = (word, groupIndex), grouped in category order
    val flat by lazy {
        puzzle.categories.flatMapIndexed { gi, cat -> cat.members.map { it to gi } }
    }
    fun category(gi: Int) = puzzle.categories[gi]

    override fun initialState(): ClustersState {
        val order = (0 until 16).toMutableList().also { it.shuffle(Random(today * 1000003L + 17)) }
        return ClustersState(order, emptyList(), emptyList(), 0)
    }
    override fun isSolved(s: ClustersState) = s.solved.size == 4

    fun tap(pos: Int) {
        val s = state
        if (flat[pos].second in s.solved) return
        val sel = if (pos in s.selected) s.selected - pos else if (s.selected.size < 4) s.selected + pos else s.selected
        set(s.copy(selected = sel))
    }

    fun submit() {
        val s = state
        if (s.selected.size != 4) return
        val groups = s.selected.map { flat[it].second }
        if (groups.all { it == groups[0] } && groups[0] !in s.solved) {
            val solved = s.solved + groups[0]
            set(s.copy(solved = solved, selected = emptyList()))
            if (solved.size == 4) win()
        } else {
            set(s.copy(selected = emptyList(), mistakes = s.mistakes + 1))
        }
    }

    override fun hint() {
        val s = state
        val gi = (0 until 4).firstOrNull { it !in s.solved } ?: return
        val solved = s.solved + gi
        set(s.copy(solved = solved, selected = s.selected.filter { flat[it].second != gi }))
        if (solved.size == 4) win()
    }
}

@Composable
fun ClustersScreen(onBack: () -> Unit, vm: ClustersVM = gameViewModel()) {
    GameShell(
        vm = vm,
        controls = listOf(
            ControlButton("Hint", true, vm::hint),
            ControlButton("Submit", vm.state.selected.size == 4, vm::submit),
            ControlButton("Reset", true, vm::reset),
        ),
        hint = vm.game.rules[1],
        onBack = onBack,
        shareGrid = { ShareGrid() },
    ) { Board(vm) }
}

@Composable
private fun Board(vm: ClustersVM) {
    val s = vm.state
    Column(Modifier.width(330.dp)) {
        for (gi in s.solved) {
            val cat = vm.category(gi)
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .background(GROUP_COLORS[cat.level % 4], RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(cat.name.uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                Text(cat.members.joinToString(",  "), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        // remaining tiles in rows of 4
        val remaining = s.order.filter { vm.flat[it].second !in s.solved }
        remaining.chunked(4).forEach { rowPos ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (pos in rowPos) {
                    val sel = pos in s.selected
                    Box(
                        Modifier.weight(1f).heightIn(min = 56.dp)
                            .background(if (sel) Brain.Ink else Brain.Chip, RoundedCornerShape(10.dp))
                            .clickable { vm.tap(pos) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            vm.flat[pos].first,
                            color = if (sel) Color.White else Brain.Ink,
                            fontWeight = FontWeight.Bold, fontSize = 12.5.sp,
                            textAlign = TextAlign.Center, lineHeight = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Mistakes", color = Brain.Muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            for (i in 0 until 4) {
                Box(Modifier.padding(horizontal = 3.dp).size(10.dp).background(if (i < s.mistakes) Brain.Red else Color(0xFFD5D9DF), CircleShape))
            }
        }
    }
}

@Composable
private fun ShareGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        for (r in 0 until 4) Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in 0 until 4) Box(Modifier.size(13.dp).background(GROUP_COLORS[r], RoundedCornerShape(2.dp)))
        }
    }
}
