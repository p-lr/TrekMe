package com.peterlaurence.trekme.features.common.domain.repositories

class OnBoardingRepository {
    var mapCreateOnBoarding: Boolean = false
        private set

    fun setMapCreateOnBoarding(flag: Boolean) {
        mapCreateOnBoarding = flag
    }
}