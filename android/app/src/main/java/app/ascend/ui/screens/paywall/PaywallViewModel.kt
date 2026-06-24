package app.ascend.ui.screens.paywall

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val analytics: app.ascend.analytics.Analytics,
    entitlements: EntitlementRepository,
) : ViewModel() {

    val isPro: StateFlow<Boolean> =
        entitlements.isPro.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    var plans by mutableStateOf<List<SubPlan>>(emptyList()); private set
    var selected by mutableStateOf<String?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null)

    init {
        analytics.log(app.ascend.analytics.Ev.PAYWALL_VIEW)
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
        viewModelScope.launch {
            val ok = runCatching { billing.subscribe(id) }.getOrDefault(false)
            busy = false
            if (ok) { analytics.log(app.ascend.analytics.Ev.SUBSCRIBE, mapOf("product" to id)); onSuccess() }
            else message = "Purchase didn't complete. Please try again."
        }
    }

    fun restore() {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            val ok = runCatching { billing.restore() }.getOrDefault(false)
            busy = false
            message = if (ok) "Pro restored." else "No previous purchases found."
        }
    }
}
