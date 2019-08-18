package com.peterlaurence.trekme.billing.ign

import java.util.*
import java.util.concurrent.TimeUnit

/**
 * The billing API uses a purchase time in milliseconds since the epoch (Jan 1, 1970), which is
 * exactly the same as what we get with [Date.getTime].
 * So we obtain the current time in millis and convert the difference with the purchase time in
 * days. If the purchase is older than a year (365 days) or
 */
fun checkTime(timeMillis: Long): Boolean {
    val now = Date().time
    val millis = now - timeMillis
    return if (millis > 0) {
        TimeUnit.DAYS.convert(millis, TimeUnit.MILLISECONDS) <= 365
    } else {
        true    // purchase happened "in the future"
    }
}