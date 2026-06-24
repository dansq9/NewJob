package app.ascend.ui.screens.paywall

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ascend.R
import app.ascend.analytics.AnalyticsTracker
import app.ascend.analytics.PaywallVariant
import app.ascend.data.billing.EntitlementRepository
import app.ascend.monetization.billing.BillingManager
import app.ascend.monetization.billing.SubPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billing: BillingManager,
    private val analytics: AnalyticsTracker,
    entitlements: EntitlementRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> =
        entitlements.isPro.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    var plans by mutableStateOf<List<SubPlan>>(emptyList()); private set
    var selected by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    @StringRes var message by mutableStateOf<Int?>(null)

    init {
        // Single-variant paywall for now; A/B variant comes from Remote Config later.
        analytics.paywallView(PaywallVariant.CONTROL, trigger = null)
        viewModelScope.launch {
            plans = billing.plans()
            selected = plans.firstOrNull()?.productId
        }
    }

    fun select(productId: String) { selected = productId }

    fun subscribe(onSuccess: () -> Unit) {
        val id = selected ?: return
        if (busy) return
        busy = true; message = null
        analytics.paywallStartTrialClick(PaywallVariant.CONTROL)
        viewModelScope.launch {
            val ok = runCatching { billing.subscribe(id) }.getOrDefault(false)
            busy = false
            // The real `purchase` event (with actual local value + currency, CLAUDE.md
            // rule 7) is emitted by the billing layer on a confirmed purchase, not here.
            if (ok) onSuccess()
            else message = R.string.paywall_purchase_failed
        }
    }

    fun restore() {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            val ok = runCatching { billing.restore() }.getOrDefault(false)
            busy = false
            message = if (ok) R.string.paywall_restore_success else R.string.paywall_restore_none
        }
    }
}
