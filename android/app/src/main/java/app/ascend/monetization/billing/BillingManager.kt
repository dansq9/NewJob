package app.ascend.monetization.billing

/** A purchasable subscription plan (mapped from Play Console product/base-plan). */
data class SubPlan(
    val productId: String,
    val title: String,
    val price: String,      // formatted, e.g. "$9.99"
    val period: String,     // "month", "year"
    val highlight: String? = null,
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
}
