package com.peterlaurence.trekme.core.billing.domain.model

interface PurchaseVerifier {
    fun checkTime(timeMillis: Long): AccessState
}

sealed class AccessState
data class AccessGranted(val remainingDays: Int) : AccessState()
data class GracePeriod(val remainingDays: Int) : AccessState()
object AccessDeniedLicenseOutdated : AccessState()