package com.peterlaurence.trekme.core.billing.data.api.factories

import android.app.Application
import com.peterlaurence.trekme.core.billing.data.api.Billing
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.events.AppEventBus

private const val ONETIME_SKU = "gps_pro"
private const val SUBSCRIPTION_SKU = "gps_pro_sub"

fun buildGpsProBilling(app: Application, appEventBus: AppEventBus): BillingApi {
    return Billing(
        app,
        ONETIME_SKU,
        listOf(SUBSCRIPTION_SKU),
        AnnualWithGracePeriodVerifier(),
        appEventBus
    )
}