package com.peterlaurence.trekme.ui.gpspro.presentation.ui.screens

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.databinding.FragmentBtDeviceSettingsBinding
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
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
    private var binding: FragmentBtDeviceSettingsBinding? = null

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
        binding = FragmentBtDeviceSettingsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
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