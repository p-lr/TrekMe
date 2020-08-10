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
        trekMeContext: TrekMeContext,
        private val settings: Settings
) : ViewModel() {
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
    private val _defineScaleCentered = MutableLiveData<Boolean>()
    val defineScaleCentered: LiveData<Boolean> = _defineScaleCentered
    private val _scaleCentered = MutableLiveData<Float>()
    val scaleCentered: LiveData<Float> = _scaleCentered

    /* For instance the need is only to fetch settings once */
    init {
        /* App dir list */
        _appDirListLiveData.value = trekMeContext.mapsDirList?.map { it.absolutePath }
                ?: emptyList()

        /* App dir active */
        _appDirLiveData.value = settings.getAppDir()?.absolutePath ?: "error"

        /* StartOn policy */
        _startOnPolicyLiveData.value = settings.getStartOnPolicy()

        /* Magnifying factor */
        _magnifyingFactorLiveData.value = settings.getMagnifyingFactor()

        /* Rotation mode */
        _rotationModeLiveData.value = settings.getRotationMode()

        /* Define scale centered */
        _defineScaleCentered.value = settings.getDefineScaleCentered()

        /* Scale centered */
        _scaleCentered.value = settings.getScaleCentered()
    }

    fun setDownloadDirPath(newPath: String) {
        _appDirLiveData.postValue(newPath)
        settings.setAppDir(File(newPath))
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        _startOnPolicyLiveData.postValue(policy)
        settings.setStartOnPolicy(policy)
    }

    fun setMagnifyingFactor(factor: Int) {
        _magnifyingFactorLiveData.postValue(factor)
        viewModelScope.launch {
            settings.setMagnifyingFactor(factor)
        }
    }

    fun setRotationMode(mode: RotationMode) {
        _rotationModeLiveData.postValue(mode)
        settings.setRotationMode(mode)
    }

    fun setDefineScaleCentered(defined: Boolean) {
        settings.setDefineScaleCentered(defined)
    }

    /**
     * Define the scale of the MapView should be set when centering on the current position.
     * It should be between 0f and 2f.
     */
    fun setScaleCentered(scaleCentered: Float) {
        require(scaleCentered in 0f..2f)
        settings.setScaleCentered(scaleCentered)
    }
}