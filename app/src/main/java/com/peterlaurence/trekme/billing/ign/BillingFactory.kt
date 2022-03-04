package com.peterlaurence.trekme.billing.ign

import android.app.Application
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.common.AnnualWithGracePeriodVerifier

private const val IGN_ONETIME_SKU = "ign_license"
private const val IGN_SUBSCRIPTION_YEAR_SKU = "ign_license_sub"
private const val IGN_SUBSCRIPTION_MONTH_SKU = "ign_license_sub_monthly"

fun buildIgnBilling(app: Application): Billing {
    return Billing(
        app,
        IGN_ONETIME_SKU,
        listOf(IGN_SUBSCRIPTION_MONTH_SKU, IGN_SUBSCRIPTION_YEAR_SKU),
        AnnualWithGracePeriodVerifier()
    )
}