package com.peterlaurence.trekme.core.billing.domain.model

import java.util.*


data class SubscriptionDetails(val id: UUID, val price: String, val trialInfo: TrialInfo)

sealed interface TrialInfo
data class TrialAvailable(val trialDurationInDays: Int): TrialInfo
object TrialUnavailable : TrialInfo
