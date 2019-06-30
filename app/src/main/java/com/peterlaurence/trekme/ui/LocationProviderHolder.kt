package com.peterlaurence.trekme.ui

import com.peterlaurence.trekme.viewmodel.common.LocationProvider

internal interface LocationProviderHolder {
    val locationProvider: LocationProvider
}