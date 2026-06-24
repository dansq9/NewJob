package app.ascend.ui.screens.onboarding

import app.ascend.analytics.OnboardingAnimationVariant
import app.ascend.analytics.OnboardingTourPlacement
import app.ascend.analytics.OnboardingTourVariant
import app.ascend.monetization.config.RcKeys
import app.ascend.monetization.config.RemoteConfig
import javax.inject.Inject
import javax.inject.Singleton

/** Validated, clamped onboarding tour + animation config. Affects onboarding UI only. */
data class OnboardingConfig(
    val tourEnabled: Boolean,
    val tourVariant: OnboardingTourVariant,
    val maxCards: Int,
    val forceCompletion: Boolean,
    val showSkip: Boolean,
    val placement: OnboardingTourPlacement,
    val suppressIfResumeUploaded: Boolean,
    val suppressIfReturningUser: Boolean,
    val oncePerInstall: Boolean,
    val animationsEnabled: Boolean,
    val animationVariant: OnboardingAnimationVariant,
    val animationDurationMs: Long,
    val reduceMotionRespectSystem: Boolean,
    val splashBrandDurationMs: Long,
)

/**
 * Reads the onboarding tour/animation Remote Config into a typed [OnboardingConfig],
 * validating enums and clamping numerics into allowed ranges. NEVER throws and never
 * blocks onboarding: any bad/missing config falls back to conservative safe defaults
 * (tour OFF, minimal motion).
 */
@Singleton
class OnboardingConfigProvider @Inject constructor(private val rc: RemoteConfig) {

    fun load(): OnboardingConfig = runCatching {
        OnboardingConfig(
            tourEnabled = rc.bool(RcKeys.ONB_TOUR_ENABLED),
            tourVariant = OnboardingTourVariant.fromValue(rc.string(RcKeys.ONB_TOUR_VARIANT)),
            maxCards = rc.long(RcKeys.ONB_TOUR_MAX_CARDS).toInt().coerceIn(0, MAX_TOUR_CARDS),
            forceCompletion = rc.bool(RcKeys.ONB_TOUR_FORCE_COMPLETION),
            showSkip = rc.bool(RcKeys.ONB_TOUR_SHOW_SKIP),
            placement = OnboardingTourPlacement.fromValue(rc.string(RcKeys.ONB_TOUR_PLACEMENT)),
            suppressIfResumeUploaded = rc.bool(RcKeys.ONB_TOUR_SUPPRESS_IF_RESUME),
            suppressIfReturningUser = rc.bool(RcKeys.ONB_TOUR_SUPPRESS_IF_RETURNING),
            oncePerInstall = rc.bool(RcKeys.ONB_TOUR_ONCE_PER_INSTALL),
            animationsEnabled = rc.bool(RcKeys.ONB_ANIM_ENABLED),
            animationVariant = OnboardingAnimationVariant.fromValue(rc.string(RcKeys.ONB_ANIM_VARIANT)),
            animationDurationMs = rc.long(RcKeys.ONB_ANIM_DURATION_MS).coerceIn(300L, 1500L),
            reduceMotionRespectSystem = rc.bool(RcKeys.ONB_ANIM_REDUCE_MOTION_RESPECT),
            splashBrandDurationMs = rc.long(RcKeys.ONB_ANIM_SPLASH_BRAND_DURATION_MS).coerceIn(500L, 3000L),
        )
    }.getOrDefault(SAFE_DEFAULT)

    /**
     * How many tour cards to actually show given user state. 0 = skip the tour entirely
     * (disabled / variant none / suppressed / cap 0). Shows only the first N eligible cards.
     */
    fun eligibleCardCount(
        c: OnboardingConfig,
        resumeUploaded: Boolean,
        returningUser: Boolean,
        alreadyShown: Boolean,
    ): Int {
        if (!c.tourEnabled || c.tourVariant == OnboardingTourVariant.NONE) return 0
        if (c.suppressIfResumeUploaded && resumeUploaded) return 0
        if (c.suppressIfReturningUser && returningUser) return 0
        if (c.oncePerInstall && alreadyShown) return 0
        return minOf(c.maxCards, c.tourVariant.cap, MAX_TOUR_CARDS).coerceAtLeast(0)
    }

    companion object {
        /** The number of localized tour cards the app can render (see OnboardingTour). */
        const val MAX_TOUR_CARDS = 5
        val SAFE_DEFAULT = OnboardingConfig(
            tourEnabled = false, tourVariant = OnboardingTourVariant.NONE, maxCards = 0,
            forceCompletion = false, showSkip = true, placement = OnboardingTourPlacement.AFTER_LOCATION,
            suppressIfResumeUploaded = true, suppressIfReturningUser = true, oncePerInstall = true,
            animationsEnabled = true, animationVariant = OnboardingAnimationVariant.NONE,
            animationDurationMs = 500L, reduceMotionRespectSystem = true, splashBrandDurationMs = 800L,
        )
    }
}
