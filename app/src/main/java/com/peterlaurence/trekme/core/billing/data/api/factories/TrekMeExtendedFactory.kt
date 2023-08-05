package com.peterlaurence.trekme.core.billing.data.api.factories

import android.app.Application
import com.peterlaurence.trekme.core.billing.data.api.Billing
import com.peterlaurence.trekme.core.billing.data.api.components.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.events.AppEventBus

private const val TREKME_EXTENDED_ONETIME_SKU = "trekme_extended"
private const val TREKME_EXTENDED_SUBSCRIPTION_YEAR_SKU = "trekme_extended_sub"
private const val TREKME_EXTENDED_SUBSCRIPTION_MONTH_SKU = "trekme_extended_sub_monthly"

fun buildTrekmeExtendedBilling(app: Application, appEventBus: AppEventBus): BillingApi {
    return Billing(
        app,
        TREKME_EXTENDED_ONETIME_SKU,
        listOf(TREKME_EXTENDED_SUBSCRIPTION_MONTH_SKU, TREKME_EXTENDED_SUBSCRIPTION_YEAR_SKU),
        AnnualWithGracePeriodVerifier(),
        appEventBus
    )
}