package com.peterlaurence.trekme.viewmodel.settings

import androidx.lifecycle.*
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
        trekMeContext: TrekMeContext,
        private val settings: Settings
) : ViewModel() {
    private val _appDirListLiveData = MutableLiveData<List<String>>()
    val appDirListLiveData: LiveData<List<String>> = _appDirListLiveData
    val appDirLiveData: LiveData<String> = settings.getAppDir().map {
        it?.absolutePath ?: "error"
    }.asLiveData()
    val startOnPolicyLiveData: LiveData<StartOnPolicy> = settings.getStartOnPolicy().asLiveData()
    val maxScaleLiveData: LiveData<Float> = settings.getMaxScale().asLiveData()
    val magnifyingFactorLiveData: LiveData<Int> = settings.getMagnifyingFactor().asLiveData()
    val rotationModeLiveData = settings.getRotationMode().asLiveData()
    val defineScaleCentered: LiveData<Boolean> = settings.getDefineScaleCentered().asLiveData()
    val scaleRatioCentered: LiveData<Float> = settings.getScaleRatioCentered().asLiveData()
    val measurementSystemLiveData: LiveData<MeasurementSystem> = settings.getMeasurementSystem().asLiveData()

    init {
        /* App dir list */
        _appDirListLiveData.value = trekMeContext.mapsDirList?.map { it.absolutePath }
                ?: emptyList()
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
}