package com.peterlaurence.trekme.billing.gpspro

import android.app.Application
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.common.AnnualWithGracePeriodVerifier

private const val ONETIME_SKU = "gps_pro"
private const val SUBSCRIPTION_SKU = "gps_pro_sub"

fun buildGpsProBilling(app: Application): Billing {
    return Billing(app, ONETIME_SKU, listOf(SUBSCRIPTION_SKU), AnnualWithGracePeriodVerifier())
}