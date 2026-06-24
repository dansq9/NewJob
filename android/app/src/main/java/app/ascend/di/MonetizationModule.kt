package app.ascend.di

import app.ascend.monetization.ads.AdsManager
import app.ascend.monetization.ads.NoopAdsManager
import app.ascend.monetization.billing.BillingManager
import app.ascend.monetization.billing.StubBillingManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Monetization bindings. Swap these to PlayBillingManager / Google Mobile Ads
 * implementations once Play Console products + AdMob unit IDs are configured.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MonetizationModule {
    @Binds @Singleton
    abstract fun billingManager(impl: StubBillingManager): BillingManager

    @Binds @Singleton
    abstract fun adsManager(impl: NoopAdsManager): AdsManager
}
