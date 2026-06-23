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
) {
    val isPro: Boolean get() = tier == Tier.PRO
}
