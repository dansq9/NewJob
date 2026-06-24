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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ascend.R
import app.ascend.ui.theme.AscendColors

/**
 * Branded pre-ad transition shown over the app while a full-screen interstitial is about to
 * display — the splash/session-start interstitial OR the onboarding-complete interstitial
 * (Apero-style). Mirrors the prototype splash: indigo→violet gradient, soft blurred glow
 * circles, the Ascend logo mark, and HONEST loading copy only — no tap prompt, no reward
 * language, no fake CTA (QA IA11). Visibility + duration are driven entirely by
 * [MonetizationManager] (brandedAdTransition); this is only the surface.
 */
@Composable
fun BrandedAdTransition(modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val markAlpha by pulse.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "splashMarkAlpha",
    )
    Box(
        modifier.fillMaxSize().background(
            Brush.linearGradient(
                0f to Color(0xFF4F46E5), 0.55f to Color(0xFF6D5CF0), 1f to Color(0xFF8B5CF6),
            ),
        ),
        contentAlignment = Alignment.Center,
    ) {
        // Soft blurred glow circles (no-op blur below API 31 — still renders as translucent discs).
        Box(
            Modifier.size(260.dp).offset(x = (-120).dp, y = (-160).dp)
                .blur(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
        )
        Box(
            Modifier.size(220.dp).offset(x = 130.dp, y = 200.dp)
                .blur(60.dp).clip(CircleShape).background(Color(0xFFB7A6FF).copy(alpha = 0.25f)),
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Logo mark — white rounded square with the brand initial.
            Box(
                Modifier.size(82.dp).alpha(markAlpha).clip(RoundedCornerShape(24.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.app_name).take(1),
                    color = AscendColors.Indigo, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                stringResource(R.string.splash_brand_tagline),
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.splash_brand_loading),
                color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, textAlign = TextAlign.Center,
            )
        }
    }
}
