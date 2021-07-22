package com.peterlaurence.trekme.ui.gpspro.screens

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.gpspro.components.IconCircle
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.BluetoothDeviceStub
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProViewModel
import com.peterlaurence.trekme.viewmodel.gpspro.PairedDeviceList


@Composable
fun BtDeviceSettingsUI(btDeviceStub: BluetoothDeviceStub) {
    val color = Color(0xFF4CAF50)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        IconCircle(backgroundColor = color, 50.dp, R.drawable.bluetooth)
        Spacer(Modifier.height(8.dp))
        Text(btDeviceStub.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(16.dp))
        Divider()
    }

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