package com.peterlaurence.trekme.features.gpspro.presentation.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.peterlaurence.trekme.features.gpspro.domain.repositories.DiagnosisAwaitingSave
import com.peterlaurence.trekme.features.gpspro.domain.repositories.DiagnosisEmpty
import com.peterlaurence.trekme.features.gpspro.domain.repositories.DiagnosisRunning
import com.peterlaurence.trekme.features.gpspro.domain.repositories.DiagnosisState
import com.peterlaurence.trekme.features.gpspro.domain.repositories.Ready
import com.peterlaurence.trekme.features.gpspro.presentation.ui.components.IconCircle
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.BluetoothDeviceStub
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.GpsProViewModel
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.selectedDevice
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BtDeviceSettingsStateful(
    viewModel: GpsProViewModel,
    onBackClick: () -> Unit
) {
    val selectedDevice = viewModel.bluetoothState.selectedDevice ?: return
    val diagnosisState by viewModel.isDiagnosisRunning.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.writeDiagnosisFileTo(uri)
            }
        }
    }

    val diagnosisStr = stringResource(id = R.string.diagnosis)
    LaunchedEffectWithLifecycle(viewModel.diagnosisEvent) { fileContent ->
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)
        val title = "$diagnosisStr-${formatter.format(Date())}.txt"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "txt/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra("fileContent", fileContent)
        }
        launcher.launch(intent)
    }

    BtDeviceSettingsScreen(
        btDeviceStub = selectedDevice,
        diagnosisState = diagnosisState,
        onGenerateReport = viewModel::generateDiagnosis,
        onCancelDiagnosisSave = viewModel::cancelDiagnosis,
        onSaveDiagnosis = viewModel::onRequestSaveDiagnosis,
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BtDeviceSettingsScreen(
    btDeviceStub: BluetoothDeviceStub,
    diagnosisState: DiagnosisState,
    onGenerateReport: () -> Unit,
    onCancelDiagnosisSave: () -> Unit,
    onSaveDiagnosis: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.bt_device_frgmt_diag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
            )
        }
    ) { paddingValues ->
        val color = Color(0xFF4CAF50)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Header(name = btDeviceStub.name, color)
            RecordButton(
                diagnosisState = diagnosisState,
                onGenerateReport = onGenerateReport,
                onCancelDiagnosisSave = onCancelDiagnosisSave,
                onSaveDiagnosis = onSaveDiagnosis
            )
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
    HorizontalDivider()
}

@Composable
private fun RecordButton(
    diagnosisState: DiagnosisState,
    onGenerateReport: () -> Unit,
    onCancelDiagnosisSave: () -> Unit,
    onSaveDiagnosis: () -> Unit
) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    TextButton(
                        onClick = onCancelDiagnosisSave,
                    ) {
                        Text(stringResource(id = R.string.cancel_dialog_string))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSaveDiagnosis,
                    ) {
                        Text(stringResource(id = R.string.save_action))
                    }
                }
            }
        }

        if (openDialog.value) {
            ShowDialog(openDialog, onGenerateReport)
        }
    }

    HorizontalDivider()
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