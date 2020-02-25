package com.peterlaurence.trekme.viewmodel.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel : ViewModel() {
    private val downloadDirListLiveData = MutableLiveData<List<String>>()
    private val downloadDirLiveData = MutableLiveData<String>()
    private val startOnPolicyLiveData = MutableLiveData<StartOnPolicy>()
    private val magnifyingFactorLiveData = MutableLiveData<Int>()

    init {
        /* For instance the need is only to fetch this once */
        updateDownloadDirList()
        updateDownloadDir()
        updateStartOnPolicy()
        updateMagnifyingFactor()
    }

    fun getDownloadDirList(): LiveData<List<String>> {
        return downloadDirListLiveData
    }

    fun getDownloadDir(): LiveData<String> {
        return downloadDirLiveData
    }

    fun getStartOnPolicy(): LiveData<StartOnPolicy> {
        return startOnPolicyLiveData
    }

    fun getMagnifyingFactorLiveData(): LiveData<Int> {
        return magnifyingFactorLiveData
    }

    fun setDownloadDirPath(newPath: String) {
        downloadDirLiveData.postValue(newPath)
        viewModelScope.launch {
            Settings.setDownloadDir(File(newPath))
        }
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        startOnPolicyLiveData.postValue(policy)
        viewModelScope.launch {
            Settings.setStartOnPolicy(policy)
        }
    }

    fun setMagnifyingFactor(factor: Int) {
        magnifyingFactorLiveData.postValue(factor)
        viewModelScope.launch {
            Settings.setMagnifyingFactor(factor)
        }
    }

    private fun updateDownloadDirList() {
        downloadDirListLiveData.postValue(TrekMeContext.downloadDirList?.map { it.absolutePath } ?: emptyList())
    }

    private fun updateDownloadDir() {
        viewModelScope.launch {
            downloadDirLiveData.postValue(Settings.getDownloadDir()?.absolutePath ?: "error")
        }
    }

    private fun updateStartOnPolicy() {
        viewModelScope.launch {
            startOnPolicyLiveData.postValue(Settings.getStartOnPolicy())
        }
    }

    private fun updateMagnifyingFactor() {
        viewModelScope.launch {
            magnifyingFactorLiveData.postValue(Settings.getMagnifyingFactor())
        }
    }
}