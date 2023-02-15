package com.peterlaurence.trekme.features.gpspro.presentation.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.GpsProViewModel
import com.peterlaurence.trekme.features.gpspro.presentation.viewmodel.selectedDevice
import com.peterlaurence.trekme.util.collectWhileStarted
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class BtDeviceSettingsFragment : Fragment() {
    private val viewModel: GpsProViewModel by activityViewModels()

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var app: Application

    @Volatile
    private var fileContent: String = ""

    private val formatter: SimpleDateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gpsProEvents.writeDiagnosisFileFlow.collectWhileStarted(this) {
            fileContent = it
            writeFile(it, "${getString(R.string.diagnosis)}-${formatter.format(Date())}.txt")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        /* The action bar isn't managed by Compose */
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            show()
            title = getString(R.string.bt_device_frgmt_title)
        }

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            val intents = object : BtDeviceSettingsIntents {
                override fun onGenerateReport() {
                    viewModel.generateDiagnosis()
                }

                override fun onCancelDiagnosisSave() = viewModel.cancelDiagnosis()

                override fun onSaveDiagnosis() {
                    viewModel.saveDiagnosis()
                }
            }

            setContent {
                val selectedDevice = viewModel.bluetoothState.selectedDevice ?: return@setContent
                val diagnosisState by viewModel.isDiagnosisRunning.collectAsState()

                TrekMeTheme {
                    BtDeviceSettingsUI(selectedDevice, diagnosisState, intents)
                }
            }
        }
    }

    private val fileWriteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        app.applicationContext.contentResolver.openFileDescriptor(uri, "wt")?.use {
                            FileOutputStream(it.fileDescriptor).use { fos ->
                                fos.write(fileContent.toByteArray())
                            }
                        }
                        appEventBus.postMessage(StandardMessage(getString(R.string.bt_device_frgmt_record_done)))
                    } catch (e: Exception) {
                        appEventBus.postMessage(StandardMessage(getString(R.string.bt_device_frgmt_record_error)))
                    }
                }
            }
        }
    }

    private fun writeFile(fileContent: String, title: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "txt/plain"
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra("fileContent", fileContent)

        }
        fileWriteLauncher.launch(intent)
    }
}