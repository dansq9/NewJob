package app.ascend.monetization.billing

import app.ascend.data.billing.Entitlement
import app.ascend.data.billing.EntitlementRepository
import app.ascend.data.billing.Tier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dev/stub billing. Grants Pro locally so the paywall, gating and "no ads when
 * Pro" can be exercised before Play Console products + app signing exist.
 *
 * Replace the Hilt binding with PlayBillingManager (Google Play Billing) once
 * subscription products are live. See MonetizationModule.
 */
@Singleton
class StubBillingManager @Inject constructor(
    private val entitlements: EntitlementRepository,
) : BillingManager {

    override suspend fun plans(): List<SubPlan> = listOf(
        SubPlan("ascend_pro_weekly", "Ascend Pro", "$4.99", "week", "Free 3-day trial"),
        SubPlan("ascend_pro_yearly", "Ascend Pro", "$59.99", "year", "Best value"),
    )

    override suspend fun subscribe(productId: String): Boolean {
        entitlements.set(Entitlement(tier = Tier.PRO, source = productId))
        return true
    }

    override suspend fun restore(): Boolean = false
}
