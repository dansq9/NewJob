package app.ascend.monetization.ads

import kotlinx.coroutines.flow.Flow

/** Ad slots in the product (per the monetization plan). */
enum class AdPlacement {
    SPLASH_INTERSTITIAL,        // interstitial after splash
    LOCALIZATION_NATIVE,        // native ad on the language screen
    RESUME_ONBOARDING_NATIVE,   // native ad on resume optimize during onboarding
    JOB_LIST_NATIVE,            // native ad every 5 job cards
}

/** AI features that are free-with-rewarded-ad (and ad-free for Pro). */
enum class RewardedFeature { RESUME_OPTIMIZE, MOCK_INTERVIEW, RESUME_GENERATE }

/**
 * Abstraction over the ad SDK (AdMob / Google Mobile Ads). [NoopAdsManager] is
 * wired until the AdMob App ID + ad-unit IDs are configured; swap the Hilt
 * binding for the real implementation then. Pro users never see ads.
 */
interface AdsManager {
    /** false when the user is Pro — UIs hide native ad slots accordingly. */
    val adsEnabled: Flow<Boolean>

    suspend fun showInterstitial(placement: AdPlacement = AdPlacement.SPLASH_INTERSTITIAL)

    /** Show a rewarded ad to unlock one use of [feature]; true if reward granted
     *  (always true for Pro, who bypass the ad). */
    suspend fun showRewarded(feature: RewardedFeature): Boolean
}
