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

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            // TODO(AdMob): MobileAds.initialize(context) here, once the AdMob App ID exists.
            if (BuildConfig.DEBUG) Log.d("NoopAdsManager", "ad SDK initialize() — consent gate open")
        }
    }

    override suspend fun showInterstitial(placement: AdPlacement) {
        if (!initialized.get()) return            // consent gate not open yet — never request
        if (entitlements.isPro.first()) return
        // TODO(AdMob): load + show InterstitialAd for [placement].
    }

    override suspend fun showRewarded(feature: RewardedFeature): Boolean {
        if (entitlements.isPro.first()) return true   // Pro bypasses ads entirely
        if (!initialized.get()) return false      // consent gate not open — no ad, no reward
        // TODO(AdMob): show RewardedAd; return whether the reward callback fired.
        return true
    }
}
