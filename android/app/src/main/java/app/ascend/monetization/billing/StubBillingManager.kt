package app.ascend.monetization.billing

import app.ascend.analytics.AnalyticsTracker
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
 *
 * The PLACEHOLDER prices below stand in for `ProductDetails`; the real impl reads
 * `priceAmountMicros` + `priceCurrencyCode` (localized) so the `purchase` event
 * always reports the true local value+currency, never a hardcoded USD figure.
 */
@Singleton
class StubBillingManager @Inject constructor(
    private val entitlements: EntitlementRepository,
    private val analytics: AnalyticsTracker,
) : BillingManager {

    // Single weekly subscription. Product id + price are PLACEHOLDERS (Play Console TBD) — the
    // real PlayBillingManager reads the localized price/currency from ProductDetails at runtime.
    override suspend fun plans(): List<SubPlan> = listOf(
        SubPlan("ascend_pro_weekly", "Ascend Pro", "$4.99", "week", "Free 3-day trial",
            priceMicros = 4_990_000L, currencyCode = "USD", productType = "weekly"),
    )

    override suspend fun subscribe(productId: String): Boolean {
        entitlements.set(Entitlement(tier = Tier.PRO, source = productId))
        // Log the manual purchase with the plan's structured value+currency. The real
        // impl supplies the Play order id; the stub derives a deterministic dev id so
        // dedupe-on-transaction_id is exercised.
        val plan = plans().firstOrNull { it.productId == productId }
        if (plan != null) {
            analytics.purchase(
                valueMicros = plan.priceMicros,
                currency = plan.currencyCode,
                productId = plan.productId,
                productType = plan.productType,
                trial = plan.highlight?.contains("trial", ignoreCase = true) == true,
                isRenewal = false,
                transactionId = "stub-$productId",
            )
        }
        return true
    }

    override suspend fun restore(): Boolean {
        // No real purchases to restore in the stub; resolve so forced ads can run.
        entitlements.markResolved()
        return false
    }

    override suspend fun syncEntitlement() {
        // Real impl: BillingClient.queryPurchasesAsync → set the resolved entitlement.
        // Stub: keep the cached tier and mark it resolved (clears entitlement_unknown).
        entitlements.markResolved()
    }
}
