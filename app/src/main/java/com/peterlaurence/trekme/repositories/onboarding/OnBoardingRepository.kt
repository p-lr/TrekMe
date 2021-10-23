package com.peterlaurence.trekme.repositories.onboarding

class OnBoardingRepository {
    var mapCreateOnBoarding: Boolean = false
        private set

    fun setMapCreateOnBoarding(flag: Boolean) {
        mapCreateOnBoarding = flag
    }
}