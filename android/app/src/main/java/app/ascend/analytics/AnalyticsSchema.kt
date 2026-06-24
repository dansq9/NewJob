package app.ascend.analytics

import app.ascend.core.isOffline

/**
 * Typed constants generated from /docs/event-schema.md. The ONLY place raw
 * event/param strings live (CLAUDE.md rules 8-9). Screens/VMs call typed methods
 * on [AnalyticsTracker]; nothing logs ad-hoc strings.
 *
 * GA4 limits enforced by design: event/param name ≤40, value ≤100,
 * user-property name ≤24 / value ≤36.
 */
object Ev {
    // Acquisition & activation
    const val ONBOARDING_STEP = "onboarding_step"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val CORE_ACTION_DONE = "core_action_done"
    // Core job loop
    const val JOB_SEARCH = "job_search"
    const val JOB_DETAIL_VIEW = "job_detail_view"
    const val JOB_SAVE = "job_save"
    const val JOB_APPLY_CLICK = "job_apply_click"
    const val TRACKER_STAGE_CHANGE = "tracker_stage_change"
    // Resume tools
    const val RESUME_UPLOAD = "resume_upload"
    const val RESUME_OPTIMIZE_START = "resume_optimize_start"
    const val RESUME_OPTIMIZE_COMPLETE = "resume_optimize_complete"
    const val RESUME_DOWNLOAD = "resume_download"
    const val COVER_LETTER_GENERATE = "cover_letter_generate"
    // Interview
    const val MOCK_INTERVIEW_START = "mock_interview_start"
    const val MOCK_INTERVIEW_COMPLETE = "mock_interview_complete"
    const val COPILOT_SESSION_START = "copilot_session_start"
    // Monetization spine (value+currency)
    const val AD_IMPRESSION = "ad_impression"
    const val REWARDED_AD_START = "rewarded_ad_start"
    const val REWARDED_AD_COMPLETE = "rewarded_ad_complete"
    const val PURCHASE = "purchase"
    // Paywall funnel
    const val PAYWALL_VIEW = "paywall_view"
    const val PAYWALL_START_TRIAL_CLICK = "paywall_start_trial_click"
    const val PAYWALL_DISMISS = "paywall_dismiss"
    // Failures & errors
    const val JOB_SEARCH_FAILED = "job_search_failed"
    const val RESUME_UPLOAD_FAILED = "resume_upload_failed"
    const val RESUME_OPTIMIZE_FAILED = "resume_optimize_failed"
    const val MOCK_INTERVIEW_FAILED = "mock_interview_failed"
    const val COPILOT_ANSWER_FAILED = "copilot_answer_failed"
    const val EXTERNAL_APPLY_FAILED = "external_apply_failed"
    const val FILE_PICKER_CANCELLED = "file_picker_cancelled"
    const val PERMISSION_RESULT = "permission_result"
    // Retention & push (push_open ONLY — push_sent is server-side)
    const val SESSION_START_ENRICHED = "session_start_enriched"
    const val APP_OPEN_RESUME = "app_open_resume"
    const val PUSH_OPEN = "push_open"
    // Onboarding tour-guide + animations (RC-controlled onboarding UI)
    const val ONBOARDING_TOUR_VIEW = "onboarding_tour_view"
    const val ONBOARDING_TOUR_SKIP = "onboarding_tour_skip"
    const val ONBOARDING_TOUR_COMPLETE = "onboarding_tour_complete"
    const val ONBOARDING_ANIMATION_VARIANT = "onboarding_animation_variant"
    // Ad lifecycle diagnostics (DEBUG — not imported to Google Ads)
    const val AD_REQUEST = "ad_request"
    const val AD_LOADED = "ad_loaded"
    const val AD_LOAD_FAILED = "ad_load_failed"
    const val AD_SHOW_ATTEMPT = "ad_show_attempt"
    const val AD_SHOW_FAILED = "ad_show_failed"
    const val AD_SUPPRESSED = "ad_suppressed"
    const val AD_DISMISSED = "ad_dismissed"
    const val AD_REWARD_EARNED = "ad_reward_earned"
    const val AD_REWARD_GRANTED = "ad_reward_granted"
    const val SCREEN_VIEW = "screen_view"
}

