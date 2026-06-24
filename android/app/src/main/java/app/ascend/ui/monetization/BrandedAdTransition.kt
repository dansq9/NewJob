package app.ascend.ui.monetization

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.R
import app.ascend.ui.theme.AscendColors

/**
 * Branded pre-ad transition shown over the app while a full-screen interstitial is about
 * to display — the splash/session-start interstitial OR the onboarding-complete interstitial
 * (Apero-style). This is a NEUTRAL branded loading surface — no tap prompt, no reward
 * language, no ad-like UI (QA IA11). Its visibility + duration are driven entirely by
 * [MonetizationManager] (brandedAdTransition); this is only the surface.
 */
@Composable
fun BrandedAdTransition(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "splashAlpha",
    )
    Box(modifier.fillMaxSize().background(AscendColors.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(AscendColors.ChipIndigo).alpha(alpha),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.app_name).take(1), color = AscendColors.Indigo, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
            }
            Box(Modifier.padding(top = 24.dp)) {
                CircularProgressIndicator(color = AscendColors.Indigo, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            }
        }
    }
}
