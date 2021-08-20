package com.peterlaurence.trekme.ui.mapcreate.events

import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.ui.mapcreate.dialogs.DownloadFormDataBundle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MapCreateEventBus {
    private val _layerSelectEvent = MutableSharedFlow<String>(0, 1, BufferOverflow.DROP_OLDEST)
    val layerSelectEvent = _layerSelectEvent.asSharedFlow()

    fun postLayerSelectEvent(layer: String) = _layerSelectEvent.tryEmit(layer)

    private val _placeSelectEvent = MutableSharedFlow<GeoPlace>(0, 1, BufferOverflow.DROP_OLDEST)
    val paceSelectEvent = _placeSelectEvent.asSharedFlow()

    fun postPlaceSelectEvent(place: GeoPlace) = _placeSelectEvent.tryEmit(place)

    private val _showDownloadFormEvent = MutableSharedFlow<DownloadFormDataBundle>(0, 1, BufferOverflow.DROP_OLDEST)
    val showDownloadFormEvent = _showDownloadFormEvent.asSharedFlow()

    fun showDownloadForm(data: DownloadFormDataBundle) = _showDownloadFormEvent.tryEmit(data)
}