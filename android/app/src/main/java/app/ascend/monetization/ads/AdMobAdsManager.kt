package app.ascend.monetization.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import app.ascend.BuildConfig
import app.ascend.analytics.AdPrecision
import app.ascend.data.billing.EntitlementRepository
import app.ascend.monetization.AdFormat
import app.ascend.monetization.AdPaidEvent
import app.ascend.monetization.Placement
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Real Google Mobile Ads (AdMob) implementation of [AdsManager]. It owns ONLY SDK
 * load/show plumbing and ILRD forwarding — every eligibility/cap/suppression/mutex decision
 * still lives in [app.ascend.monetization.MonetizationManager] (CLAUDE.md rule 2). Debug
 * builds use Google TEST ad units ([AdUnitIds]); release uses the human-configured units and
 * fails open on any blank.
 *
 * Fail-open everywhere: a missing Activity, blank unit id, no-fill, timeout, or SDK error
 * never throws and never blocks — the manager's callers continue (rule 4).
 */
@Singleton
class AdMobAdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entitlements: EntitlementRepository,
) : AdsManager {

    override val adsEnabled: Flow<Boolean> = entitlements.isPro.map { !it }

    private val initialized = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    // The single ILRD paid-event sink (MonetizationManager.onAdPaid). Attached to EVERY ad.
    @Volatile private var paidListener: ((AdPaidEvent) -> Unit)? = null

    // Preloaded interstitials keyed by ad-unit id (placements can map to different units).
    private val interstitials = ConcurrentHashMap<String, InterstitialAd>()

    @Volatile private var appOpenAd: AppOpenAd? = null
    @Volatile private var appOpenLoadedAtMs: Long = 0L
    @Volatile private var appOpenLoading = false

    override fun setPaidListener(listener: (AdPaidEvent) -> Unit) { paidListener = listener }

    override fun initialize() {
        if (!initialized.compareAndSet(false, true)) return   // idempotent
        onMain {
            // A bad SDK state must never crash the app at launch — fail open (no ads).
            runCatching {
                if (BuildConfig.DEBUG) {
                    // Emulators are auto-test devices; this also marks physical debug devices as test.
                    MobileAds.setRequestConfiguration(
                        RequestConfiguration.Builder()
                            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                            .build(),
                    )
                }
                MobileAds.initialize(context) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "MobileAds initialized")
                    // Preload the readiness-poll formats so splash/onboarding/app-open show promptly.
                    preloadInterstitial(Placement.INTER_AFTER_SPLASH)
                    preloadInterstitial(Placement.INTER_AFTER_ONBOARDING_COMPLETE)
                    preloadAppOpen()
                }
            }.onFailure { if (BuildConfig.DEBUG) Log.w(TAG, "MobileAds init failed; ads disabled", it) }
        }
    }

    // ---- Interstitial ----

    override suspend fun showInterstitial(placement: Placement) {
        require(placement.format == AdFormat.INTERSTITIAL) {
            "showInterstitial requires an INTERSTITIAL placement, got ${placement.format} ($placement)"
        }
        if (!initialized.get() || entitlements.isPro.first()) return
        val unit = AdUnitIds.unitFor(placement)
        if (unit.isBlank()) return                              // not configured → fail open
        withContext(Dispatchers.Main) {
            val activity = CurrentActivityHolder.current
            val ad = interstitials.remove(unit)
            if (activity == null || ad == null) {
                preloadInterstitial(placement)                  // not ready → fail open; ready next time
                return@withContext
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { preloadInterstitial(placement) }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { preloadInterstitial(placement) }
            }
            runCatching { ad.show(activity) }
        }
    }

    override fun isInterstitialReady(): Boolean = interstitials.isNotEmpty()

    private fun preloadInterstitial(placement: Placement) {
        if (!initialized.get()) return
        val unit = AdUnitIds.unitFor(placement)
        if (unit.isBlank() || interstitials.containsKey(unit)) return
        onMain {
            runCatching {
                InterstitialAd.load(
                    context, unit, AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            ad.onPaidEventListener = OnPaidEventListener { v -> forwardPaid(placement, ad.adUnitId, v, ad) }
                            interstitials[unit] = ad
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) { interstitials.remove(unit) }
                    },
                )
            }
        }
    }

    // ---- App Open ----

    override fun isAppOpenAdAvailable(): Boolean {
        val ad = appOpenAd
        if (ad == null) { preloadAppOpen(); return false }
        // AdMob app-open ads expire ~4h after load.
        if (SystemClock.elapsedRealtime() - appOpenLoadedAtMs > APP_OPEN_TTL_MS) {
            appOpenAd = null; preloadAppOpen(); return false
        }
        return true
    }

    override suspend fun showAppOpen() {
        if (!initialized.get() || entitlements.isPro.first()) return
        withContext(Dispatchers.Main) {
            val activity = CurrentActivityHolder.current
            val ad = appOpenAd
            if (activity == null || ad == null) return@withContext
            appOpenAd = null
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { preloadAppOpen() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { preloadAppOpen() }
            }
            runCatching { ad.show(activity) }
        }
    }

    private fun preloadAppOpen() {
        if (!initialized.get() || appOpenAd != null || appOpenLoading) return
        val unit = AdUnitIds.unitFor(Placement.APPOPEN_RESUME)
        if (unit.isBlank()) return
        appOpenLoading = true
        onMain {
            runCatching {
            AppOpenAd.load(
                context, unit, AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        ad.onPaidEventListener = OnPaidEventListener { v -> forwardPaid(Placement.APPOPEN_RESUME, ad.adUnitId, v, ad) }
                        appOpenAd = ad
                        appOpenLoadedAtMs = SystemClock.elapsedRealtime()
                        appOpenLoading = false
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) { appOpenAd = null; appOpenLoading = false }
                },
            )
            }
        }
    }

    // ---- Rewarded ----

    override suspend fun showRewarded(feature: RewardedFeature): Boolean {
        if (entitlements.isPro.first()) return true             // Pro bypasses ads (shouldn't reach)
        if (!initialized.get()) return false
        val unit = AdUnitIds.rewardedUnit()
        if (unit.isBlank()) return false                        // not configured → no ad, no reward
        val placement = feature.toPlacement()
        return withContext(Dispatchers.Main) {
            val activity = CurrentActivityHolder.current ?: return@withContext false
            val ad = loadRewarded(unit, placement) ?: return@withContext false   // no-fill/timeout → false
            showRewardedAndAwait(ad, activity)
        }
    }

    /** Loads a rewarded ad, bounded by a short LOAD timeout so no-fill fails open fast. */
    private suspend fun loadRewarded(unit: String, placement: Placement): RewardedAd? =
        withTimeoutOrNull(REWARDED_LOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                RewardedAd.load(
                    context, unit, AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            ad.onPaidEventListener = OnPaidEventListener { v -> forwardPaid(placement, ad.adUnitId, v, ad) }
                            if (cont.isActive) cont.resume(ad)
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) { if (cont.isActive) cont.resume(null) }
                    },
                )
            }
        }

    /** Shows the rewarded ad and resolves to true ONLY if the earned-reward callback fired (rule 5). */
    private suspend fun showRewardedAndAwait(ad: RewardedAd, activity: Activity): Boolean =
        suspendCancellableCoroutine { cont ->
            var earned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { if (cont.isActive) cont.resume(earned) }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) { if (cont.isActive) cont.resume(false) }
            }
            ad.show(activity, OnUserEarnedRewardListener { earned = true })
        }

    private fun RewardedFeature.toPlacement(): Placement = when (this) {
        RewardedFeature.MOCK_INTERVIEW -> Placement.REWARDED_MOCK_START
        RewardedFeature.RESUME_GENERATE -> Placement.REWARDED_RESUME_DOWNLOAD
        RewardedFeature.RESUME_OPTIMIZE -> Placement.REWARDED_RESUME_OPTIMIZE
    }

    // ---- ILRD ----

    /** Builds one [AdPaidEvent] from an AdMob paid callback and forwards it (→ one ad_impression). */
    private fun forwardPaid(placement: Placement, adUnitId: String?, adValue: AdValue, ad: Any) {
        val adSource = when (ad) {
            is InterstitialAd -> ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName
            is RewardedAd -> ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName
            is AppOpenAd -> ad.responseInfo?.loadedAdapterResponseInfo?.adSourceName
            else -> null
        }
        paidListener?.invoke(
            AdPaidEvent(
                placementId = placement.id,
                format = placement.format,
                valueMicros = adValue.valueMicros,
                currencyCode = adValue.currencyCode,
                precision = AdPrecision.fromAdMob(adValue.precisionType),
                adSource = adSource,
                adUnitId = adUnitId,
            ),
        )
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private companion object {
        const val TAG = "AdMobAdsManager"
        const val APP_OPEN_TTL_MS = 4L * 60 * 60 * 1000   // 4 hours
        const val REWARDED_LOAD_TIMEOUT_MS = 10_000L      // load only; the watch is bounded by the manager
    }
}
