package app.ascend.games.engine.games.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GameShell(
    vm: BaseGameVM<*>,
    controls: List<ControlButton>,
    hint: String,
    onBack: () -> Unit,
    scoreHeader: (@Composable () -> Unit)? = null,
    winTitle: String = "Solved!",
    loseScoreText: String? = null,
    shareGrid: @Composable () -> Unit = {},
    board: @Composable () -> Unit,
) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize().background(Brain.Page)) {
        Column(Modifier.fillMaxSize()) {
            Header(vm, onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                scoreHeader?.invoke()
                board()
                Spacer(Modifier.height(18.dp))
                Text(
                    hint,
                    color = Color(0xFF8A9098),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            ControlsBar(controls)
        }

        Scrim(visible = vm.rulesOpen, onClick = { vm.rulesOpen = false }) {
            RulesSheet(vm) { vm.rulesOpen = false }
        }
        Scrim(visible = vm.lost && loseScoreText != null, onClick = {}) {
            LoseModal(loseScoreText ?: "", onRetry = { vm.reset() }, onBack = onBack)
        }
        Scrim(visible = vm.won, onClick = {}) {
            WinModal(vm, winTitle, onShare = { vm.shareOpen = true }, onBack = onBack)
        }
        Scrim(visible = vm.shareOpen, onClick = { vm.shareOpen = false }) {
            ShareSheet(vm, shareGrid, onShare = { shareResult(context, vm) }, onCopy = { copyResult(context, vm) })
        }
    }
}

@Composable
private fun Header(vm: BaseGameVM<*>, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Brain.Card)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircleIcon(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Brain.InkSoft, modifier = Modifier.size(18.dp))
            }
            Text(vm.game.title, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, color = Brain.Ink, modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .background(Brain.Page, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Schedule, null, tint = Brain.Muted, modifier = Modifier.size(13.dp))
                Text(formatTime(vm.elapsed), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Brain.Ink)
            }
            CircleIcon(onClick = { vm.rulesOpen = true }) {
                Text("?", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Brain.Blue)
            }
        }
    }
}

@Composable
private fun CircleIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).background(Brain.Page, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ControlsBar(controls: List<ControlButton>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brain.Card)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (c in controls) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Brain.Chip, RoundedCornerShape(12.dp))
                    .clickable(enabled = c.enabled, onClick = c.onClick)
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    c.label,
                    color = if (c.enabled) Brain.InkSoft else Brain.InkSoft.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun Scrim(visible: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier.fillMaxSize().background(Brain.Scrim).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { content() }
    }
}

@Composable
private fun RulesSheet(vm: BaseGameVM<*>, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = true, enter = slideInVertically { it }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brain.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(22.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).background(Color(0xFFDADDE2), RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(16.dp))
                Text("How to play ${vm.game.title}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Brain.Ink)
                Spacer(Modifier.height(16.dp))
                vm.game.rules.forEachIndexed { i, r ->
                    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                        Box(Modifier.size(24.dp).background(Brain.Blue, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Text(r, color = Color(0xFF42474E), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                PrimaryButton("Got it", onClose)
            }
        }
    }
}

@Composable
private fun WinModal(vm: BaseGameVM<*>, title: String, onShare: () -> Unit, onBack: () -> Unit) {
    val stats = vm.stats()
    val best = stats.bestSeconds
    val winBest = if (best in 1 until vm.elapsed) "best ${formatTime(best)}" else "new best!"
    ModalCard {
        Box(Modifier.size(66.dp).background(Brain.GreenBg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Brain.Green, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, color = Brain.Ink)
        Text("${vm.game.title} · today's puzzle", color = Brain.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatBox(Modifier.weight(1f), formatTime(vm.elapsed), winBest)
            StatBox(Modifier.weight(1f), "${maxOf(stats.streak, 1)}", "day streak")
        }
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().background(Brain.BlueSoft, RoundedCornerShape(14.dp)).padding(14.dp)
        ) {
            Text("TODAY'S LEADERBOARD", color = Brain.Blue, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
            Text("You ranked #${vm.rank}", color = Brain.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            Text("of ${8200 + vm.rank * 3} players today", color = Color(0xFF7B818A), fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Share result", onShare, leadingShare = true)
        Spacer(Modifier.height(4.dp))
        TextButton("Back to games", onBack)
    }
}

@Composable
private fun LoseModal(scoreText: String, onRetry: () -> Unit, onBack: () -> Unit) {
    ModalCard {
        Text("No moves left", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Brain.Ink)
        Text(scoreText, color = Brain.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Try again", onRetry)
        Spacer(Modifier.height(4.dp))
        TextButton("Back to games", onBack)
    }
}

@Composable
private fun ShareSheet(vm: BaseGameVM<*>, shareGrid: @Composable () -> Unit, onShare: () -> Unit, onCopy: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible = true, enter = slideInVertically { it }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brain.Card, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clickable(enabled = false) {}
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(18.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 40.dp, height = 4.dp).background(Color(0xFFDADDE2), RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(14.dp))
                Text("Share your result", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Brain.Ink, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Column(
                    Modifier.fillMaxWidth().padding(top = 16.dp).background(Brain.Blue, RoundedCornerShape(18.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("BRAIN GAMES", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(vm.game.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, modifier = Modifier.padding(top = 6.dp))
                    Text(dateLabel(), color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Box(Modifier.padding(vertical = 14.dp)) { shareGrid() }
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ShareStat(formatTime(vm.elapsed), "time")
                        ShareStat("${maxOf(vm.stats().streak, 1)}", "streak")
                        ShareStat("#${vm.rank}", "rank")
                    }
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Share", onShare, leadingShare = true)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().background(Brain.Chip, RoundedCornerShape(14.dp)).clickable(onClick = onCopy).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text("Copy text", color = Brain.InkSoft, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
            }
        }
    }
}

// ---- small pieces ----

@Composable
private fun ModalCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .background(Brain.Card, RoundedCornerShape(24.dp))
            .clickable(enabled = false) {}
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

@Composable
private fun StatBox(modifier: Modifier, big: String, small: String) {
    Column(
        modifier.background(Color(0xFFF5F6F8), RoundedCornerShape(14.dp)).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(big, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, color = Brain.Ink)
        Text(small, color = Brain.Muted, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ShareStat(big: String, small: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
        Text(small, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit, leadingShare: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brain.Blue, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingShare) {
            Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun TextButton(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = Color(0xFF8A9098), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

private fun dateLabel(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

private fun shareText(vm: BaseGameVM<*>): String {
    val streak = maxOf(vm.stats().streak, 1)
    return "Brain Games · ${vm.game.title}\n" +
        "Solved today in ${formatTime(vm.elapsed)}  ·  $streak day streak  ·  ranked #${vm.rank}\n" +
        "Play free in the app and try to beat me!"
}

private fun shareResult(context: Context, vm: BaseGameVM<*>) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText(vm))
        putExtra(Intent.EXTRA_TITLE, "Brain Games")
    }
    context.startActivity(Intent.createChooser(intent, "Share your result"))
}

private fun copyResult(context: Context, vm: BaseGameVM<*>) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Brain Games", shareText(vm)))
}
