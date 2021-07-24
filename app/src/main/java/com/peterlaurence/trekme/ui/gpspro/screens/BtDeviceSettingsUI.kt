package com.peterlaurence.trekme.ui.gpspro.screens

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        Header(name = btDeviceStub.name, color)
        RecordButton()
    }

}

@Composable
private fun Header(name: String, color: Color) {
    Spacer(Modifier.height(24.dp))
    IconCircle(backgroundColor = color, 50.dp, R.drawable.bluetooth)
    Spacer(Modifier.height(8.dp))
    Text(name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(16.dp))
    Divider()
}

@Composable
private fun RecordButton() {
    val openDialog = remember { mutableStateOf(false) }
    Row(Modifier
            .fillMaxWidth()
            .clickable {
                openDialog.value = true
            }
    ) {
        Text(
                text = stringResource(id = R.string.bt_device_frgmt_record),
                modifier = Modifier.padding(start = 74.dp, top = 24.dp, bottom = 24.dp),
        )
        if (openDialog.value) {
            ShowDialog(openDialog)
        }
    }

    Divider()
}

@Composable
private fun ShowDialog(openDialog: MutableState<Boolean>) {
    AlertDialog(
            onDismissRequest = { openDialog.value = false },
            title = { Text(stringResource(id = R.string.bt_device_frgmt_diag_title)) },
            text = {
                Text(
                        text = stringResource(id = R.string.bt_device_frgmt_diag_content),
                        textAlign = TextAlign.Justify)
            },
            confirmButton = {
                OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(id = R.color.colorGreen)),
                        onClick = {
                            openDialog.value = false
                        }) {
                    Text(stringResource(id = R.string.start_dialog_string))
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = {
                            openDialog.value = false
                        }) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            }
    )
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