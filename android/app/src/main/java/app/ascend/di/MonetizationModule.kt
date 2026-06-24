package app.ascend.di

import app.ascend.BuildConfig
import app.ascend.monetization.ads.AdMobAdsManager
import app.ascend.monetization.ads.AdsManager
import app.ascend.monetization.ads.NoopAdsManager
import app.ascend.monetization.billing.BillingManager
import app.ascend.monetization.billing.StubBillingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Monetization bindings. The ads binding is chosen at build time by [BuildConfig.USE_REAL_ADS]:
 * the real [AdMobAdsManager] (debug uses Google test units) or the [NoopAdsManager] fallback for
 * an ad-free local build. Either way every ad DECISION still flows through MonetizationManager.
 *
 * Swap [StubBillingManager] for the real Play Billing implementation once Play Console products
 * are configured.
 */
@Module
@InstallIn(SingletonComponent::class)
object MonetizationModule {
    @Provides @Singleton
    fun billingManager(impl: StubBillingManager): BillingManager = impl

    @Provides @Singleton
    fun adsManager(real: Provider<AdMobAdsManager>, noop: Provider<NoopAdsManager>): AdsManager =
        if (BuildConfig.USE_REAL_ADS) real.get() else noop.get()
}
