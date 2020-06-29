package com.peterlaurence.trekme.viewmodel.settings

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel @ViewModelInject constructor(
        private val trekMeContext: TrekMeContext,
        private val settings: Settings
): ViewModel() {
    private val _appDirListLiveData = MutableLiveData<List<String>>()
    val appDirListLiveData: LiveData<List<String>> = _appDirListLiveData
    private val _appDirLiveData = MutableLiveData<String>()
    val appDirLiveData: LiveData<String> = _appDirLiveData
    private val _startOnPolicyLiveData = MutableLiveData<StartOnPolicy>()
    val startOnPolicyLiveData: LiveData<StartOnPolicy> = _startOnPolicyLiveData
    private val _magnifyingFactorLiveData = MutableLiveData<Int>()
    val magnifyingFactorLiveData: LiveData<Int> = _magnifyingFactorLiveData
    private val _rotationModeLiveData = MutableLiveData<RotationMode>()
    val rotationModeLiveData = _rotationModeLiveData
    private val _scaleFactor = MutableLiveData<Float>()
    val scaleCentered: LiveData<Float> = _scaleFactor

    init {
        /* For instance the need is only to fetch this once */
        viewModelScope.launch {
            /* App dir list */
            _appDirListLiveData.postValue(trekMeContext.mapsDirList?.map { it.absolutePath }
                    ?: emptyList())

            /* App dir active */
            _appDirLiveData.postValue(settings.getAppDir()?.absolutePath ?: "error")

            /* StartOn policy */
            _startOnPolicyLiveData.postValue(settings.getStartOnPolicy())

            /* Magnifying factor */
            _magnifyingFactorLiveData.postValue(settings.getMagnifyingFactor())

            /* Rotation mode */
            _rotationModeLiveData.value = settings.getRotationMode()

            /* Scale centered */
            _scaleFactor.value = settings.getScaleCentered()
        }
    }

    fun setDownloadDirPath(newPath: String) {
        _appDirLiveData.postValue(newPath)
        viewModelScope.launch {
            settings.setAppDir(File(newPath))
        }
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        _startOnPolicyLiveData.postValue(policy)
        viewModelScope.launch {
            settings.setStartOnPolicy(policy)
        }
    }

    fun setMagnifyingFactor(factor: Int) {
        _magnifyingFactorLiveData.postValue(factor)
        viewModelScope.launch {
            settings.setMagnifyingFactor(factor)
        }
    }

    fun setRotationMode(mode: RotationMode) {
        _rotationModeLiveData.postValue(mode)
        viewModelScope.launch {
            settings.setRotationMode(mode)
        }
    }

    /**
     * Define the scale of the MapView should be set when centering on the current position.
     * It should be between 0f and 2f.
     */
    fun setScaleCentered(scaleCentered: Float) {
        require(scaleCentered in 0f..2f)
        viewModelScope.launch {
            settings.setScaleCentered(scaleCentered)
        }
    }
}