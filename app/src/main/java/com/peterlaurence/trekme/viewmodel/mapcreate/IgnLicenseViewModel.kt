package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.Billing
import kotlinx.coroutines.launch

class IgnLicenseViewModel : ViewModel() {
    private val ignLicenseStatus = MutableLiveData<Boolean>()

    fun getIgnLicensePurchaseStatus(billing: Billing) {
        viewModelScope.launch {
            billing.getIgnLicensePurchaseStatus().also {
                println("IGN license purchase status: $it")
            }
        }
    }

    fun getIgnLicenseInfo(billing: Billing) {
        viewModelScope.launch {
            val licenseDetails = billing.getIgnLicenseDetails()
            println("license details")
            println(licenseDetails)

            ignLicenseStatus.postValue(true)
        }
    }

    fun getIgnLicenseStatus(): LiveData<Boolean> {
        return ignLicenseStatus
    }
}

data class IgnLicenseDetails(val price: String)
class NotSupportedException : Exception()
class ProductNotFoundException : Exception()