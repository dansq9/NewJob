package app.ascend.monetization.billing

/** A purchasable subscription plan (mapped from Play Console product/base-plan). */
data class SubPlan(
    val productId: String,
    val title: String,
    val price: String,            // localized formatted string from ProductDetails (e.g. "₹799")
    val period: String,           // "month", "year"
    val highlight: String? = null,
    // Structured pricing straight from ProductDetails — the REAL local figures the
    // `purchase` event reports (never hardcoded/pre-converted USD, rule 7).
    val priceMicros: Long = 0L,   // ProductDetails…priceAmountMicros
    val currencyCode: String = "", // ProductDetails…priceCurrencyCode (ISO 4217)
    val productType: String = "",  // weekly | yearly | lifetime
)

/**
 * Abstraction over Google Play Billing. The real implementation
 * (PlayBillingManager) wraps BillingClient; [StubBillingManager] is used until
 * Play Console subscription products + signing are configured.
 *
 * Pro entitlement (no ads + Interview Navigator) is updated via EntitlementRepository.
 */
interface BillingManager {
    /** Plans to render on the paywall. */
    suspend fun plans(): List<SubPlan>

    /** Launch the purchase flow; returns true if the user became Pro. */
    suspend fun subscribe(productId: String): Boolean

    /** Re-query Play purchases and refresh entitlement; true if Pro found. */
    suspend fun restore(): Boolean

    /**
     * Connect to Play Billing and resolve the entitlement at startup. Until this
     * completes the entitlement is `entitlement_unknown` and NO forced ads show.
     * Call once per cold start.
     */
    suspend fun syncEntitlement()
}
