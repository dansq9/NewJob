package app.ascend.monetization.ads

import app.ascend.BuildConfig
import android.util.Log
import app.ascend.data.billing.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No-op ads. Respects Pro (no ads), and grants rewarded actions immediately so
 * the flows work before AdMob is integrated. Replace the Hilt binding with the
 * real Google Mobile Ads implementation once ad-unit IDs exist.
 */
@Singleton
class NoopAdsManager @Inject constructor(
    private val entitlements: EntitlementRepository,
) : AdsManager {

    override val adsEnabled: Flow<Boolean> = entitlements.isPro.map { !it }

    // Gate: no ad request runs until initialize() has been called (post-consent).
    private val initialized = AtomicBoolean(false)

    // ILRD paid-event sink (MonetizationManager.onAdPaid). The real impl attaches an
    // OnPaidEventListener to every loaded ad and invokes this; the Noop has no real
    // impressions so it never fires.
    @Suppress("unused")
    private var paidListener: ((app.ascend.monetization.AdPaidEvent) -> Unit)? = null

    override fun setPaidListener(listener: (app.ascend.monetization.AdPaidEvent) -> Unit) {
        paidListener = listener
    }

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            // TODO(AdMob): MobileAds.initialize(context) here, once the AdMob App ID exists.
            if (BuildConfig.DEBUG) Log.d("NoopAdsManager", "ad SDK initialize() — consent gate open")
        }
    }

    // TODO(AdMob) — attach the SAME ILRD listener to EVERY format when the SDK lands.
    // Each loaded ad calls back per paid impression; forward it verbatim:
    //
    //   ad.setOnPaidEventListener { adValue ->
    //       paidListener?.invoke(AdPaidEvent(
    //           placementId = placement.id,
    //           format = placement.format,
    //           valueMicros = adValue.valueMicros,
    //           currencyCode = adValue.currencyCode,
    //           precision = AdPrecision.fromAdMob(adValue.precisionType),
    //           adSource = ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName,
    //           adUnitId = ad.adUnitId,
    //       ))
    //   }
    //
    // Applies identically to NativeAd, InterstitialAd, RewardedAd, and AppOpenAd.

    override suspend fun showInterstitial(placement: AdPlacement) {
        if (!initialized.get()) return            // consent gate not open yet — never request
        if (entitlements.isPro.first()) return
        // TODO(AdMob): load + show InterstitialAd for [placement].
    }

    // No real preload in the Noop → never ready → splash/app-open always fail open (continue).
    override fun isInterstitialReady(): Boolean = false
    override fun isAppOpenAdAvailable(): Boolean = false

    override suspend fun showAppOpen() {
        // TODO(AdMob): show the preloaded AppOpenAd here (only reached once a real ad is ready).
    }

    override suspend fun showRewarded(feature: RewardedFeature): Boolean {
        if (entitlements.isPro.first()) return true   // Pro bypasses ads entirely
        if (!initialized.get()) return false      // consent gate not open — no ad, no reward
        // TODO(AdMob): show RewardedAd; return whether the reward callback fired.
        return true
    }
}
