package com.peterlaurence.trekme.core.billing.data.api.factories

import android.app.Application
import com.peterlaurence.trekme.core.billing.data.api.Billing
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.events.AppEventBus

private const val IGN_ONETIME_SKU = "ign_license"
private const val IGN_SUBSCRIPTION_YEAR_SKU = "ign_license_sub"
private const val IGN_SUBSCRIPTION_MONTH_SKU = "ign_license_sub_monthly"

fun buildIgnBilling(app: Application, appEventBus: AppEventBus): BillingApi {
    return Billing(
        app,
        IGN_ONETIME_SKU,
        listOf(IGN_SUBSCRIPTION_MONTH_SKU, IGN_SUBSCRIPTION_YEAR_SKU),
        AnnualWithGracePeriodVerifier(),
        appEventBus
    )
}