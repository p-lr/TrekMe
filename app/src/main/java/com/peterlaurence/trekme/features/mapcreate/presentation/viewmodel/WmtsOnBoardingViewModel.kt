package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.features.common.domain.repositories.OnBoardingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WmtsOnBoardingViewModel @Inject constructor(
    private val onBoardingRepository: OnBoardingRepository
) : ViewModel() {
    val onBoardingState =
        mutableStateOf(
            if (onBoardingRepository.mapCreateOnBoarding) {
                ShowTip(fabTip = false, centerOnPosTip = true)
            } else Hidden
        )

    fun onCenterOnPosTipAck() {
        if (onBoardingRepository.mapCreateOnBoarding) {
            onBoardingState.value = ShowTip(fabTip = true, centerOnPosTip = false)
        }
    }

    fun onFabTipAck() {
        onBoardingState.value = Hidden
        onBoardingRepository.setMapCreateOnBoarding(false)
    }
}

sealed interface OnBoardingState
data class ShowTip(val fabTip: Boolean, val centerOnPosTip: Boolean) : OnBoardingState
object Hidden : OnBoardingState