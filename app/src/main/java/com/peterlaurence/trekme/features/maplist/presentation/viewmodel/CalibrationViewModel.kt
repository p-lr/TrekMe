package com.peterlaurence.trekme.features.maplist.presentation.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.common.domain.interactors.MapComposeTileStreamProviderInteractor
import com.peterlaurence.trekme.features.maplist.domain.interactors.CalibrationInteractor
import com.peterlaurence.trekme.features.maplist.domain.model.CalibrationData
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.CalibrationMarker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.layout.Fill
import ovh.plrapps.mapcompose.ui.state.MapState
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val calibrationInteractor: CalibrationInteractor,
    private val mapComposeTileStreamProviderInteractor: MapComposeTileStreamProviderInteractor
) : ViewModel() {
    private var mapState: MapState? = null
    private var calibrationPoints: List<CalibrationPointModel>? = null

    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val acknowledgeableEvents = mutableStateListOf<AcknowledgeableEvent>()

    init {
        mapRepository.settingsMapFlow.map {
            if (it != null) {
                onMapChange(it)
            }
        }.launchIn(viewModelScope)
    }

    fun onPointSelectionChange(pointId: PointId) {
        val mapUiState = uiState.value as? MapUiState ?: return
        mapUiState.selected.value = pointId

        val calibrationPoints = this.calibrationPoints ?: return
        val newPoint = calibrationPoints.getOrNull(pointId.index) ?: return

        mapUiState.mapState.moveMarker(calibrationMarkerId, newPoint.x, newPoint.y)
        viewModelScope.launch {
            mapUiState.mapState.centerOnMarker(calibrationMarkerId, destScale = 2f)
        }
    }

    fun onSave(pointId: PointId) {
        val mapState = this.mapState ?: return
        val calibrationPoints = this.calibrationPoints ?: return
        val currentPoint = calibrationPoints.getOrNull(pointId.index) ?: return

        /* Update the relevant marker using the current marker position */
        mapState.getMarkerInfo(calibrationMarkerId)?.also {
            currentPoint.x = it.x
            currentPoint.y = it.y
        }

        /* Prepare the calibration data */
        val map = mapRepository.settingsMapFlow.value ?: return
        val data = calibrationPoints.map {
            val lat = it.lat.toDoubleOrNull() ?: 0.0
            val lon = it.lon.toDoubleOrNull() ?: 0.0
            CalibrationData(lat, lon, it.x, it.y)
        }

        viewModelScope.launch {
            calibrationInteractor.updateCalibration(data, map).also { ok ->
                if (!ok) {
                    acknowledgeableEvents.add(CalibrationError)
                } else {
                    acknowledgeableEvents.add(CalibrationPointSaved)
                }
            }
        }
    }

    fun acknowledgeEvent() {
        acknowledgeableEvents.removeFirstOrNull()
    }

    private suspend fun onMapChange(map: Map) {
        /* Shutdown the previous map state, if any */
        mapState?.shutdown()

        /* For instance, MapCompose only supports levels of uniform tile size (and squared) */
        val tileSize = map.levelList.firstOrNull()?.tileSize?.width ?: run {
            _uiState.value = EmptyMap
            return
        }

        val tileStreamProvider = mapComposeTileStreamProviderInteractor.makeTileStreamProvider(map)

        val mapState = MapState(
            map.levelList.size,
            map.widthPx,
            map.heightPx,
            tileSize
        ) {
            highFidelityColors(false)
            minimumScaleMode(Fill)
            scale(2f)
        }.apply {
            addLayer(tileStreamProvider)
        }

        this.mapState = mapState

        /* Calibration points */
        val calibrationPoints = (0 until map.calibrationPointsNumber).mapNotNull { index ->
            val pt = map.calibrationPoints.getOrNull(index)
            if (pt != null) {
                val latLon = calibrationInteractor.getLatLonForCalibrationPoint(pt, map)
                if (latLon != null) {
                    CalibrationPointModel(pt.normalizedX, pt.normalizedY, latLon.lat.toString(), latLon.lon.toString())
                } else {
                    createCalibrationPointFromIndex(index)
                }
            } else {
                createCalibrationPointFromIndex(index)
            }
        }

        this.calibrationPoints = calibrationPoints

        /* Configure the calibration marker behavior */
        calibrationPoints.firstOrNull()?.also { firstPoint ->
            mapState.addMarker(
                calibrationMarkerId,
                firstPoint.x,
                firstPoint.y,
                relativeOffset = Offset(-0.5f, -0.5f)
            ) {
                CalibrationMarker()
            }
            mapState.enableMarkerDrag(calibrationMarkerId)
            viewModelScope.launch {
                mapState.centerOnMarker(calibrationMarkerId)
            }
        }

        _uiState.value = MapUiState(
            mapState,
            calibrationPoints,
            calibrationMethod = map.calibrationMethod
        )
    }

    private fun createCalibrationPointFromIndex(index: Int): CalibrationPointModel? {
        return when (index) {
            0 -> CalibrationPointModel(0.0, 0.0, "", "")
            1 -> CalibrationPointModel(1.0, 1.0, "", "")
            2 -> CalibrationPointModel(1.0, 0.0, "", "")
            3 -> CalibrationPointModel(0.0, 1.0, "", "")
            else -> null
        }
    }
}

private const val calibrationMarkerId = "calibration_marker_id"

enum class PointId(val index: Int) {
    One(0), Two(1), Three(2), Four(3)
}

class CalibrationPointModel(x: Double, y: Double, lat: String, lon: String) {
    var x by mutableStateOf(x)
    var y by mutableStateOf(y)
    var lat by mutableStateOf(lat)
    var lon by mutableStateOf(lon)
}

sealed interface AcknowledgeableEvent
object CalibrationPointSaved : AcknowledgeableEvent
object CalibrationError : AcknowledgeableEvent


sealed interface UiState
object Loading : UiState
object EmptyMap : UiState
data class MapUiState(
    val mapState: MapState,
    val calibrationPoints: List<CalibrationPointModel>,
    val selected: MutableState<PointId> = mutableStateOf(PointId.One),
    val calibrationMethod: CalibrationMethod
) : UiState