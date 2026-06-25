package app.ascend.ui.monetization

import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ascend.BuildConfig
import app.ascend.R
import app.ascend.analytics.AdPrecision
import app.ascend.monetization.AdDecision
import app.ascend.monetization.AdPaidEvent
import app.ascend.monetization.MonetizationManager
import app.ascend.monetization.Placement
import app.ascend.monetization.ads.AdUnitIds
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.flow.Flow
import app.ascend.ui.theme.AscendColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Inline native ad slot for [placement]. Renders the (non-blocking) ad ONLY when
 * [MonetizationManager] decides to show it; otherwise it COLLAPSES — renders nothing,
 * never a blank container (CLAUDE.md rule 4 / spec "no-fill: collapse").
 *
 * The decision (Pro → hidden, UMP consent gate, RC enable key) all lives in the manager
 * (rule 2); this composable just observes it and, on Show, loads & renders a real AdMob
 * native ad (Google TEST creative in debug). If real ads are disabled for the build
 * ([BuildConfig.USE_REAL_ADS] == false) it shows a clearly-marked debug placeholder.
 */
@Composable
fun NativeAdSlot(placement: Placement, modifier: Modifier = Modifier) {
    val manager = rememberMonetizationManager()
    // Typed nullable so the initial (pre-gate) value can be null → collapsed.
    val flow: Flow<AdDecision?> = remember(placement) { manager.nativeAd(placement) }
    val decision by flow.collectAsStateWithLifecycle(initialValue = null)
    if (decision is AdDecision.Show) {
        if (BuildConfig.USE_REAL_ADS) RealNativeAd(placement, manager, modifier)
        else DebugNativePlaceholder(modifier)
    }
}

/**
 * Loads and renders a real AdMob native ad. Collapses (renders nothing) on no-fill, error,
 * or a blank/unconfigured unit (fail open). Attaches the ILRD paid listener → one ad_impression.
 */
@Composable
private fun RealNativeAd(placement: Placement, manager: MonetizationManager, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val unit = remember(placement) { AdUnitIds.nativeUnitFor(placement) }
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(unit) {
        if (unit.isBlank()) {
            onDispose { }   // not configured → fail open, render nothing
        } else {
            // Wrapped: a misbehaving ad SDK / loader must never crash the host screen.
            runCatching {
                val loader = AdLoader.Builder(context, unit)
                    .forNativeAd { ad ->
                        ad.setOnPaidEventListener(
                            OnPaidEventListener { v ->
                                manager.onAdPaid(
                                    AdPaidEvent(
                                        placementId = placement.id,
                                        format = placement.format,
                                        valueMicros = v.valueMicros,
                                        currencyCode = v.currencyCode,
                                        precision = AdPrecision.fromAdMob(v.precisionType),
                                        adSource = ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName,
                                        adUnitId = unit,
                                    ),
                                )
                            },
                        )
                        nativeAd = ad
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) { nativeAd = null }
                    })
                    .build()
                loader.loadAd(AdRequest.Builder().build())
            }
            onDispose { runCatching { nativeAd?.destroy() } }
        }
    }

    nativeAd?.let { ad ->
        Surface(
            modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = AscendColors.Card,
            border = BorderStroke(1.5.dp, AscendColors.Line),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx -> runCatching { buildNativeAdView(ctx) }.getOrElse { NativeAdView(ctx) } },
                update = { view -> bindNativeAd(view, ad) },
            )
        }
    }
}

/** Builds the NativeAdView hierarchy once; AdMob injects the AdChoices overlay automatically. */
private fun buildNativeAdView(ctx: android.content.Context): NativeAdView {
    val d = ctx.resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    val adView = NativeAdView(ctx)

    val root = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(15), dp(15), dp(15), dp(15))
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    val row = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val icon = ImageView(ctx).apply { layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)) }
    val textCol = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
            it.marginStart = dp(12); it.marginEnd = dp(8)
        }
    }
    val headline = TextView(ctx).apply {
        setTextColor(0xFF15151C.toInt()); textSize = 15f
        setTypeface(typeface, Typeface.BOLD); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    val body = TextView(ctx).apply {
        setTextColor(0xFF6B6B78.toInt()); textSize = 13f; maxLines = 2; ellipsize = TextUtils.TruncateAt.END
    }
    val badge = TextView(ctx).apply {
        text = ctx.getString(R.string.jobs_ad_badge)
        setTextColor(0xFFB0A06A.toInt()); textSize = 9f
        setPadding(dp(7), dp(2), dp(7), dp(2))
    }
    val cta = Button(ctx).apply {
        isAllCaps = false; textSize = 13f
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .also { it.topMargin = dp(12) }
    }
    textCol.addView(headline); textCol.addView(body)
    row.addView(icon); row.addView(textCol); row.addView(badge)
    root.addView(row); root.addView(cta)
    adView.addView(root)

    adView.headlineView = headline
    adView.bodyView = body
    adView.iconView = icon
    adView.callToActionView = cta
    return adView
}

/**
 * Binds the loaded native ad's assets to the view, then registers it (required last step).
 * Wrapped defensively so a malformed asset can never crash the host screen — at worst the
 * slot stays blank (the manager already collapses suppressed slots).
 */
private fun bindNativeAd(adView: NativeAdView, ad: NativeAd) {
    runCatching {
        (adView.headlineView as? TextView)?.text = ad.headline
        (adView.bodyView as? TextView)?.let { tv ->
            tv.text = ad.body
            tv.visibility = if (ad.body.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        (adView.iconView as? ImageView)?.let { iv ->
            val icon = ad.icon
            if (icon?.drawable != null) { iv.setImageDrawable(icon.drawable); iv.visibility = View.VISIBLE }
            else iv.visibility = View.GONE
        }
        (adView.callToActionView as? Button)?.let { btn ->
            if (ad.callToAction.isNullOrBlank()) btn.visibility = View.GONE
            else { btn.text = ad.callToAction; btn.visibility = View.VISIBLE }
        }
        adView.setNativeAd(ad)
    }
}

/** DEBUG-ONLY placeholder (NOT a real ad) — shown only when USE_REAL_ADS is false. */
@Composable
private fun DebugNativePlaceholder(modifier: Modifier = Modifier) {
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