object Pr {
    const val STEP = "step"
    const val SKIPPED = "skipped"
    const val TARGET_ROLE_PRESENT = "target_role_present"
    const val TARGET_ROLE_CATEGORY = "target_role_category"
    const val LOCATION_TYPE = "location_type"
    const val RESUME_UPLOADED = "resume_uploaded"
    const val ACTION_TYPE = "action_type"
    const val SESSION_NUMBER = "session_number"
    const val QUERY_PRESENT = "query_present"
    const val FILTERS_USED = "filters_used"
    const val RESULTS_COUNT = "results_count"
    const val SOURCE = "source"
    const val MATCH_SCORE_BAND = "match_score_band"
    const val EMPLOYMENT_TYPE = "employment_type"
    const val REMOTE_TYPE = "remote_type"
    const val FROM_SCREEN = "from_screen"
    const val APPLY_TYPE = "apply_type"
    const val FROM_STAGE = "from_stage"
    const val TO_STAGE = "to_stage"
    const val FILE_TYPE = "file_type"
    const val FILE_SIZE_BAND = "file_size_band"
    const val HAS_TARGET_JOB = "has_target_job"
    const val SCORE_BAND = "score_band"
    const val GATED_BY = "gated_by"
    const val FORMAT = "format"
    const val ROLE_SOURCE = "role_source"
    const val QUESTIONS_ANSWERED = "questions_answered"
    const val VALUE = "value"
    const val CURRENCY = "currency"
    const val AD_FORMAT = "ad_format"
    const val AD_SOURCE = "ad_source"
    const val AD_UNIT = "ad_unit"
    const val PLACEMENT = "placement"
    const val PLACEMENT_ID = "placement_id"
    const val PRECISION = "precision"
    const val REWARD_TYPE = "reward_type"
    const val REWARD_GRANTED = "reward_granted"
    const val PRODUCT_ID = "product_id"
    const val PRODUCT_TYPE = "product_type"
    const val TRIAL = "trial"
    const val IS_RENEWAL = "is_renewal"
    const val TRANSACTION_ID = "transaction_id"
    const val PURCHASE_SOURCE = "purchase_source"
    const val VARIANT = "variant"
    const val TRIGGER_PLACEMENT = "trigger_placement"
    const val ERROR_TYPE = "error_type"
    const val REASON = "reason"
    const val PERMISSION = "permission"
    const val GRANTED = "granted"
    const val USER_AD_SEGMENT = "user_ad_segment"
    const val WAS_AD_SHOWN = "was_ad_shown"
    const val SUPPRESSED_REASON = "suppressed_reason"
    const val CHANNEL = "channel"
    const val DEEP_LINK_TARGET = "deep_link_target"
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val REFERRER_SOURCE = "referrer_source"
    const val CARD_INDEX = "card_index"
    const val CARDS_SEEN = "cards_seen"
}

/** User property names (≤24 chars each). */
object UserProp {
    const val INSTALL_ID = "install_id"
    const val ACCOUNT_STATUS = "user_account_status"
    const val PLAN = "user_plan"
    const val AD_SEGMENT = "user_ad_segment"
    const val AGGRESSIVENESS_TIER = "ad_aggressiveness_tier"
    const val RC_VARIANT = "rc_variant"
    const val APP_VERSION = "app_version"
    const val OS_VERSION = "os_version"
    const val COUNTRY = "country"
    const val REFERRER_SOURCE = "referrer_source"
}

// ---- Controlled vocabularies (no free strings; enforces the schema's enums) ----

enum class GatedBy(val v: String) { FREE("free"), REWARDED("rewarded"), PRO("pro") }
enum class RoleSource(val v: String) { TARGET_ROLE("target_role"), JOB("job") }
enum class Band(val v: String) { HIGH("high"), MED("med"), LOW("low") }
enum class SizeBand(val v: String) { SMALL("small"), MED("med"), LARGE("large") }
enum class FileType(val v: String) { PDF("pdf"), DOCX("docx"), DOC("doc") }
enum class CoreAction(val v: String) { SEARCH("search"), SAVE("save"), UPLOAD("upload"), APPLY("apply"), MOCK_START("mock_start") }
enum class SearchSource(val v: String) { HOME("home"), JOBS_TAB("jobs_tab"), PUSH("push") }
enum class SaveFrom(val v: String) { SEARCH("search"), DETAIL("detail") }
enum class ApplyType(val v: String) { EXTERNAL("external"), INTERNAL("internal") }
enum class LocationType(val v: String) { REMOTE("remote"), HYBRID("hybrid"), ONSITE("onsite"), UNKNOWN("unknown") }

