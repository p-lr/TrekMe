package com.peterlaurence.trekme.core.billing.data.api.components

import com.peterlaurence.trekme.core.billing.domain.model.*
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val gracePeriodDays = 15 // number of days the user is allowed to use the app despite expired license
const val p = 350 // validity duration in days (365 - 15)

class AnnualWithGracePeriodVerifier : PurchaseVerifier {
    /**
     * The billing API uses a purchase time in milliseconds since the epoch (Jan 1, 1970), which is
     * exactly the same as what we get with [Date.getTime].
     * So we obtain the current time in millis and convert the difference with the purchase time in
     * days.
     */
    override fun checkTime(timeMillis: Long): AccessState {
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
}
