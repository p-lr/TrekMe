package com.peterlaurence.trekme.viewmodel.gpspro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GpsProViewModel : ViewModel() {
    val searchState by mutableStateOf<DeviceSearchState>(Searching)
}

sealed class DeviceSearchState
object Searching : DeviceSearchState()
data class DeviceFound(val deviceList: List<String>) : DeviceSearchState()