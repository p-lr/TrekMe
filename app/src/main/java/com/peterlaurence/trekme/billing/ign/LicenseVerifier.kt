package com.peterlaurence.trekme.billing.ign

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val gracePeriodDays = 15
const val p = 365 // validity duration in days

/**
 * The billing API uses a purchase time in milliseconds since the epoch (Jan 1, 1970), which is
 * exactly the same as what we get with [Date.getTime].
 * So we obtain the current time in millis and convert the difference with the purchase time in
 * days. If the purchase is older than a year (365 days) or
 */
fun checkTime(timeMillis: Long): AccessState {
    val now = Date().time
    val millis = now - timeMillis
    val days = TimeUnit.DAYS.convert(abs(millis), TimeUnit.MILLISECONDS)

    return if (millis > 0) {
        if (days <= p) {
            AccessGranted((p - days).toInt())
        } else if (days > p && days < (p + gracePeriodDays)) {
            GracePeriod((p + gracePeriodDays - days).toInt())
        } else {
            AccessDeniedLicenseOutdated
        }
    } else {
        AccessGranted((p + days).toInt())    // purchase happened "in the future"
    }
}

sealed class AccessState
data class AccessGranted(val remainingDays: Int) : AccessState()
data class GracePeriod(val remainingDays: Int) : AccessState()
object AccessDeniedLicenseOutdated : AccessState()