package app.ascend.data.billing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Local cache of the user's entitlement, refreshed from Play Billing. */
@Singleton
class EntitlementRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val TIER = stringPreferencesKey("entitlement_tier")
        val SOURCE = stringPreferencesKey("entitlement_source")
        val EXPIRY = longPreferencesKey("entitlement_expiry")
    }

    val entitlement: Flow<Entitlement> = dataStore.data.map { p ->
        Entitlement(
            tier = if (p[Keys.TIER] == Tier.PRO.name) Tier.PRO else Tier.FREE,
            source = p[Keys.SOURCE],
            expiryEpochMs = p[Keys.EXPIRY],
        )
    }

    val isPro: Flow<Boolean> = entitlement.map { it.isPro }

    suspend fun set(entitlement: Entitlement) {
        dataStore.edit { p ->
            p[Keys.TIER] = entitlement.tier.name
            if (entitlement.source != null) p[Keys.SOURCE] = entitlement.source else p.remove(Keys.SOURCE)
            if (entitlement.expiryEpochMs != null) p[Keys.EXPIRY] = entitlement.expiryEpochMs else p.remove(Keys.EXPIRY)
        }
    }
}
