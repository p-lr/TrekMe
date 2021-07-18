package com.peterlaurence.trekme.viewmodel.mapcreate

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.BillingParams
import com.peterlaurence.trekme.billing.SubscriptionDetails
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.billing.ign.*
import com.peterlaurence.trekme.di.IGN
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class IgnLicenseViewModel @Inject constructor(
        @IGN private val billing: Billing,
        private val persistenceStrategy: PersistenceStrategy
) : ViewModel() {
    private val ignLicenseStatus = MutableLiveData<PurchaseState>()
    private val ignSubscriptionDetails = MutableLiveData<SubscriptionDetails>()

    fun getIgnLicensePurchaseStatus() {
        viewModelScope.launch {
            /* Indicate that the license check is pending */
            ignLicenseStatus.value = PurchaseState.CHECK_PENDING

            /* Check if we just need to acknowledge the purchase */
            val ackDone = billing.acknowledgePurchase(this@IgnLicenseViewModel::onPurchaseAcknowledged)

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
            try {
                val licenseDetails = billing.getSubDetails()
                ignSubscriptionDetails.postValue(licenseDetails)
            } catch (e: ProductNotFoundException) {
                // Something wrong on our side
                ignLicenseStatus.postValue(PurchaseState.PURCHASED)
            } catch (e: IllegalStateException) {
                // Can't check license info
                Log.e(TAG, e.message ?: Log.getStackTraceString(e))
                ignLicenseStatus.postValue(PurchaseState.UNKNOWN)
            } catch (e: NotSupportedException) {
                // TODO: alert the user that it can't buy the license and should ask for refund
            }
        }
    }

    fun buyLicense(): BillingParams? {
        val ignLicenseDetails = ignSubscriptionDetails.value
        return if (ignLicenseDetails != null) {
            billing.launchBilling(ignLicenseDetails.skuDetails, this::onPurchaseAcknowledged, this::onPurchasePending)
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

        /* Remember when (it's not exact but it doesn't matter) the license was bought */
        persistLicense(LicenseInfo(Date().time))
    }

    fun getIgnLicenseStatus(): LiveData<PurchaseState> {
        return ignLicenseStatus
    }

    fun getSubscriptionDetails(): LiveData<SubscriptionDetails> {
        return ignSubscriptionDetails
    }

    /**
     * Persist licensing info in a custom way, as we need a relatively reliable way to
     * check the license while being offline. Indeed, even if a check using the cache of
     * the play store can help, the user could have cleared the cache and then there's
     * no remote way to do the necessary verifications. So the persistence of the license buy
     * date permits an alternate way of checking. */
    private fun persistLicense(licenseInfo: LicenseInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            persistenceStrategy.persist(licenseInfo)
        }
    }
}

class NotSupportedException : Exception()
class ProductNotFoundException : Exception()

private const val TAG = "IgnLicenceViewModel"