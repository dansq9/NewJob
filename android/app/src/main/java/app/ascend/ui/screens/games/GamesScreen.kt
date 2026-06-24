package app.ascend.ui.screens.games

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import app.ascend.ui.components.AscendTopBar
import app.ascend.ui.theme.AscendColors
import app.ascend.games.engine.games.GameId

private fun iconFor(id: GameId): ImageVector = when (id) {
    GameId.PATCHES -> Icons.Outlined.Image
    GameId.SUDOKU -> Icons.Outlined.GridOn
    GameId.ZIP -> Icons.Outlined.Route
    GameId.QUEENS -> Icons.Outlined.Star
    GameId.TANGO -> Icons.Outlined.DarkMode
    GameId.G2048 -> Icons.Outlined.Apps
    GameId.CLUSTERS -> Icons.Outlined.BubbleChart
    GameId.CROSSWORD -> Icons.Outlined.Dashboard
    GameId.LIGHTSOUT -> Icons.Outlined.Lightbulb
}

@Composable
fun GamesScreen(nav: NavController) {
    Scaffold(
        containerColor = AscendColors.Bg,
        topBar = { AscendTopBar("Brain Games", onBack = { nav.popBackStack() }) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(2) }) {
                Text("A daily puzzle to keep your mind sharp between applications.",
                    color = AscendColors.Muted, fontSize = 14.sp)
            }
            items(GameId.all) { game ->
                PuzzleCard(game) { nav.navigate("game/${game.id}") }
            }
        }
    }
}

@Composable
private fun PuzzleCard(game: GameId, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp), color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Column(Modifier.padding(15.dp)) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AscendColors.ChipIndigo), contentAlignment = Alignment.Center) {
                Icon(iconFor(game), null, tint = AscendColors.Indigo)
            }
            Spacer(Modifier.height(12.dp))
            Text(game.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = AscendColors.Ink)
            Text(game.blurb, fontSize = 12.sp, color = AscendColors.Muted2, lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick)) {
                Icon(Icons.Filled.PlayArrow, null, tint = AscendColors.Indigo, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Play", color = AscendColors.Indigo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
