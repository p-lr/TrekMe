package com.peterlaurence.trekme.viewmodel.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import java.io.File

class SettingsViewModel @ViewModelInject constructor(
        trekMeContext: TrekMeContext,
        private val settings: Settings
) : ViewModel() {
    private val _appDirListLiveData = MutableLiveData<List<String>>()
    val appDirListLiveData: LiveData<List<String>> = _appDirListLiveData
    private val _appDirLiveData = MutableLiveData<String>()
    val appDirLiveData: LiveData<String> = _appDirLiveData
    private val _startOnPolicyLiveData = MutableLiveData<StartOnPolicy>()
    val startOnPolicyLiveData: LiveData<StartOnPolicy> = _startOnPolicyLiveData
    private val _maxScaleLiveData = MutableLiveData<Float>()
    val maxScaleLiveData: LiveData<Float> = _maxScaleLiveData
    private val _magnifyingFactorLiveData = MutableLiveData<Int>()
    val magnifyingFactorLiveData: LiveData<Int> = _magnifyingFactorLiveData
    private val _rotationModeLiveData = MutableLiveData<RotationMode>()
    val rotationModeLiveData = _rotationModeLiveData
    private val _defineScaleCentered = MutableLiveData<Boolean>()
    val defineScaleCentered: LiveData<Boolean> = _defineScaleCentered
    private val _scaleCentered = MutableLiveData<Float>()
    val scaleCentered: LiveData<Float> = _scaleCentered
    private val _measurementSystemLiveData = MutableLiveData<MeasurementSystem>()
    val measurementSystemLiveData: LiveData<MeasurementSystem> = _measurementSystemLiveData

    /* For instance the need is only to fetch settings once */
    init {
        /* App dir list */
        _appDirListLiveData.value = trekMeContext.mapsDirList?.map { it.absolutePath }
                ?: emptyList()

        /* App dir active */
        _appDirLiveData.value = settings.getAppDir()?.absolutePath ?: "error"

        /* StartOn policy */
        _startOnPolicyLiveData.value = settings.getStartOnPolicy()

        /* Max scale */
        _maxScaleLiveData.value = settings.getMaxScale()

        /* Magnifying factor */
        _magnifyingFactorLiveData.value = settings.getMagnifyingFactor()

        /* Rotation mode */
        _rotationModeLiveData.value = settings.getRotationMode()

        /* Define scale centered */
        _defineScaleCentered.value = settings.getDefineScaleCentered()

        /* Scale centered */
        _scaleCentered.value = settings.getScaleRatioCentered()

        /* Measurement system */
        _measurementSystemLiveData.value = settings.getMeasurementSystem()
    }

    fun setDownloadDirPath(newPath: String) {
        _appDirLiveData.postValue(newPath)
        settings.setAppDir(File(newPath))
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        _startOnPolicyLiveData.postValue(policy)
        settings.setStartOnPolicy(policy)
    }

    fun setMeasurementSystem(system: MeasurementSystem) {
        UnitFormatter.system = system
        _measurementSystemLiveData.postValue(system)
        settings.setMeasurementSystem(system)
    }

    fun setMaxScale(scale: Float) {
        _maxScaleLiveData.postValue(scale)
        settings.setMaxScale(scale)
    }

    fun setMagnifyingFactor(factor: Int) {
        _magnifyingFactorLiveData.postValue(factor)
        settings.setMagnifyingFactor(factor)
    }

    fun setRotationMode(mode: RotationMode) {
        _rotationModeLiveData.postValue(mode)
        settings.setRotationMode(mode)
    }

    fun setDefineScaleCentered(defined: Boolean) {
        settings.setDefineScaleCentered(defined)
    }

    /**
     * Define the scale ratio of the MapView when centering on the current position.
     * Value should be between 0f and 100f.
     * The correspondence is a linear interpolation between:
     *    100 -> maxScale
     *    0   -> 0f
     */
    fun setScaleRatioCentered(percent: Float) {
        require(percent in 0f..100f)
        settings.setScaleRatioCentered(percent)
    }
}