/** Controlled employment_type vocabulary (job_detail_view). Never log the raw JSearch string. */
enum class EmploymentType(val v: String) {
    FULLTIME("fulltime"), PARTTIME("parttime"), CONTRACTOR("contractor"),
    INTERN("intern"), OTHER("other")
}

fun employmentTypeOf(raw: String?): EmploymentType {
    val r = raw?.lowercase().orEmpty()
    return when {
        r.isBlank() -> EmploymentType.OTHER
        r.contains("full") -> EmploymentType.FULLTIME
        r.contains("part") -> EmploymentType.PARTTIME
        r.contains("contract") || r.contains("freelan") -> EmploymentType.CONTRACTOR
        r.contains("intern") -> EmploymentType.INTERN
        else -> EmploymentType.OTHER
    }
}

/** Controlled tracker-stage vocabulary (tracker_stage_change), mapped from the domain TrackStage. */
enum class TrackerStage(val v: String) {
    UNTRACKED("untracked"), SAVED("saved"), APPLIED("applied"),
    INTERVIEW("interview"), OFFER("offer"), CLOSED("closed")
}

fun trackerStageOf(stage: app.ascend.data.model.TrackStage?): TrackerStage = when (stage) {
    null -> TrackerStage.UNTRACKED
    app.ascend.data.model.TrackStage.SAVED -> TrackerStage.SAVED
    app.ascend.data.model.TrackStage.APPLIED -> TrackerStage.APPLIED
    app.ascend.data.model.TrackStage.INTERVIEW -> TrackerStage.INTERVIEW
    app.ascend.data.model.TrackStage.OFFER -> TrackerStage.OFFER
    app.ascend.data.model.TrackStage.CLOSED -> TrackerStage.CLOSED
}
enum class RoleCategory(val v: String) {
    PRODUCT("product"), ENGINEERING("engineering"), SALES("sales"),
    MARKETING("marketing"), OPS("ops"), OTHER("other")
}
enum class OnboardingStep(val v: String) { LANGUAGE("language"), ROLE("role"), LOCATION("location"), RESUME("resume") }
enum class ErrorType(val v: String) {
    NETWORK("network"), TIMEOUT("timeout"), VALIDATION("validation"),
    NO_RESULTS("no_results"), UNSUPPORTED_FILE("unsupported_file"), API_ERROR("api_error")
}

/** Maps a caught throwable to a low-cardinality [ErrorType] for the *_failed events. */
fun errorTypeOf(t: Throwable): ErrorType = when {
    t is java.net.SocketTimeoutException -> ErrorType.TIMEOUT
    t is retrofit2.HttpException -> ErrorType.API_ERROR
    t.isOffline() -> ErrorType.NETWORK
    else -> ErrorType.API_ERROR
}

// ---- PII-safe banders: map raw values to low-cardinality bands (never log the raw value) ----

fun bandOf(score: Int): Band = when {
    score >= 80 -> Band.HIGH
    score >= 50 -> Band.MED
    else -> Band.LOW
}

fun sizeBandOf(bytes: Long?): SizeBand = when {
    bytes == null -> SizeBand.MED
    bytes < 100_000 -> SizeBand.SMALL
    bytes < 1_000_000 -> SizeBand.MED
    else -> SizeBand.LARGE
}

fun fileTypeOf(name: String?): FileType = when {
    name == null -> FileType.PDF
    name.endsWith(".docx", true) -> FileType.DOCX
    name.endsWith(".doc", true) -> FileType.DOC
    else -> FileType.PDF
}

