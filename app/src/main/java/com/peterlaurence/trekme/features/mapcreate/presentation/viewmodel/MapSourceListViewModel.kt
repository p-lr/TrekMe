package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.domain.repositories.OnBoardingRepository
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MapSourceListViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val wmtsSourceRepository: WmtsSourceRepository,
    onBoardingRepository: OnBoardingRepository,
): ViewModel() {
    val sourceList = mutableStateOf<List<WmtsSource>>(listOf())
    val showOnBoarding = mutableStateOf(onBoardingRepository.mapCreateOnBoarding)

    /**
     * When the app is in english, put [WmtsSource.USGS] in front.
     * When in french, put [WmtsSource.IGN] in front.
     */
    init {
        val wmtsSourceSet = WmtsSource.values().sortedBy {
            if (isEnglish(appContext) && it == WmtsSource.USGS) {
                -1
            } else if (isFrench(appContext) && it == WmtsSource.IGN) {
                -1
            } else {
                0
            }
        }

        sourceList.value = wmtsSourceSet
    }

    fun setMapSource(source: WmtsSource) {
        wmtsSourceRepository.setMapSource(source)
    }
}