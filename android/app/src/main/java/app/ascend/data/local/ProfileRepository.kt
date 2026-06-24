package app.ascend.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.ascend.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @Named("device") private val deviceStore: DataStore<Preferences>,
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val ROLE = stringPreferencesKey("target_role")
        val LOCATION = stringPreferencesKey("location")
        val RESUME = stringPreferencesKey("resume_name")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val INSTALL_ID = stringPreferencesKey("anonymous_install_id")
        val SELECTED_RESUME = stringPreferencesKey("selected_resume_id")
        val SESSION_NUMBER = intPreferencesKey("session_number")
        val ACTIVATED = booleanPreferencesKey("activated")
        val LOGGED_PURCHASES = stringSetPreferencesKey("logged_purchase_tx")
        val ONB_INTERSTITIAL_SHOWN = booleanPreferencesKey("onboarding_interstitial_shown")
        val TOUR_SHOWN = booleanPreferencesKey("onboarding_tour_shown")
        val ONBOARDED_ONCE = booleanPreferencesKey("onboarded_once")
    }

    /** True once the onboarding tour has been shown (once_per_install suppression). */
    suspend fun tourShown(): Boolean = dataStore.data.first()[Keys.TOUR_SHOWN] ?: false
    suspend fun markTourShown() { dataStore.edit { it[Keys.TOUR_SHOWN] = true } }

    /** True if the user has completed onboarding at least once before (returning-user suppression). */
    suspend fun onboardedOnce(): Boolean = dataStore.data.first()[Keys.ONBOARDED_ONCE] ?: false
    suspend fun markOnboardedOnce() { dataStore.edit { it[Keys.ONBOARDED_ONCE] = true } }

    /** True once the onboarding-complete interstitial has shown (per-install cap of 1). */
    suspend fun onboardingInterstitialShown(): Boolean = dataStore.data.first()[Keys.ONB_INTERSTITIAL_SHOWN] ?: false
    suspend fun markOnboardingInterstitialShown() { dataStore.edit { it[Keys.ONB_INTERSTITIAL_SHOWN] = true } }

    /**
     * Atomically records that purchase [txId] has been logged; returns true only the
     * FIRST time (so the `purchase` event is deduped on transaction_id — one source
     * of truth, never double-counted against the Firebase auto event).
     */
    suspend fun recordPurchaseOnce(txId: String): Boolean {
        var fresh = false
        dataStore.edit { p ->
            val seen = p[Keys.LOGGED_PURCHASES] ?: emptySet()
            if (txId !in seen) { p[Keys.LOGGED_PURCHASES] = seen + txId; fresh = true }
        }
        return fresh
    }

    /** Local session counter — incremented once per cold start (analytics session_number). */
    suspend fun nextSessionNumber(): Int {
        var n = 1
        dataStore.edit { p -> n = (p[Keys.SESSION_NUMBER] ?: 0) + 1; p[Keys.SESSION_NUMBER] = n }
        return n
    }

    /** Reads the current session counter WITHOUT incrementing (ad eligibility checks). */
    suspend fun currentSessionNumber(): Int = dataStore.data.first()[Keys.SESSION_NUMBER] ?: 1

    /** True once the user has completed a core action (drives user_ad_segment). */
    suspend fun activatedOnce(): Boolean = dataStore.data.first()[Keys.ACTIVATED] ?: false
    suspend fun markActivated() { dataStore.edit { it[Keys.ACTIVATED] = true } }

    /** Id of the resume the user has marked as active (drives optimize/generate targets). */
    val selectedResumeId: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_RESUME] }

    suspend fun setSelectedResume(id: String?) {
        dataStore.edit { p -> if (id.isNullOrBlank()) p.remove(Keys.SELECTED_RESUME) else p[Keys.SELECTED_RESUME] = id }
    }

    suspend fun selectedResumeIdOnce(): String? = dataStore.data.first()[Keys.SELECTED_RESUME]

    /**
     * Stable anonymous install id (random UUID), generated once on first access.
     * Stored in the device-scoped store which is excluded from Android backup, so
     * it is regenerated on a fresh install rather than restored from the cloud.
     */
    suspend fun installId(): String {
        deviceStore.data.first()[Keys.INSTALL_ID]?.let { return it }
        val id = UUID.randomUUID().toString()
        deviceStore.edit { it[Keys.INSTALL_ID] = id }
        return id
    }

    val profile: Flow<UserProfile> = dataStore.data.map { p ->
        UserProfile(
            name = p[Keys.NAME].orEmpty(),
            targetRole = p[Keys.ROLE].orEmpty(),
            location = p[Keys.LOCATION].orEmpty(),
            resumeName = p[Keys.RESUME],
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun save(profile: UserProfile) {
        dataStore.edit { p ->
            p[Keys.NAME] = profile.name
            p[Keys.ROLE] = profile.targetRole
            p[Keys.LOCATION] = profile.location
            p[Keys.ONBOARDED] = profile.onboarded
            if (profile.resumeName.isNullOrBlank()) p.remove(Keys.RESUME) else p[Keys.RESUME] = profile.resumeName
        }
    }

    suspend fun setResume(name: String?) {
        dataStore.edit { p -> if (name.isNullOrBlank()) p.remove(Keys.RESUME) else p[Keys.RESUME] = name }
    }

    /** Clears the profile + onboarding flag so the user re-runs onboarding. Keeps the device install id. */
    suspend fun resetOnboarding() {
        dataStore.edit { p ->
            p.remove(Keys.NAME); p.remove(Keys.ROLE); p.remove(Keys.LOCATION)
            p.remove(Keys.RESUME); p.remove(Keys.SELECTED_RESUME)
            p[Keys.ONBOARDED] = false
        }
    }

    suspend fun update(transform: (UserProfile) -> UserProfile) {
        dataStore.edit { p ->
            val cur = UserProfile(
                name = p[Keys.NAME].orEmpty(), targetRole = p[Keys.ROLE].orEmpty(),
                location = p[Keys.LOCATION].orEmpty(), resumeName = p[Keys.RESUME],
                onboarded = p[Keys.ONBOARDED] ?: false,
            )
            val next = transform(cur)
            p[Keys.NAME] = next.name; p[Keys.ROLE] = next.targetRole
            p[Keys.LOCATION] = next.location; p[Keys.ONBOARDED] = next.onboarded
            if (next.resumeName.isNullOrBlank()) p.remove(Keys.RESUME) else p[Keys.RESUME] = next.resumeName
        }
    }
}
