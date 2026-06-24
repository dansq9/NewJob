package app.ascend.monetization.ads

import app.ascend.BuildConfig
import app.ascend.monetization.AdFormat
import app.ascend.monetization.Placement

/**
 * Maps a canonical [Placement.id] (snake_case, the SAME string used in Remote Config and
 * analytics — CLAUDE.md rule 9) to its AdMob ad-unit id.
 *
 * - **Debug builds** return Google's public TEST ad units, so Android Studio always shows
 *   safe test creatives and never a real production ad (acceptance: "No real production ads
 *   in debug").
 * - **Release builds** return the real ad-unit id wired by the human engineer. Until then
 *   the value is BLANK, which every load path treats as fail-open (no request, collapse /
 *   continue) — we never invent a real unit id (CLAUDE.md "DO NOT TOUCH … unit IDs are TBD").
 *
 * The same unit may legitimately back several placements while real IDs are pending; that is
 * a configuration detail, not policy. Placement *behavior* stays owned by MonetizationManager.
 */
object AdUnitIds {

    /** Google's official sample/test ad units (https://developers.google.com/admob/android/test-ads). */
    object Test {
        const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
        const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    }

    /**
     * Real ad-unit id for [placement] in a release build, or "" when not yet configured.
     *
     * TODO(AdMob): wire the real per-placement unit IDs here (or source them from a config
     * the human controls). Leaving an entry blank is intentional — that placement fails open.
     */
    private fun releaseUnit(placement: Placement): String = when (placement.format) {
        // No real units configured yet → blank → fail open. Do NOT invent ids.
        else -> ""
    }

    /** The test unit for a format (debug). */
    private fun testUnit(placement: Placement): String = when (placement.format) {
        AdFormat.INTERSTITIAL -> Test.INTERSTITIAL
        AdFormat.REWARDED -> Test.REWARDED
        AdFormat.APP_OPEN -> Test.APP_OPEN
        AdFormat.NATIVE -> Test.NATIVE
    }

    /** Ad-unit id for [placement]; "" means "not configured → fail open" (never request). */
    fun unitFor(placement: Placement): String =
        if (BuildConfig.DEBUG) testUnit(placement) else releaseUnit(placement)

    /** Convenience for the native Compose slot, which works from a [Placement]. */
    fun nativeUnitFor(placement: Placement): String = unitFor(placement)

    /**
     * Rewarded ad unit. The rewarded show path only knows a coarse [RewardedFeature] (not a
     * canonical placement), so all rewarded placements share one unit: the test unit in debug,
     * blank in release (→ no ad, no reward) until a real unit is wired.
     */
    fun rewardedUnit(): String = if (BuildConfig.DEBUG) Test.REWARDED else ""
}
