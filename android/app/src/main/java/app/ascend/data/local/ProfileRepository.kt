package app.ascend.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ascend.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val NAME = stringPreferencesKey("name")
        val ROLE = stringPreferencesKey("target_role")
        val LOCATION = stringPreferencesKey("location")
        val RESUME = stringPreferencesKey("resume_name")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val INSTALL_ID = stringPreferencesKey("anonymous_install_id")
        val SELECTED_RESUME = stringPreferencesKey("selected_resume_id")
    }

    /** Id of the resume the user has marked as active (drives optimize/generate targets). */
    val selectedResumeId: Flow<String?> = dataStore.data.map { it[Keys.SELECTED_RESUME] }

    suspend fun setSelectedResume(id: String?) {
        dataStore.edit { p -> if (id.isNullOrBlank()) p.remove(Keys.SELECTED_RESUME) else p[Keys.SELECTED_RESUME] = id }
    }

    suspend fun selectedResumeIdOnce(): String? = dataStore.data.first()[Keys.SELECTED_RESUME]

    /** Stable anonymous install id (random UUID), generated once on first access. */
    suspend fun installId(): String {
        dataStore.data.first()[Keys.INSTALL_ID]?.let { return it }
        val id = UUID.randomUUID().toString()
        dataStore.edit { it[Keys.INSTALL_ID] = id }
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
