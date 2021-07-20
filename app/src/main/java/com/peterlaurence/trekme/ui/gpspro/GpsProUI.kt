package com.peterlaurence.trekme.ui.gpspro

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.gpspro.components.IconCircle
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.*


@Composable
fun GpsProUI(
        bluetoothState: BluetoothState,
        isHostSelected: Boolean,
        onHostSelection: () -> Unit,
        onBtDeviceSelection: (BluetoothDeviceStub) -> Unit
) {
    Column {
        HostDeviceLine(name = stringResource(id = R.string.internal_gps), isHostSelected, onHostSelection)
        BluetoothUI(bluetoothState, onBtDeviceSelection)
    }
}

@Composable
fun BluetoothUI(bluetoothState: BluetoothState, onBtDeviceSelection: (BluetoothDeviceStub) -> Unit) {
    Text(
            stringResource(id = R.string.previously_connected_bt_devices),
            color = Color(0xFF808080),
            fontSize = 11.sp,
            fontWeight = FontWeight.W500,
            style = TextStyle(textIndent = TextIndent(25.sp), letterSpacing = 1.sp * 0.8)
    )
    when (bluetoothState) {
        is Searching -> Text("Searching")
        is PairedDeviceList -> Column {
            for (device in bluetoothState.deviceList) {
                DeviceLine(device) {
                    onBtDeviceSelection(it)
                }
            }
        }
        BtDisabled -> TODO("show rationale (bt is disabled)")
        BtNotSupported -> TODO("show rationale (bt not supported on this device)")
    }
}

@Composable
fun HostDeviceLine(name: String, isSelected: Boolean, onSelection: () -> Unit) {
    val color = Color(0xFF2196F3)
    Row(Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelection() }
            .then(
                    if (isSelected) {
                        Modifier.border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
                    } else Modifier
            )
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(backgroundColor = color, R.drawable.phone)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = name)
    }
}

@Composable
fun DeviceLine(device: BluetoothDeviceStub, onSelection: (BluetoothDeviceStub) -> Unit) {
    val color = Color(0xFF4CAF50)
    Row(Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onSelection(device) }
            .then(
                    if (device.isActive) {
                        Modifier.border(BorderStroke(2.dp, color), RoundedCornerShape(5.dp))
                    } else Modifier
            )
            .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(backgroundColor = color, R.drawable.bluetooth)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = device.name)
        if (device.isActive) {
            Spacer(modifier = Modifier.weight(1f))
            Box(
                    modifier = Modifier
                            .size(1.dp, 24.dp)
                            .background(color = Color.LightGray)
            )
            Icon(imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    modifier = Modifier
                            .clickable {
                                // TODO: navigate to device settings fragment
                            }
                            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    tint = MaterialTheme.colors.secondary)
        }
    }
}

class GpsProUIView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: GpsProViewModel = viewModel()

        TrekMeTheme {
            GpsProUI(viewModel.bluetoothState, viewModel.isHostSelected,
                    viewModel::onHostSelected, viewModel::onBtDeviceSelection)
        }
    }
}