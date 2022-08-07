package com.peterlaurence.trekme.core.billing.data.model

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams

data class BillingParams(val billingClient: BillingClient, val flowParams: BillingFlowParams)