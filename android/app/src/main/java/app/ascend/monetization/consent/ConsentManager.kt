package app.ascend.monetization.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import app.ascend.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google UMP consent gate (CLAUDE.md rule 1 / monetization-spec "Consent gate").
 *
 * The UMP SDK ships GLOBALLY. On every launch we call [requestConsentInfoUpdate];
 * [canRequestAds] stays **false** until that resolves AND consent permits ad
 * requests. The consent form is shown only where the Google-certified CMP / IAB
 * TCF mandate applies (EEA + UK + Switzerland); elsewhere it no-ops.
 *
 * Nothing in the app may initialize an ad SDK or request an ad until
 * [canRequestAds] turns true — this is an ad-init ORDERING rule, enforced by the
 * orchestration in MainActivity, not just a legal checkbox.
 */
@Singleton
class ConsentManager @Inject constructor(
    @ApplicationContext appContext: Context,
) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(appContext)

    private val _canRequestAds = MutableStateFlow(false)

    /** True only after [requestConsentInfoUpdate] resolves and consent allows ad requests. */
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    /**
     * Run on every launch from the host Activity. Updates consent info, shows the
     * form where required, then opens the ad gate iff [ConsentInformation.canRequestAds].
     */
    fun gather(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    // Force EEA geography in debug so the gate + form can be verified
                    // without travelling. To actually SEE the form on a device, add that
                    // device's hashed id (printed in Logcat) via addTestDeviceHashedId(...).
                    setConsentDebugSettings(
                        ConsentDebugSettings.Builder(activity)
                            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                            .build()
                    )
                }
            }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity, params,
            {
                // Consent info updated. Show the form where required (no-op elsewhere),
                // then (re)evaluate the gate from the final consent state.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null && BuildConfig.DEBUG) {
                        Log.w(TAG, "consent form: ${formError.errorCode} ${formError.message}")
                    }
                    updateGate()
                }
            },
            { requestError ->
                // Update failed: honor any consent cached from a prior session, else stay gated.
                if (BuildConfig.DEBUG) Log.w(TAG, "consent update failed: ${requestError.errorCode} ${requestError.message}")
                updateGate()
            },
        )
    }

    private fun updateGate() {
        val allowed = consentInformation.canRequestAds()
        _canRequestAds.value = allowed
        if (BuildConfig.DEBUG) Log.d(TAG, "canRequestAds=$allowed privacyOptions=${privacyOptionsRequired()}")
    }

    /** True where a persistent "Privacy options" entry must be offered (EEA/UK/CH with a form). */
    fun privacyOptionsRequired(): Boolean =
        consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Re-present the consent form so the user can change their choices. */
    fun showPrivacyOptions(activity: Activity, onError: (String?) -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            // Choices may have changed — re-evaluate the gate.
            updateGate()
            onError(formError?.message)
        }
    }

    private companion object { const val TAG = "ConsentManager" }
}
