package com.peterlaurence.trekme.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.main.ui.MainStateful
import com.peterlaurence.trekme.main.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var mapArchiveEvents: MapArchiveEvents

    @Inject
    lateinit var gpsProEvents: GpsProEvents

    @Inject
    lateinit var appEventBus: AppEventBus

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            TrekMeTheme {
                MainStateful(
                    viewModel = viewModel,
                    recordingEventHandlerViewModel = hiltViewModel(),
                    appEventBus = appEventBus,
                    gpsProEvents = gpsProEvents,
                    mapArchiveEvents = mapArchiveEvents
                )
            }
        }
    }
}
