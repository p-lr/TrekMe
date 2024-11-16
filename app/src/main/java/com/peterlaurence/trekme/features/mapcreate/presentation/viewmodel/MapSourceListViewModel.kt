package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.domain.repositories.OnBoardingRepository
import com.peterlaurence.trekme.util.checkInternet
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapSourceListViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val wmtsSourceRepository: WmtsSourceRepository,
    onBoardingRepository: OnBoardingRepository
): ViewModel() {
    val sourceList = MutableStateFlow<List<WmtsSource>>(listOf())
    val showOnBoarding = MutableStateFlow(onBoardingRepository.mapCreateOnBoarding)

    private val _events = Channel<Event>(1)
    val events = _events.receiveAsFlow()

    /**
     * When the app is in english, put [WmtsSource.USGS] in front.
     * When in french, put [WmtsSource.IGN] in front.
     */
    init {
        /* Map creation requires internet access */
        viewModelScope.launch {
            if (!checkInternet()) {
                _events.send(Event.NoInternet)
            }
        }

        val wmtsSourceSet = WmtsSource.entries.sortedBy {
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

    fun hideOnboarding() {
        showOnBoarding.value = false
    }

    sealed interface Event {
        data object NoInternet : Event
    }
}