/** Coarse role bucket from a free-text target role (never logs the raw role string). */
fun roleCategoryOf(role: String): RoleCategory {
    val r = role.lowercase()
    return when {
        r.isBlank() -> RoleCategory.OTHER
        listOf("product manager", "product owner", " pm", "product").any { r.contains(it) } -> RoleCategory.PRODUCT
        listOf("engineer", "developer", "programmer", "swe", "data ", "scientist").any { r.contains(it) } -> RoleCategory.ENGINEERING
        listOf("sales", "account exec", " ae", "business develop").any { r.contains(it) } -> RoleCategory.SALES
        listOf("market", "growth", "seo", "content", "brand").any { r.contains(it) } -> RoleCategory.MARKETING
        listOf("operations", "ops", "logistics", "supply").any { r.contains(it) } -> RoleCategory.OPS
        else -> RoleCategory.OTHER
    }
}
enum class PaywallVariant(val v: String) { CONTROL("control"), DISCOUNT("discount"), TRIAL("trial"), LIFETIME("lifetime") }
enum class TriggerPlacement(val v: String) { COPILOT("copilot"), RESUME_DOWNLOAD("resume_download"), MOCK_SCORE("mock_score") }
enum class PushChannel(val v: String) {
    JOBS_FRESH("jobs_fresh"), TRACKER("tracker"), RESUME_INTERVIEW("resume_interview"),
    WINBACK("winback"), MONETIZATION("monetization")
}
enum class DeepLinkTarget(val v: String) { JOBS("jobs"), TRACKER("tracker"), RESUME("resume") }
enum class AppPermission(val v: String) { RECORD_AUDIO("record_audio"), STORAGE("storage") }
enum class ScreenName(val v: String) {
    HOME("home"), JOBS("jobs"), JOB_DETAIL("job_detail"), TRACKER("tracker"),
    RESUME("resume"), RESUME_RESULT("resume_result"), MOCK_INTERVIEW("mock_interview"),
    COPILOT("copilot"), GAMES("games"), PAYWALL("paywall")
}

/** user_ad_segment (schema §7). */
enum class AdSegment(val v: String) {
    NEW("new"), ACTIVATED("activated"), AD_TOLERANT("ad_tolerant"),
    AD_SENSITIVE("ad_sensitive"), PAYER("payer"), LAPSED("lapsed")
}

/** ad_aggressiveness_tier — set from Remote Config later; default balanced. */
enum class AggressivenessTier(val v: String) {
    RETENTION_PROTECT("retention_protect"), BALANCED("balanced"),
    ARBITRAGE_HIGH("arbitrage_high"), PAID_OR_HIGH_INTENT("paid_or_high_intent")
}

enum class AccountStatus(val v: String) { FREE("Free"), PRO("Pro") }
enum class Plan(val v: String) { FREE("Free"), WEEKLY("Weekly"), YEARLY("Yearly"), LIFETIME("Lifetime") }

// ---- Onboarding tour-guide + animation controlled vocabularies (RC-mapped) ----

/** Tour size. [cap] = max cards this variant ever shows (FULL = no cap of its own). */
enum class OnboardingTourVariant(val v: String, val cap: Int) {
    NONE("none", 0), ONE_CARD("one_card", 1), THREE_CARD("three_card", 3), FULL("full", Int.MAX_VALUE);
    companion object { fun fromValue(s: String?) = entries.firstOrNull { it.v == s } ?: NONE }
}

/**
 * Where the onboarding tour appears. This native flow has NO language step, so
 * [BEFORE_LANGUAGE] / [AFTER_LANGUAGE] are LEGACY/ABSTRACT names (cross-platform RC parity)
 * mapped to the Welcome / Name steps in OnboardingScreen — see the mapping table there.
 * Future: add `before_welcome` / `after_welcome` aliases when the RC schema is revised.
 */
enum class OnboardingTourPlacement(val v: String) {
    BEFORE_LANGUAGE("before_language"), AFTER_LANGUAGE("after_language"),
    AFTER_LOCATION("after_location"), BEFORE_HOME("before_home");
    companion object { fun fromValue(s: String?) = entries.firstOrNull { it.v == s } ?: AFTER_LOCATION }
}

enum class OnboardingAnimationVariant(val v: String) {
    NONE("none"), SUBTLE("subtle"), STANDARD("standard"), RICH("rich");
    companion object { fun fromValue(s: String?) = entries.firstOrNull { it.v == s } ?: NONE }
}

/**
 * ILRD value precision (`ad_impression.precision`). Mirrors the Google Mobile Ads
 * `AdValue.PrecisionType` int so the rest of the app stays SDK-agnostic:
 * 0=unknown, 1=estimated, 2=publisher_provided, 3=precise.
 */
enum class AdPrecision(val v: String) {
    UNKNOWN("unknown"), ESTIMATED("estimated"), PUBLISHER_PROVIDED("publisher_provided"), PRECISE("precise");

    companion object {
        fun fromAdMob(precisionType: Int): AdPrecision = when (precisionType) {
            1 -> ESTIMATED
            2 -> PUBLISHER_PROVIDED
            3 -> PRECISE
            else -> UNKNOWN
        }
    }
}
