package com.peterlaurence.trekme.core.billing.domain.model

import java.util.*


data class SubscriptionDetails(val id: UUID, val price: String, val trialDurationInDays: Int)
