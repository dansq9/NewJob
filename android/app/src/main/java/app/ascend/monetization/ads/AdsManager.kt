package app.ascend.monetization.ads

import app.ascend.monetization.Placement
import kotlinx.coroutines.flow.Flow

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

    /**
     * Show an interstitial for the given canonical [Placement] (snake_case `placement_id`,
     * rule 9). The real AdMob impl selects the correct ad unit per placement — e.g.
     * `ad_inter_after_splash` vs `ad_inter_after_onboarding_complete` map to distinct
     * units. Format-specific: NEVER route App Open or rewarded through this path
     * (they have showAppOpen()/showRewarded()), and never route native here (rule 10).
     */
    suspend fun showInterstitial(placement: Placement)

    /**
     * Whether a non-expired interstitial is preloaded and ready to show now. Used by
     * the splash interstitial to fail open (continue) when no ad is ready within the
     * load timeout — never block the user. The real impl preloads in the background.
     */
    fun isInterstitialReady(): Boolean

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
