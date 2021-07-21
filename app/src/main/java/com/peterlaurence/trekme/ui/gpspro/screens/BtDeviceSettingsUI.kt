package com.peterlaurence.trekme.ui.gpspro.screens

import android.content.Context
import android.util.AttributeSet
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.BluetoothDeviceStub
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProViewModel
import com.peterlaurence.trekme.viewmodel.gpspro.PairedDeviceList


@Composable
fun BtDeviceSettingsUI(btDeviceStub: BluetoothDeviceStub) {
    Text("settings for ${btDeviceStub.name}")
}

class BtDeviceSettingsUI @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpsProViewModel = viewModel()

        val selectedDevice = when (val state = viewModel.bluetoothState) {
            is PairedDeviceList -> state.deviceList.firstOrNull { it.isActive }
            else -> null
        } ?: return

        TrekMeTheme {
            BtDeviceSettingsUI(selectedDevice)
        }
    }
}