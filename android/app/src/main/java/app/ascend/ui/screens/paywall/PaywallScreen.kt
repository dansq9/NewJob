package app.ascend.ui.screens.paywall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ascend.R
import app.ascend.ui.theme.AscendColors

// Premium vs Basic; basic cell: YES (free), ADS (free with rewarded ad), NO (Pro only)
private enum class Basic { YES, ADS, NO }
private data class CompareRow(val icon: ImageVector, @StringRes val label: Int, val basic: Basic)

private val COMPARE = listOf(
    CompareRow(Icons.Outlined.Work, R.string.paywall_feature_find_jobs, Basic.YES),
    CompareRow(Icons.Outlined.AutoFixHigh, R.string.paywall_feature_resume, Basic.ADS),
    CompareRow(Icons.Outlined.RecordVoiceOver, R.string.paywall_feature_mock, Basic.ADS),
    CompareRow(Icons.Outlined.Bolt, R.string.paywall_feature_navigator, Basic.NO),
    CompareRow(Icons.Outlined.Block, R.string.paywall_feature_ad_free, Basic.NO),
    CompareRow(Icons.Outlined.SupportAgent, R.string.paywall_feature_support, Basic.NO),
)

// TODO(legal): replace with the real hosted policy URLs.
private const val TERMS_URL = "https://ascend.app/terms"
private const val PRIVACY_URL = "https://ascend.app/privacy"

@Composable
fun PaywallScreen(onClose: () -> Unit, vm: PaywallViewModel = hiltViewModel()) {
    val isPro by vm.isPro.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    LaunchedEffect(isPro) { if (isPro) onClose() }
    val primary = vm.plans.firstOrNull()
    val priceLine = primary?.let { stringResource(R.string.paywall_price_line, it.price, it.period) }
        ?: stringResource(R.string.paywall_price_fallback)

    Column(Modifier.fillMaxSize().background(AscendColors.Card).statusBarsPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // header
            Box(Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 18.dp)) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).size(34.dp)) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.paywall_close), tint = AscendColors.Ink2)
                }
                Column(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.WorkspacePremium, null, tint = AscendColors.Indigo, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.paywall_headline), fontSize = 27.sp, fontWeight = FontWeight.ExtraBold,
                        color = AscendColors.Ink, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.paywall_subhead), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Muted)
                }
            }
            // comparison table
            Column(Modifier.padding(horizontal = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.paywall_col_premium), Modifier.width(70.dp), color = AscendColors.Indigo, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.paywall_col_basic), Modifier.width(64.dp), color = AscendColors.Muted2, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                COMPARE.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(row.icon, null, tint = AscendColors.Indigo, modifier = Modifier.size(23.dp))
                            Spacer(Modifier.width(13.dp))
                            Text(stringResource(row.label), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AscendColors.Ink2)
                        }
                        Box(Modifier.width(70.dp), Alignment.Center) {
                            Icon(Icons.Filled.Check, null, tint = AscendColors.Indigo, modifier = Modifier.size(22.dp))
                        }
                        Box(Modifier.width(64.dp), Alignment.Center) { BasicCell(row.basic) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        // bottom: price + CTA
        Column(Modifier.padding(22.dp).navigationBarsPadding()) {
            Text(priceLine, Modifier.fillMaxWidth(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                color = AscendColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { vm.subscribe(onClose) },
                enabled = !vm.busy && primary != null,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AscendColors.Indigo),
            ) {
                if (vm.busy) CircularProgressIndicator(Modifier.size(24.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                else Text(stringResource(R.string.paywall_start_trial), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
            Text(
                stringResource(R.string.paywall_billing_disclosure),
                Modifier.fillMaxWidth().padding(top = 14.dp), fontSize = 13.sp, color = AscendColors.Muted2,
                textAlign = TextAlign.Center, lineHeight = 18.sp,
            )
            vm.message?.let { Text(stringResource(it), Modifier.fillMaxWidth().padding(top = 8.dp), color = AscendColors.Indigo, fontSize = 13.sp, textAlign = TextAlign.Center) }
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center) {
                LinkText(stringResource(R.string.paywall_restore)) { vm.restore() }
                Dot(); LinkText(stringResource(R.string.paywall_terms)) { runCatching { uri.openUri(TERMS_URL) } }
                Dot(); LinkText(stringResource(R.string.paywall_privacy)) { runCatching { uri.openUri(PRIVACY_URL) } }
            }
        }
    }
}

@Composable
private fun BasicCell(basic: Basic) = when (basic) {
    Basic.YES -> Icon(Icons.Filled.Check, null, tint = AscendColors.Muted2, modifier = Modifier.size(22.dp))
    Basic.NO -> Text("—", color = AscendColors.Faint, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Basic.ADS -> Surface(shape = RoundedCornerShape(6.dp), color = Color_FBF4D9) {
        Text(stringResource(R.string.paywall_basic_ads), Modifier.padding(horizontal = 7.dp, vertical = 2.dp), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color_B0A06A)
    }
}

private val Color_FBF4D9 = androidx.compose.ui.graphics.Color(0xFFFBF4D9)
private val Color_B0A06A = androidx.compose.ui.graphics.Color(0xFFB0A06A)

@Composable
private fun LinkText(text: String, onClick: () -> Unit) =
    Text(text, Modifier.clickable(onClick = onClick), color = AscendColors.Faint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

@Composable
private fun Dot() = Text("  ·  ", color = AscendColors.Faint, fontSize = 12.sp)
