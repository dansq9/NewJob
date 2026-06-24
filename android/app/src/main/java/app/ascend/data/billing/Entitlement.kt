package app.ascend.data.billing

enum class Tier { FREE, PRO }

/**
 * Cached entitlement. Pro = no ads + Interview Navigator (live copilot) + higher
 * AI usage limits. The source of truth is Google Play Billing; this is the local
 * cache that gates the UI.
 */
data class Entitlement(
    val tier: Tier = Tier.FREE,
    val source: String? = null,          // e.g. play product id
    val expiryEpochMs: Long? = null,
    /**
     * Whether the entitlement has been resolved against Play Billing. False =
     * `entitlement_unknown` (fresh install/cache-cleared/billing not yet connected):
     * the app shows NO forced ads until a restore/query resolves it (spec IAP states).
     */
    val resolved: Boolean = true,
) {
    val isPro: Boolean get() = tier == Tier.PRO
    /** True until Play Billing resolves the real state — suppress forced ads while unknown. */
    val isUnknown: Boolean get() = !resolved
}
