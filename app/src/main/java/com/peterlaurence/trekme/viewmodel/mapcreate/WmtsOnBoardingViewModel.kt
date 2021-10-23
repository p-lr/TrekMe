package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.repositories.onboarding.OnBoardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WmtsOnBoardingViewModel @Inject constructor(
    private val onBoardingRepository: OnBoardingRepository
) : ViewModel() {
    val onBoardingState =
        mutableStateOf<OnBoardingState>(ShowTip(fabTip = false, centerOnPosTip = true))

    fun onCenterOnPosTipAck() {
        onBoardingState.value = ShowTip(fabTip = true, centerOnPosTip = false)
    }

    fun onFabTipAck() {
        onBoardingState.value = Hidden
        onBoardingRepository.setMapCreateOnBoarding(false)
    }
}

sealed interface OnBoardingState
data class ShowTip(val fabTip: Boolean, val centerOnPosTip: Boolean) : OnBoardingState
object Hidden : OnBoardingState