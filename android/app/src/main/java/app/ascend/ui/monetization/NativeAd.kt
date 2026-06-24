package app.ascend.ui.monetization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ascend.R
import app.ascend.monetization.AdDecision
import app.ascend.monetization.MonetizationManager
import app.ascend.monetization.Placement
import kotlinx.coroutines.flow.Flow
import app.ascend.ui.theme.AscendColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Inline native ad slot for [placement]. Renders the (non-blocking) ad card ONLY
 * when [MonetizationManager] decides to show it; otherwise it COLLAPSES — renders
 * nothing, never a blank container (CLAUDE.md rule 4 / spec "no-fill: collapse").
 *
 * The decision (Pro → hidden, UMP consent gate, RC enable key) all lives in the
 * manager (rule 2); this composable just observes it. Starts collapsed so nothing
 * flashes before the gate resolves.
 */
@Composable
fun NativeAdSlot(placement: Placement, modifier: Modifier = Modifier) {
    val manager = rememberMonetizationManager()
    // Typed nullable so the initial (pre-gate) value can be null → collapsed.
    val flow: Flow<AdDecision?> = remember(placement) { manager.nativeAd(placement) }
    val decision by flow.collectAsStateWithLifecycle(initialValue = null)
    if (decision is AdDecision.Show) {
        NativeAdCard(modifier)
    }
}

/** The placeholder native creative (the real NativeAd view renders here once AdMob lands). */
@Composable
private fun NativeAdCard(modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AscendColors.Card,
        border = BorderStroke(1.5.dp, AscendColors.Line),
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(AscendColors.ChipIndigo), Alignment.Center) {
                Icon(Icons.Outlined.Campaign, null, tint = AscendColors.Indigo)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.jobs_ad_sponsored), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AscendColors.Ink)
                Text(stringResource(R.string.jobs_ad_placeholder), fontSize = 13.sp, color = AscendColors.Muted)
            }
            Text(
                stringResource(R.string.jobs_ad_badge), color = Color(0xFFB0A06A), fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFBF4D9)).padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

/** Hilt entry point so non-VM composables can reach the singleton ad authority. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MonetizationEntryPoint {
    fun monetizationManager(): MonetizationManager
}

@Composable
fun rememberMonetizationManager(): MonetizationManager {
    val appContext = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(appContext, MonetizationEntryPoint::class.java).monetizationManager()
    }
}
