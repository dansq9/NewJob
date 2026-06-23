package app.ascend.monetization.ads

import app.ascend.data.billing.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    override suspend fun showInterstitial(placement: AdPlacement) {
        if (entitlements.isPro.first()) return
        // TODO(AdMob): load + show InterstitialAd for [placement].
    }

    override suspend fun showRewarded(feature: RewardedFeature): Boolean {
        if (entitlements.isPro.first()) return true
        // TODO(AdMob): show RewardedAd; return whether the reward callback fired.
        return true
    }
}
