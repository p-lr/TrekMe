package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

/**
 * The view-model for displaying track statistics in the MapView fragment.
 *
 * @author P.Laurence on 01/05/20
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    gpxRecordEvents: GpxRecordEvents
) : ViewModel() {
    /* In this context, a null value means that statistics shouldn't be displayed - the view should
     * reflect this appropriately */
    val stats: SharedFlow<GeoStatistics?> = gpxRecordEvents.geoStatisticsEvent
}