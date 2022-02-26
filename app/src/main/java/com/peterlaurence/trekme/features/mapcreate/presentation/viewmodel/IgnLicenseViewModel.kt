package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.*
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.di.IGN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IgnLicenseViewModel @Inject constructor(
    @IGN private val billing: Billing,
) : ViewModel() {
    private val ignLicenseStatus = MutableLiveData<PurchaseState>()
    private val ignSubscriptionDetails = MutableLiveData<SubscriptionDetails>()

    fun getIgnLicensePurchaseStatus() {
        viewModelScope.launch {
            /* Indicate that the license check is pending */
            ignLicenseStatus.value = PurchaseState.CHECK_PENDING

            /* Check if we just need to acknowledge the purchase */
            val ackDone =
                billing.acknowledgePurchase(this@IgnLicenseViewModel::onPurchaseAcknowledged)

            /* Otherwise, do normal checks */
            if (!ackDone) {
                billing.getPurchase().also {
                    ignLicenseStatus.postValue(if (it != null) PurchaseState.PURCHASED else PurchaseState.NOT_PURCHASED)
                }
            }
        }
    }

    fun getIgnLicenseInfo() {
        viewModelScope.launch {
            runCatching {
                val licenseDetails = billing.getSubDetails()
                ignSubscriptionDetails.postValue(licenseDetails)
            }
        }
    }

    fun buyLicense(): BillingParams? {
        val ignLicenseDetails = ignSubscriptionDetails.value
        return if (ignLicenseDetails != null) {
            billing.launchBilling(
                ignLicenseDetails.skuDetails,
                this::onPurchaseAcknowledged,
                this::onPurchasePending
            )
        } else null
    }

    private fun onPurchasePending() {
        ignLicenseStatus.postValue(PurchaseState.PURCHASE_PENDING)
    }

    /**
     * This is the callback called when the IGN module is considered successfully bought.
     */
    private fun onPurchaseAcknowledged() {
        /* It's assumed that if this is called, it's a success */
        ignLicenseStatus.postValue(PurchaseState.PURCHASED)
    }

    fun getIgnLicenseStatus(): LiveData<PurchaseState> {
        return ignLicenseStatus
    }

    fun getSubscriptionDetails(): LiveData<SubscriptionDetails> {
        return ignSubscriptionDetails
    }
}

private const val TAG = "IgnLicenceViewModel"