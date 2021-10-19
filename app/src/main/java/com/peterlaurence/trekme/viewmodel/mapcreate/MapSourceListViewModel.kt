package com.peterlaurence.trekme.viewmodel.mapcreate

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.repositories.mapcreate.WmtsSourceRepository
import com.peterlaurence.trekme.util.isEnglish
import com.peterlaurence.trekme.util.isFrench
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class MapSourceListViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val wmtsSourceRepository: WmtsSourceRepository,
    mapLoader: MapLoader
): ViewModel() {
    val sourceList = mutableStateOf<List<WmtsSource>>(listOf())
    val showOnBoarding = mutableStateOf(mapLoader.maps.isEmpty())

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