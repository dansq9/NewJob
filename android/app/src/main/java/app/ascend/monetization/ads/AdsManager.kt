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

    /**
     * Initialize the ad SDK. MUST only be called once the UMP gate is open
     * (ConsentManager.canRequestAds == true) — see CLAUDE.md rule 1. Idempotent.
     * No ad request may fire until this has run.
     */
    fun initialize()

    /**
     * Register the single ILRD paid-event sink (MonetizationManager.onAdPaid).
     * The real impl MUST attach `setOnPaidEventListener` to EVERY loaded ad —
     * native, interstitial, rewarded, app-open — and forward each [AdPaidEvent]
     * here so exactly one `ad_impression` is logged per impression (rule 7).
     */
    fun setPaidListener(listener: (app.ascend.monetization.AdPaidEvent) -> Unit)

    suspend fun showInterstitial(placement: AdPlacement = AdPlacement.SPLASH_INTERSTITIAL)

    /**
     * Whether a non-expired app-open ad is preloaded and ready to show right now.
     * If false, the caller must continue immediately — app-open never blocks resume
     * (CLAUDE.md rule 4, fail open). The real impl preloads in the background and
     * honors `ad_expiration_hours`.
     */
    fun isAppOpenAdAvailable(): Boolean

    /** Show the preloaded app-open ad. Only called when [isAppOpenAdAvailable] is true. */
    suspend fun showAppOpen()

    /** Show a rewarded ad to unlock one use of [feature]; true if reward granted
     *  (always true for Pro, who bypass the ad). */
    suspend fun showRewarded(feature: RewardedFeature): Boolean
}
