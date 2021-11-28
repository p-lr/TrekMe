package com.peterlaurence.trekme.ui.gpspro.screens

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.repositories.gpspro.*
import com.peterlaurence.trekme.ui.gpspro.components.IconCircle
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.gpspro.BluetoothDeviceStub
import com.peterlaurence.trekme.viewmodel.gpspro.GpsProViewModel
import com.peterlaurence.trekme.viewmodel.gpspro.selectedDevice


@Composable
fun BtDeviceSettingsUI(
    btDeviceStub: BluetoothDeviceStub,
    diagnosisState: DiagnosisState,
    intents: BtDeviceSettingsIntents
) {
    val color = Color(0xFF4CAF50)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Header(name = btDeviceStub.name, color)
        RecordButton(diagnosisState, intents)
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
private fun RecordButton(diagnosisState: DiagnosisState, intents: BtDeviceSettingsIntents) {
    val openDialog = remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(end = 16.dp, bottom = 8.dp)
            .clickable(enabled = diagnosisState is Ready || diagnosisState is DiagnosisEmpty) {
                openDialog.value = true
            }
    ) {
        when (diagnosisState) {
            Ready -> {
                Text(
                    text = stringResource(id = R.string.bt_device_frgmt_record),
                    modifier = Modifier.padding(start = 74.dp, top = 24.dp, bottom = 24.dp)
                )
            }
            DiagnosisRunning -> {
                Text(
                    text = stringResource(id = R.string.bt_device_frgmt_record_running),
                    color = colorResource(id = R.color.colorGrey),
                    modifier = Modifier.padding(start = 74.dp, top = 24.dp, bottom = 24.dp)
                )
            }
            DiagnosisEmpty -> {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.bt_device_frgmt_record),
                        modifier = Modifier.padding(start = 74.dp, top = 24.dp, bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.bt_device_settings_diagnostic_empty),
                        modifier = Modifier.padding(start = 74.dp, top = 0.dp, bottom = 24.dp),
                        color = colorResource(id = R.color.colorGrey)
                    )
                }

            }
            is DiagnosisAwaitingSave -> {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.bt_device_settings_diagnostic_done).format(
                            diagnosisState.nbSentences
                        ),
                        modifier = Modifier.padding(start = 74.dp, top = 24.dp, bottom = 24.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedButton(
                            onClick = { intents.onCancelDiagnosisSave() }
                        ) {
                            Text(stringResource(id = R.string.cancel_dialog_string))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = { intents.onSaveDiagnosis() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorResource(
                                    id = R.color.colorGreen
                                )
                            )
                        ) {
                            Text(stringResource(id = R.string.save_action))
                        }
                    }
                }
            }
        }

        if (openDialog.value) {
            ShowDialog(openDialog, intents::onGenerateReport)
        }
    }

    Divider()
}

@Composable
private fun ShowDialog(openDialog: MutableState<Boolean>, onStartPressed: () -> Unit) {
    AlertDialog(
        onDismissRequest = { openDialog.value = false },
        title = { Text(stringResource(id = R.string.bt_device_frgmt_diag_title)) },
        text = {
            Text(
                text = stringResource(id = R.string.bt_device_frgmt_diag_content),
                textAlign = TextAlign.Justify
            )
        },
        confirmButton = {
            OutlinedButton(
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(id = R.color.colorGreen)),
                onClick = {
                    openDialog.value = false
                    onStartPressed()
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

class BtDeviceSettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        /* Get a view-model scoped to the GpsProFragment in the nav graph. Once the GpsProFragment
         * is popped from the backstack, the view-model is cleared */
        val f = findFragment<BtDeviceSettingsFragment>()
        val viewModel: GpsProViewModel = viewModel(
            f.findNavController().getBackStackEntry(R.id.gpsProFragment),
            factory = f.defaultViewModelProviderFactory
        )

        val selectedDevice = viewModel.bluetoothState.selectedDevice ?: return
        val diagnosisState by viewModel.isDiagnosisRunning.collectAsState()

        val intents = object : BtDeviceSettingsIntents {
            override fun onGenerateReport() {
                viewModel.generateDiagnosis()
            }

            override fun onCancelDiagnosisSave() = viewModel.cancelDiagnosis()

            override fun onSaveDiagnosis() {
                viewModel.saveDiagnosis()
            }
        }

        TrekMeTheme {
            BtDeviceSettingsUI(selectedDevice, diagnosisState, intents)
        }
    }
}

interface BtDeviceSettingsIntents {
    fun onGenerateReport()
    fun onCancelDiagnosisSave()
    fun onSaveDiagnosis()
}