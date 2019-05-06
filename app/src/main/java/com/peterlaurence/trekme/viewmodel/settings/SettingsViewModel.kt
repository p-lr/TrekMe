package com.peterlaurence.trekme.viewmodel.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
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
        Settings.setDownloadDir(File(newPath))
    }

    fun setStartOnPolicy(policy: StartOnPolicy) {
        Settings.setStartOnPolicy(policy)
    }

    private fun updateDownloadDirList() {
        downloadDirList.postValue(TrekMeContext.downloadDirList.map { it.absolutePath })
    }

    private fun updateDownloadDir() {
        downloadDir.postValue(Settings.getDownloadDir().absolutePath)
    }

    private fun updateStartOnPolicy() {
        startOnPolicy.postValue(Settings.getStartOnPolicy())
    }
}