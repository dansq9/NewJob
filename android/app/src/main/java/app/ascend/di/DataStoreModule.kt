package app.ascend.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    /** Backed-up store: profile, onboarding, entitlement, selected resume. */
    @Provides @Singleton
    fun preferencesDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("ascend_profile") }

    /**
     * Device-scoped store, EXCLUDED from Android backup (see res/xml backup rules).
     * Holds the anonymous_install_id so it is regenerated on a fresh install /
     * new device rather than restored from the cloud.
     */
    @Provides @Singleton @Named("device")
    fun deviceDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { ctx.preferencesDataStoreFile("ascend_device") }
}
