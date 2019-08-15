package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.Billing
import kotlinx.coroutines.launch

class IgnLicenseViewModel : ViewModel() {
    private val ignLicenseStatus = MutableLiveData<Boolean>()
    private val ignLicenseDetails = MutableLiveData<IgnLicenseDetails>()

    fun getIgnLicensePurchaseStatus(billing: Billing) {
        viewModelScope.launch {
            billing.getIgnLicensePurchaseStatus().also {
                ignLicenseStatus.postValue(it)
            }
        }
    }

    fun getIgnLicenseInfo(billing: Billing) {
        viewModelScope.launch {
            val licenseDetails = billing.getIgnLicenseDetails()
            ignLicenseDetails.postValue(licenseDetails)
        }
    }

    fun getIgnLicenseStatus(): LiveData<Boolean> {
        return ignLicenseStatus
    }

    fun getIgnLicenseDetails(): LiveData<IgnLicenseDetails> {
        return ignLicenseDetails
    }
}

data class IgnLicenseDetails(val price: String)
class NotSupportedException : Exception()
class ProductNotFoundException : Exception()