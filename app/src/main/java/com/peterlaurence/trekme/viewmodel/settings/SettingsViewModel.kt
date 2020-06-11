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

    init {
        /* For instance the need is only to fetch this once */
        updateAppDirList()
        updateAppDir()
        updateStartOnPolicy()
        updateMagnifyingFactor()
        updateRotateWithOrientation()
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

    private fun updateAppDirList() {
        _appDirListLiveData.postValue(trekMeContext.mapsDirList?.map { it.absolutePath }
                ?: emptyList())
    }

    private fun updateAppDir() {
        viewModelScope.launch {
            _appDirLiveData.postValue(settings.getAppDir()?.absolutePath ?: "error")
        }
    }

    private fun updateStartOnPolicy() {
        viewModelScope.launch {
            _startOnPolicyLiveData.postValue(settings.getStartOnPolicy())
        }
    }

    private fun updateMagnifyingFactor() {
        viewModelScope.launch {
            _magnifyingFactorLiveData.postValue(settings.getMagnifyingFactor())
        }
    }

    private fun updateRotateWithOrientation() {
        viewModelScope.launch {
            _rotationModeLiveData.value = settings.getRotationMode()
        }
    }
}