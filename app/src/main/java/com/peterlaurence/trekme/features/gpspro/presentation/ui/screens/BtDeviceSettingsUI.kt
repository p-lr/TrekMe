package com.peterlaurence.trekme.features.gpspro.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.gpspro.domain.repositories.*
import com.peterlaurence.trekme.features.gpspro.presentation.ui.components.IconCircle
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BluetoothDeviceStub


@Composable
fun BtDeviceSettingsUI(
    btDeviceStub: BluetoothDeviceStub,
    diagnosisState: DiagnosisState,
    intents: BtDeviceSettingsIntents
) {
    val color = Color(0xFF4CAF50)
    Surface {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Header(name = btDeviceStub.name, color)
            RecordButton(diagnosisState, intents)
        }
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (diagnosisState) {
            Ready -> {
                ElevatedButton(
                    onClick = { openDialog.value = true },
                ) {
                    Text(
                        text = stringResource(id = R.string.bt_device_frgmt_record),
                    )
                }
            }
            DiagnosisRunning -> {
                LinearProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(id = R.string.bt_device_frgmt_record_running))
            }
            DiagnosisEmpty -> {
                Text(
                    text = stringResource(id = R.string.bt_device_settings_diagnostic_empty),
                    modifier = Modifier.padding(horizontal = 48.dp)
                )

                ElevatedButton(
                    onClick = { openDialog.value = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 24.dp)
                ) {
                    Text(text = stringResource(id = R.string.bt_device_frgmt_record))
                }
            }
            is DiagnosisAwaitingSave -> {
                Text(
                    text = stringResource(id = R.string.bt_device_settings_diagnostic_done).format(
                        diagnosisState.nbSentences
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextButton(
                        onClick = { intents.onCancelDiagnosisSave() },
                    ) {
                        Text(stringResource(id = R.string.cancel_dialog_string))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { intents.onSaveDiagnosis() },
                    ) {
                        Text(stringResource(id = R.string.save_action))
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
            TextButton(
                onClick = {
                    openDialog.value = false
                    onStartPressed()
                }
            ) {
                Text(stringResource(id = R.string.start_dialog_string))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    openDialog.value = false
                }
            ) {
                Text(stringResource(id = R.string.cancel_dialog_string))
            }
        }
    )
}

interface BtDeviceSettingsIntents {
    fun onGenerateReport()
    fun onCancelDiagnosisSave()
    fun onSaveDiagnosis()
}