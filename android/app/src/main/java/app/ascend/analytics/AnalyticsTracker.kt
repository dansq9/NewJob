package app.ascend.analytics

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import app.ascend.BuildConfig
import app.ascend.data.billing.EntitlementRepository
import app.ascend.data.local.ProfileRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The ONE analytics wrapper over Firebase/GA4 (CLAUDE.md rule 8). All events flow
 * through typed methods here using [Ev]/[Pr]/[UserProp] constants — no ad-hoc
 * strings in screens/VMs. PII is never logged: params are bands/booleans/enums,
 * never raw queries, resume text, names, emails, or filenames (rule 8).
 *
 * Backed by FirebaseAnalytics, which only sends once the google-services.json +
 * google-services plugin are added by the human engineer. Until then FirebaseApp
 * is absent and this degrades to Logcat (debug) — the call sites don't change.
 *
 * Note: implements `push_open` only. `push_sent` is a server-side event and is
 * deliberately NOT logged from the client (schema §7).
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profile: ProfileRepository,
    private val entitlements: EntitlementRepository,
) {
    private val fa: FirebaseAnalytics? by lazy {
        if (FirebaseApp.getApps(context).isNotEmpty()) FirebaseAnalytics.getInstance(context) else null
    }

    @Volatile private var sessionNumber: Int = 1

    // ---- Identity + session (call once per cold start) ----

    /** Increment session, set the stable identity + all user properties, fire session_start_enriched. */
    suspend fun startSession() {
        val installId = profile.installId()
        sessionNumber = profile.nextSessionNumber()
        val pro = entitlements.isPro.first()
        val source = entitlements.entitlement.first().source
        val activated = profile.activatedOnce()
        val segment = computeAdSegment(pro, sessionNumber, activated)

        fa?.setUserId(installId)
        setUserProp(UserProp.INSTALL_ID, installId)
        setUserProp(UserProp.ACCOUNT_STATUS, (if (pro) AccountStatus.PRO else AccountStatus.FREE).v)
        setUserProp(UserProp.PLAN, planOf(pro, source).v)
        setUserProp(UserProp.AD_SEGMENT, segment.v)
        setUserProp(UserProp.AGGRESSIVENESS_TIER, AggressivenessTier.BALANCED.v)   // Remote Config default
        setUserProp(UserProp.RC_VARIANT, "control")                                // Remote Config default
        setUserProp(UserProp.APP_VERSION, BuildConfig.VERSION_NAME)
        setUserProp(UserProp.OS_VERSION, "Android ${Build.VERSION.RELEASE}")
        setUserProp(UserProp.COUNTRY, country())

        log(Ev.SESSION_START_ENRICHED, Pr.SESSION_NUMBER to sessionNumber, Pr.USER_AD_SEGMENT to segment.v)
    }

    /** user_ad_segment (schema §7) from the signals available locally. */
    private fun computeAdSegment(pro: Boolean, session: Int, activated: Boolean): AdSegment = when {
        pro -> AdSegment.PAYER
        !activated && session <= 1 -> AdSegment.NEW
        activated -> AdSegment.ACTIVATED
        else -> AdSegment.AD_TOLERANT
    }

    private fun planOf(pro: Boolean, source: String?): Plan = when {
        !pro -> Plan.FREE
        source?.contains("yearly", true) == true -> Plan.YEARLY
        source?.contains("lifetime", true) == true -> Plan.LIFETIME
        else -> Plan.WEEKLY
    }

    private fun country(): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val iso = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().country
        return iso.uppercase(Locale.ROOT).take(36)
    }

    // ---- Activation / acquisition ----

    fun onboardingStep(step: OnboardingStep, skipped: Boolean) =
        log(Ev.ONBOARDING_STEP, Pr.STEP to step.v, Pr.SKIPPED to skipped)

    fun onboardingComplete(rolePresent: Boolean, roleCategory: RoleCategory, location: LocationType, resumeUploaded: Boolean) =
        log(
            Ev.ONBOARDING_COMPLETE,
            Pr.TARGET_ROLE_PRESENT to rolePresent, Pr.TARGET_ROLE_CATEGORY to roleCategory.v,
            Pr.LOCATION_TYPE to location.v, Pr.RESUME_UPLOADED to resumeUploaded,
        )

    /** Logs core_action_done and marks the user activated (segment promotion). */
    suspend fun coreActionDone(action: CoreAction) {
        profile.markActivated()
        log(Ev.CORE_ACTION_DONE, Pr.ACTION_TYPE to action.v, Pr.SESSION_NUMBER to sessionNumber)
    }

    // ---- Core job loop ----

    fun jobSearch(queryPresent: Boolean, filtersUsed: Boolean, resultsCount: Int, source: SearchSource) =
        log(Ev.JOB_SEARCH, Pr.QUERY_PRESENT to queryPresent, Pr.FILTERS_USED to filtersUsed,
            Pr.RESULTS_COUNT to resultsCount, Pr.SOURCE to source.v)

    fun jobDetailView(matchBand: Band?, employment: EmploymentType, remote: LocationType) =
        log(Ev.JOB_DETAIL_VIEW, Pr.MATCH_SCORE_BAND to matchBand?.v,
            Pr.EMPLOYMENT_TYPE to employment.v, Pr.REMOTE_TYPE to remote.v)

    fun jobSave(from: SaveFrom) = log(Ev.JOB_SAVE, Pr.FROM_SCREEN to from.v)
    fun jobApplyClick(type: ApplyType) = log(Ev.JOB_APPLY_CLICK, Pr.APPLY_TYPE to type.v)
    fun trackerStageChange(from: TrackerStage, to: TrackerStage) =
        log(Ev.TRACKER_STAGE_CHANGE, Pr.FROM_STAGE to from.v, Pr.TO_STAGE to to.v)

    // ---- Resume / interview ----

    fun resumeUpload(type: FileType, sizeBand: SizeBand) =
        log(Ev.RESUME_UPLOAD, Pr.FILE_TYPE to type.v, Pr.FILE_SIZE_BAND to sizeBand.v)
    fun resumeOptimizeStart(hasTargetJob: Boolean) = log(Ev.RESUME_OPTIMIZE_START, Pr.HAS_TARGET_JOB to hasTargetJob)
    fun resumeOptimizeComplete(scoreBand: Band, gatedBy: GatedBy) =
        log(Ev.RESUME_OPTIMIZE_COMPLETE, Pr.SCORE_BAND to scoreBand.v, Pr.GATED_BY to gatedBy.v)
    fun resumeDownload(format: FileType, gatedBy: GatedBy) =
        log(Ev.RESUME_DOWNLOAD, Pr.FORMAT to format.v, Pr.GATED_BY to gatedBy.v)
    fun mockInterviewStart(gatedBy: GatedBy, roleSource: RoleSource) =
        log(Ev.MOCK_INTERVIEW_START, Pr.GATED_BY to gatedBy.v, Pr.ROLE_SOURCE to roleSource.v)
    fun mockInterviewComplete(questionsAnswered: Int, gatedBy: GatedBy) =
        log(Ev.MOCK_INTERVIEW_COMPLETE, Pr.QUESTIONS_ANSWERED to questionsAnswered, Pr.GATED_BY to gatedBy.v)
    fun copilotSessionStart() = log(Ev.COPILOT_SESSION_START, Pr.GATED_BY to GatedBy.PRO.v)

    // ---- Monetization spine: ILRD revenue (value + currency REQUIRED, rule 7) ----

    /**
     * Logs `ad_impression` from an ILRD paid callback — fired for EVERY format
     * (native/interstitial/rewarded/app_open). [valueMicros] is the SDK's raw
     * micro-units; it is normalized to a real [Pr.VALUE] (÷ 1,000,000). The value
     * and [currency] are always the real ILRD figures — never hardcoded (rule 7).
     */
    fun adImpression(
        valueMicros: Long,
        currency: String,
        adFormat: String,
        adSource: String?,
        adUnit: String?,
        placementId: String,
        precision: AdPrecision,
    ) = log(
        Ev.AD_IMPRESSION,
        Pr.VALUE to valueMicros / 1_000_000.0,
        Pr.CURRENCY to currency,
        Pr.AD_FORMAT to adFormat,
        Pr.AD_SOURCE to adSource,
        Pr.AD_UNIT to adUnit,
        Pr.PLACEMENT to placementId,
        Pr.PRECISION to precision.v,
        Pr.SESSION_NUMBER to sessionNumber,
    )

    // ---- Rewarded unlocks: earned vs granted tracked separately (rule 5) ----

    /** SDK earned-reward callback fired. Logged BEFORE the app grants anything. */
    fun adRewardEarned(placementId: String, rewardType: String) =
        log(Ev.AD_REWARD_EARNED, Pr.PLACEMENT to placementId, Pr.REWARD_TYPE to rewardType)

    /** App actually unlocked the reward (exactly once). earned-without-granted = bug. */
    fun adRewardGranted(placementId: String, rewardType: String) =
        log(Ev.AD_REWARD_GRANTED, Pr.PLACEMENT to placementId, Pr.REWARD_TYPE to rewardType)

    // ---- Paywall funnel ----

    fun paywallView(variant: PaywallVariant, trigger: TriggerPlacement?) =
        log(Ev.PAYWALL_VIEW, Pr.VARIANT to variant.v, Pr.TRIGGER_PLACEMENT to trigger?.v)
    fun paywallStartTrialClick(variant: PaywallVariant) = log(Ev.PAYWALL_START_TRIAL_CLICK, Pr.VARIANT to variant.v)
    fun paywallDismiss(variant: PaywallVariant, trigger: TriggerPlacement?) =
        log(Ev.PAYWALL_DISMISS, Pr.VARIANT to variant.v, Pr.TRIGGER_PLACEMENT to trigger?.v)

    // ---- Purchase (THE revenue source of truth; real local value+currency, rule 7) ----

    /**
     * Logs the manual `purchase` event with the REAL local [valueMicros]+[currency]
     * from Play Billing `ProductDetails` — never a hardcoded/pre-converted USD figure.
     * Deduped on [transactionId] so a re-delivered purchase is counted once; this is
     * the single source of truth (the Firebase auto `in_app_purchase` event must NOT
     * be marked a key event — see event-schema §5).
     */
    suspend fun purchase(
        valueMicros: Long,
        currency: String,
        productId: String,
        productType: String,
        trial: Boolean,
        isRenewal: Boolean,
        transactionId: String,
    ) {
        if (!profile.recordPurchaseOnce(transactionId)) return   // dedupe on transaction_id
        log(
            Ev.PURCHASE,
            Pr.VALUE to valueMicros / 1_000_000.0,
            Pr.CURRENCY to currency,
            Pr.PRODUCT_ID to productId,
            Pr.PRODUCT_TYPE to productType,
            Pr.TRIAL to trial,
            Pr.IS_RENEWAL to isRenewal,
            Pr.TRANSACTION_ID to transactionId,
            Pr.PURCHASE_SOURCE to "play_billing_direct",
        )
    }

    // ---- Ad-lifecycle diagnostics (DEBUG funnel; NOT imported to Google Ads) ----

    fun adRequest(placementId: String, format: String) =
        log(Ev.AD_REQUEST, Pr.PLACEMENT_ID to placementId, Pr.AD_FORMAT to format)
    fun adLoaded(placementId: String, format: String, adSource: String?) =
        log(Ev.AD_LOADED, Pr.PLACEMENT_ID to placementId, Pr.AD_FORMAT to format, Pr.AD_SOURCE to adSource)
    fun adLoadFailed(placementId: String, format: String, reason: String) =
        log(Ev.AD_LOAD_FAILED, Pr.PLACEMENT_ID to placementId, Pr.AD_FORMAT to format, Pr.REASON to reason)
    fun adShowAttempt(placementId: String, format: String) =
        log(Ev.AD_SHOW_ATTEMPT, Pr.PLACEMENT_ID to placementId, Pr.AD_FORMAT to format)
    fun adShowFailed(placementId: String, reason: String) =
        log(Ev.AD_SHOW_FAILED, Pr.PLACEMENT_ID to placementId, Pr.REASON to reason)
    fun adSuppressed(placementId: String, reason: String) =
        log(Ev.AD_SUPPRESSED, Pr.PLACEMENT_ID to placementId, Pr.REASON to reason)
    fun adDismissed(placementId: String, format: String) =
        log(Ev.AD_DISMISSED, Pr.PLACEMENT_ID to placementId, Pr.AD_FORMAT to format)

    // ---- Failures (low-cardinality; separate "broke" from "lost interest") ----

    fun jobSearchFailed(error: ErrorType) = log(Ev.JOB_SEARCH_FAILED, Pr.ERROR_TYPE to error.v)
    fun resumeUploadFailed(error: ErrorType) = log(Ev.RESUME_UPLOAD_FAILED, Pr.ERROR_TYPE to error.v)
    fun resumeOptimizeFailed(error: ErrorType) = log(Ev.RESUME_OPTIMIZE_FAILED, Pr.ERROR_TYPE to error.v)
    fun mockInterviewFailed(error: ErrorType) = log(Ev.MOCK_INTERVIEW_FAILED, Pr.ERROR_TYPE to error.v)
    fun copilotAnswerFailed(error: ErrorType) = log(Ev.COPILOT_ANSWER_FAILED, Pr.ERROR_TYPE to error.v)
    fun externalApplyFailed(error: ErrorType) = log(Ev.EXTERNAL_APPLY_FAILED, Pr.ERROR_TYPE to error.v)
    fun filePickerCancelled() = log(Ev.FILE_PICKER_CANCELLED, Pr.REASON to "user_cancel")
    fun permissionResult(permission: AppPermission, granted: Boolean) =
        log(Ev.PERMISSION_RESULT, Pr.PERMISSION to permission.v, Pr.GRANTED to granted)

    // ---- Retention / push (push_open ONLY) ----

    fun pushOpen(channel: PushChannel, target: DeepLinkTarget) =
        log(Ev.PUSH_OPEN, Pr.CHANNEL to channel.v, Pr.DEEP_LINK_TARGET to target.v)

    fun screen(name: ScreenName) = log(Ev.SCREEN_VIEW, Pr.SCREEN_NAME to name.v, Pr.SCREEN_CLASS to name.v)

    // ---- Non-fatal error capture (diagnostics; never PII) ----

    fun recordError(throwable: Throwable, context: Map<String, Any?> = emptyMap()) {
        if (BuildConfig.DEBUG) Log.w(TAG, "non-fatal: ${context + Diagnostics.base()}", throwable)
        // Crashlytics.recordException(throwable) drops in here once Firebase is configured.
    }

    // ---- internals ----

    private fun setUserProp(name: String, value: String?) {
        fa?.setUserProperty(name, value?.take(36))
        if (BuildConfig.DEBUG) Log.d(TAG, "userProp $name=$value")
    }

    private fun log(event: String, vararg params: Pair<String, Any?>) {
        val bundle = Bundle()
        for ((k, v) in params) {
            when (v) {
                null -> {}
                is String -> bundle.putString(k, v.take(100))
                is Boolean -> bundle.putString(k, v.toString())   // GA4: booleans as "true"/"false"
                is Int -> bundle.putLong(k, v.toLong())
                is Long -> bundle.putLong(k, v)
                is Double -> bundle.putDouble(k, v)
                else -> bundle.putString(k, v.toString().take(100))
            }
        }
        fa?.logEvent(event, bundle)
        if (BuildConfig.DEBUG) Log.d(TAG, "$event ${params.filter { it.second != null }.joinToString { "${it.first}=${it.second}" }}")
    }

    private companion object { const val TAG = "Analytics" }
}
