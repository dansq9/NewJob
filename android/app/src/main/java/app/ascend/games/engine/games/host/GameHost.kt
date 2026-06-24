// Vendored Brain Games engine, namespaced under app.ascend.games.engine
// (renamed from the original com.gamestest.* prototype package). If this engine
// is later extracted into its own module/library, give it a real published
// package/coordinates here.
package app.ascend.games.engine.games.host

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.games.engine.games.GameId
import app.ascend.games.engine.games.clusters.ClustersScreen
import app.ascend.games.engine.games.common.Brain
import app.ascend.games.engine.games.lightsout.LightsOutScreen
import app.ascend.games.engine.games.minicross.MiniCrossScreen
import app.ascend.games.engine.games.patches.PatchesScreen
import app.ascend.games.engine.games.queens.QueensScreen
import app.ascend.games.engine.games.sudoku.SudokuScreen
import app.ascend.games.engine.games.tango.TangoScreen
import app.ascend.games.engine.games.twenty48.Twenty48Screen
import app.ascend.games.engine.games.zip.ZipScreen

@Composable
fun GameHost(gameId: String, onBack: () -> Unit) {
    when (GameId.byId(gameId)) {
        GameId.PATCHES -> PatchesScreen(onBack)
        GameId.SUDOKU -> SudokuScreen(onBack)
        GameId.ZIP -> ZipScreen(onBack)
        GameId.QUEENS -> QueensScreen(onBack)
        GameId.TANGO -> TangoScreen(onBack)
        GameId.G2048 -> Twenty48Screen(onBack)
        GameId.CLUSTERS -> ClustersScreen(onBack)
        GameId.CROSSWORD -> MiniCrossScreen(onBack)
        GameId.LIGHTSOUT -> LightsOutScreen(onBack)
        null -> ComingSoon("Game", onBack)
    }
}

@Composable
private fun ComingSoon(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brain.Page)) {
        Row(
            Modifier.fillMaxWidth().background(Brain.Card).windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(34.dp).background(Brain.Page, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Brain.InkSoft, modifier = Modifier.size(18.dp)) }
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Brain.Ink)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🧩", fontSize = 40.sp)
                Text("Coming soon", color = Brain.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("This board is being wired up next.", color = Brain.Muted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
    }
}
