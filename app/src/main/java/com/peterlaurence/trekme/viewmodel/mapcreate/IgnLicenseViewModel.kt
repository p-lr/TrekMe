package com.peterlaurence.trekme.viewmodel.mapcreate

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.SkuDetails
import com.peterlaurence.trekme.billing.ign.Billing
import com.peterlaurence.trekme.billing.ign.LicenseInfo
import com.peterlaurence.trekme.billing.ign.PersistenceStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IgnLicenseViewModel : ViewModel() {
    private val ignLicenseStatus = MutableLiveData<Boolean>()
    private val ignLicenseDetails = MutableLiveData<IgnLicenseDetails>()

    fun getIgnLicensePurchaseStatus(billing: Billing) {
        viewModelScope.launch {
            billing.getIgnLicensePurchaseStatus().also {
                ignLicenseStatus.postValue(it)

                /* Update the license status */
                persistLicense(billing.context, LicenseInfo(it))
            }
        }
    }

    fun getIgnLicenseInfo(billing: Billing) {
        viewModelScope.launch {
            try {
                val licenseDetails = billing.getIgnLicenseDetails()
                ignLicenseDetails.postValue(licenseDetails)
            } catch (e: ProductNotFoundException) {
                // something wrong on our side
                ignLicenseStatus.postValue(true)
            } catch (e: IllegalStateException) {
                // can't check license info, so assume it's not valid
            } catch (e: NotSupportedException) {
                // TODO: alert the user that it can't buy the license and should ask for refund
            }
        }
    }

    fun buyLicense(billing: Billing) {
        val ignLicenseDetails = ignLicenseDetails.value
        if (ignLicenseDetails != null) {
            billing.launchBilling(ignLicenseDetails.skuDetails) {
                /* It's assumed that if this is called, it's a success */
                ignLicenseStatus.postValue(true)

                /* Remember that the license was bought */
                persistLicense(billing.context, LicenseInfo(true))
            }
        }
    }

    fun getIgnLicenseStatus(): LiveData<Boolean> {
        return ignLicenseStatus
    }

    fun getIgnLicenseDetails(): LiveData<IgnLicenseDetails> {
        return ignLicenseDetails
    }

    /**
     * Persist licensing info in a custom way, as we need a consistent way to
     * check the license while being offline. Indeed, even if a check using the cache of
     * the play store can help, the user could have cleared the cache and then there's
     * no remote way to do the necessary verifications. So delegate to a more secure
     * persistence */
    private fun persistLicense(context: Context, licenseInfo: LicenseInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val persistenceStrategy = PersistenceStrategy(context)
            persistenceStrategy.persist(licenseInfo)
        }
    }
}

data class IgnLicenseDetails(val skuDetails: SkuDetails) {
    val price: String
        get() = skuDetails.price
}

class NotSupportedException : Exception()
class ProductNotFoundException : Exception()