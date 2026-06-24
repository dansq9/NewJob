package app.ascend.analytics

/**
 * Analytics facade. Drop in Firebase Analytics + Sentry implementations (fan-out
 * to both) once `google-services.json` and the Sentry DSN are configured; until
 * then [NoopAnalytics] is bound. Keep all event names in [Ev] so the funnels
 * stay consistent.
 */
interface Analytics {
    fun log(event: String, params: Map<String, Any?> = emptyMap())
    fun screen(name: String)
    fun setUserProperty(key: String, value: String?)
}

/** Event taxonomy (funnels). */
object Ev {
    // Jobs funnel: search → detail → apply/save
    const val SEARCH = "job_search"
    const val JOB_VIEW = "job_detail_view"
    const val JOB_SAVE = "job_save"
    const val JOB_APPLY = "job_apply_click"

    // Resume funnel: upload → optimize → download
    const val RESUME_UPLOAD = "resume_upload"
    const val RESUME_OPTIMIZE = "resume_optimize"
    const val RESUME_DOWNLOAD = "resume_download"

    // Interview funnel: mock start → finish → report
    const val MOCK_START = "mock_start"
    const val MOCK_FINISH = "mock_finish"
    const val MOCK_REPORT = "mock_report_view"
    const val COPILOT_LAUNCH = "copilot_launch"

    // Monetization funnel: paywall view → subscribe
    const val PAYWALL_VIEW = "paywall_view"
    const val SUBSCRIBE = "subscribe"
    const val RESTORE = "restore_purchases"
    const val AD_IMPRESSION = "ad_impression"
    const val REWARDED_COMPLETE = "rewarded_complete"

    // Lifecycle
    const val ONBOARD_COMPLETE = "onboarding_complete"
}
