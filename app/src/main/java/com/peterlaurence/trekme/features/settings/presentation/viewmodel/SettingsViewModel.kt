package com.peterlaurence.trekme.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.billing.domain.interactors.HasOneExtendedOfferInteractor
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.util.map as mapStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    trekMeContext: TrekMeContext,
    private val settings: Settings,
    hasOneExtendedOfferInteractor: HasOneExtendedOfferInteractor,
    mapFeatureEvents: MapFeatureEvents
) : ViewModel() {
    val appDirListFlow: StateFlow<List<String>> = trekMeContext.rootDirListFlow.mapStateFlow { folders ->
        folders.map { it.absolutePath }
    }
    val appDirFlow: Flow<String?> = settings.getAppDir().map { it?.absolutePath }
    val startOnPolicyFlow: Flow<StartOnPolicy> = settings.getStartOnPolicy()
    val measurementSystemFlow: Flow<MeasurementSystem> = settings.getMeasurementSystem()
    val maxScaleFlow: Flow<Float> = settings.getMaxScale()
    val magnifyingFactorFlow: Flow<Int> = settings.getMagnifyingFactor()
    val rotationModeFlow = settings.getRotationMode()
    val defineScaleCenteredFlow: Flow<Boolean> = settings.getDefineScaleCentered()
    val scaleRatioCenteredFlow: Flow<Float> = settings.getScaleRatioCentered()
    val showScaleIndicatorFlow: Flow<Boolean> = settings.getShowScaleIndicator()
    val trackFollowThreshold: Flow<Int> = settings.getTrackFollowThreshold()
    val purchaseFlow: StateFlow<Boolean> =
        hasOneExtendedOfferInteractor.getPurchaseFlow(viewModelScope)
    val currentZoom = MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch {
            mapFeatureEvents.mapScaleFlow.collect { scale ->
                val maxScale = maxScaleFlow.first()
                currentZoom.value = if (scale != null) (scale * 100 / maxScale).toInt() else null
            }
        }
    }

    fun setDownloadDirPath(newPath: String) = viewModelScope.launch {
        settings.setAppDir(File(newPath))
    }

    fun setStartOnPolicy(policy: StartOnPolicy) = viewModelScope.launch {
        settings.setStartOnPolicy(policy)
    }

    fun setMeasurementSystem(system: MeasurementSystem) = viewModelScope.launch {
        UnitFormatter.system = system
        settings.setMeasurementSystem(system)
    }

    fun setMaxScale(scale: Float) = viewModelScope.launch {
        settings.setMaxScale(scale)
    }

    fun setMagnifyingFactor(factor: Int) = viewModelScope.launch {
        settings.setMagnifyingFactor(factor)
    }

    fun setRotationMode(mode: RotationMode) = viewModelScope.launch {
        settings.setRotationMode(mode)
    }

    fun setDefineScaleCentered(defined: Boolean) = viewModelScope.launch {
        settings.setDefineScaleCentered(defined)
    }

    fun setShowScaleIndicator(show: Boolean) = viewModelScope.launch {
        settings.setShowScaleIndicator(show)
    }

    /**
     * Define the scale ratio of the MapView when centering on the current position.
     * Value should be between 0f and 100f.
     * The correspondence is a linear interpolation between:
     *    100 -> maxScale
     *    0   -> 0f
     */
    fun setScaleRatioCentered(percent: Float) = viewModelScope.launch {
        require(percent in 0f..100f)
        settings.setScaleRatioCentered(percent)
    }

    fun setTrackFollowThreshold(valueInMeters: Int) = viewModelScope.launch {
        settings.setTrackFollowThreshold(valueInMeters)
    }
}