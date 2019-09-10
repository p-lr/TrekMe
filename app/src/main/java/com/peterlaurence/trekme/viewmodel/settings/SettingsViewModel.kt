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
    private val downloadDirList = MutableLiveData<List<String>>()

    /* The path of the current download directory */
    private val downloadDir = MutableLiveData<String>()

    private val startOnPolicy = MutableLiveData<StartOnPolicy>()

    init {
        /* For instance the need is only to fetch this once */
        updateDownloadDirList()
        updateDownloadDir()
        updateStartOnPolicy()
    }

    fun getDownloadDirList(): LiveData<List<String>> {
        return downloadDirList
    }

    fun getDownloadDir(): LiveData<String> {
        return downloadDir
    }

    fun getStartOnPolicy(): LiveData<StartOnPolicy> {
        return startOnPolicy
    }

    fun setDownloadDirPath(newPath: String) {
        downloadDir.postValue(newPath)
        viewModelScope.launch {
            Settings.setDownloadDir(File(newPath))
        }
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        startOnPolicy.postValue(policy)
        viewModelScope.launch {
            Settings.setStartOnPolicy(policy)
        }
    }

    private fun updateDownloadDirList() {
        downloadDirList.postValue(TrekMeContext.downloadDirList.map { it.absolutePath })
    }

    private fun updateDownloadDir() {
        viewModelScope.launch {
            downloadDir.postValue(Settings.getDownloadDir().absolutePath)
        }
    }

    private fun updateStartOnPolicy() {
        viewModelScope.launch {
            startOnPolicy.postValue(Settings.getStartOnPolicy())
        }
    }
}