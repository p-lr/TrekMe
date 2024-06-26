package com.peterlaurence.trekme.features.settings.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.settings.HeaderSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ListSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.LoadingSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.SettingDivider
import com.peterlaurence.trekme.features.common.presentation.ui.settings.SliderSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ToggleSetting
import com.peterlaurence.trekme.features.settings.presentation.viewmodel.SettingsViewModel

@Composable
fun SettingsStateful(
    viewModel: SettingsViewModel = hiltViewModel(),
    onMainMenuClick: () -> Unit
) {
    val startOnPolicy by viewModel.startOnPolicyFlow.collectAsState(null)
    val measurementSystem by viewModel.measurementSystemFlow.collectAsState(null)
    val appDirList by viewModel.appDirListFlow.collectAsState()
    val appDir by viewModel.appDirFlow.collectAsState(null)
    val maxScale by viewModel.maxScaleFlow.collectAsState(null)
    val scaleIndicatorChecked by viewModel.showScaleIndicatorFlow.collectAsState(initial = false)
    val defineScaleChecked by viewModel.defineScaleCenteredFlow.collectAsState(initial = false)
    val currentZoom by viewModel.currentZoom.collectAsState()
    val zoomWhenCentered by viewModel.scaleRatioCenteredFlow.collectAsState(initial = null)
    val rotationMode by viewModel.rotationModeFlow.collectAsState(initial = null)
    val magnifyingFactor by viewModel.magnifyingFactorFlow.collectAsState(initial = null)
    val trackFollowThreshold by viewModel.trackFollowThreshold.collectAsState(initial = null)
    val hasExtendedOffer by viewModel.purchaseFlow.collectAsState()

    SettingsScreen(
        startOnPolicy = startOnPolicy,
        onStartOnPolicyChange = { viewModel.setStartOnPolicy(it) },
        measurementSystem = measurementSystem,
        onMeasurementSystemChange = { viewModel.setMeasurementSystem(it) },
        appDirList = appDirList,
        appDir = appDir,
        onAppDirChange = { viewModel.setDownloadDirPath(it) },
        maxScale = maxScale,
        onMaxScaleChange = { viewModel.setMaxScale(it) },
        scaleIndicatorChecked = scaleIndicatorChecked,
        onScaleIndicatorChange = { viewModel.setShowScaleIndicator(!scaleIndicatorChecked) },
        defineScaleChecked = defineScaleChecked,
        onDefineScaleChanged = { viewModel.setDefineScaleCentered(!defineScaleChecked) },
        currentZoom = currentZoom,
        zoomWhenCentered = zoomWhenCentered,
        onZoomWhenCenteredChanged = { viewModel.setScaleRatioCentered(it) },
        rotationMode = rotationMode,
        onRotationModeChanged = { viewModel.setRotationMode(it) },
        magnifyingFactor = magnifyingFactor,
        onMagnifyingFactorChanged = { viewModel.setMagnifyingFactor(it) },
        trackFollowThreshold = trackFollowThreshold,
        onTrackFollowThresholdChanged = { viewModel.setTrackFollowThreshold(it) },
        hasExtendedOffer = hasExtendedOffer,
        onMainMenuClick = onMainMenuClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    startOnPolicy: StartOnPolicy?,
    onStartOnPolicyChange: (StartOnPolicy) -> Unit,
    measurementSystem: MeasurementSystem?,
    onMeasurementSystemChange: (MeasurementSystem) -> Unit,
    appDirList: List<String>,
    appDir: String?,
    onAppDirChange: (String) -> Unit,
    maxScale: Float?,
    onMaxScaleChange: (Float) -> Unit,
    scaleIndicatorChecked: Boolean,
    onScaleIndicatorChange: () -> Unit,
    defineScaleChecked: Boolean,
    onDefineScaleChanged: () -> Unit,
    currentZoom: Int?,
    zoomWhenCentered: Float?,
    onZoomWhenCenteredChanged: (Float) -> Unit,
    rotationMode: RotationMode?,
    onRotationModeChanged: (RotationMode) -> Unit,
    magnifyingFactor: Int?,
    onMagnifyingFactorChanged: (Int) -> Unit,
    trackFollowThreshold: Int?,
    onTrackFollowThresholdChanged: (Int) -> Unit,
    hasExtendedOffer: Boolean,
    onMainMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_frgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onMainMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            GeneralSettings(
                startOnPolicy = startOnPolicy,
                onStartOnPolicyChange = onStartOnPolicyChange,
                measurementSystem = measurementSystem,
                onMeasurementSystemChange = onMeasurementSystemChange
            )
            SettingDivider()
            RootFolderSetting(appDirList, appDir, onAppDirChange)
            SettingDivider()
            MapSetting(
                maxScale,
                onMaxScaleChange,
                scaleIndicatorChecked,
                onScaleIndicatorChange,
                defineScaleChecked,
                onDefineScaleChanged,
                currentZoom,
                zoomWhenCentered,
                onZoomWhenCenteredChanged,
                rotationMode,
                onRotationModeChanged,
                magnifyingFactor,
                onMagnifyingFactorChanged,
                trackFollowThreshold,
                onTrackFollowThresholdChanged,
                hasExtendedOffer
            )
        }
    }
}

@Composable
private fun GeneralSettings(
    startOnPolicy: StartOnPolicy?,
    onStartOnPolicyChange: (StartOnPolicy) -> Unit,
    measurementSystem: MeasurementSystem?,
    onMeasurementSystemChange: (MeasurementSystem) -> Unit
) {
    HeaderSetting(name = stringResource(id = R.string.general_category))
    if (startOnPolicy != null) {
        ListSetting(
            name = stringResource(id = R.string.preference_starton_title),
            values = listOf(
                Pair(
                    StartOnPolicy.MAP_LIST,
                    stringResource(id = R.string.preference_starton_maplist)
                ),
                Pair(
                    StartOnPolicy.LAST_MAP,
                    stringResource(id = R.string.preference_starton_lastmap)
                )
            ),
            selectedValue = startOnPolicy,
            onValueSelected = { _, v -> onStartOnPolicyChange(v) }
        )
    }
    if (measurementSystem != null) {
        ListSetting(
            name = stringResource(id = R.string.preference_measurement_system_title),
            values = listOf(
                Pair(MeasurementSystem.METRIC, stringResource(id = R.string.metric_system)),
                Pair(MeasurementSystem.IMPERIAL, stringResource(id = R.string.imperial_system))
            ),
            selectedValue = measurementSystem,
            onValueSelected = { _, v -> onMeasurementSystemChange(v) }
        )
    }
}

@Composable
private fun RootFolderSetting(
    appDirList: List<String>,
    appDir: String?,
    onAppDirChange: (String) -> Unit
) {
    HeaderSetting(name = stringResource(id = R.string.root_folder_category))

    val selected = appDir ?: appDirList.firstOrNull()
    val settingName = stringResource(id = R.string.preference_root_location_title)
    if (selected != null) {
        ListSetting(
            name = settingName,
            values = appDirList.map { Pair(it, it) },
            selectedValue = selected,
            onValueSelected = { _, v -> onAppDirChange(v) }
        )
    } else {
        LoadingSetting()
    }
}

@Composable
private fun MapSetting(
    maxScale: Float?,
    onMaxScaleChange: (Float) -> Unit,
    scaleIndicatorChecked: Boolean,
    onScaleIndicatorChange: () -> Unit,
    defineScaleChecked: Boolean,
    onDefineScaleChanged: () -> Unit,
    currentZoom: Int?,
    zoomWhenCentered: Float?,
    onZoomWhenCenteredChanged: (Float) -> Unit,
    rotationMode: RotationMode?,
    onRotationModeChanged: (RotationMode) -> Unit,
    magnifyingFactor: Int?,
    onMagnifyingFactorChanged: (Int) -> Unit,
    trackFollowThreshold: Int?,
    onTrackFollowThresholdChanged: (Int) -> Unit,
    hasExtendedOffer: Boolean
) {
    HeaderSetting(name = stringResource(id = R.string.maps_category))
    if (maxScale != null) {
        val scales = listOf(2f, 4f, 8f)
        ListSetting(
            name = stringResource(id = R.string.preference_max_scale),
            values = scales.map { Pair(it, it.toInt().toString()) },
            selectedValue = maxScale,
            onValueSelected = { _, v -> onMaxScaleChange(v) }
        )
    }
    ToggleSetting(
        name = stringResource(id = R.string.preference_show_scale_indicator),
        checked = scaleIndicatorChecked,
        onToggle = onScaleIndicatorChange
    )
    ToggleSetting(
        name = stringResource(id = R.string.preference_change_scale_when_centering),
        checked = defineScaleChecked,
        onToggle = onDefineScaleChanged
    )

    if (defineScaleChecked && zoomWhenCentered != null) {
        SliderSetting(
            name = stringResource(id = R.string.preference_zoom_when_centered).let {
                if (currentZoom != null) "$it " + stringResource(
                    id = R.string.preference_zoom_when_centered_compl
                ).format(currentZoom) else it
            },
            valueRange = 0f..100f,
            current = zoomWhenCentered,
            onValueChange = onZoomWhenCenteredChanged
        )
    }
    if (rotationMode != null) {
        ListSetting(
            name = stringResource(id = R.string.preference_rotation_mode),
            values = listOf(
                Pair(RotationMode.NONE, stringResource(id = R.string.preference_rotate_none)),
                Pair(RotationMode.FREE, stringResource(id = R.string.preference_rotate_free)),
                Pair(RotationMode.FOLLOW_ORIENTATION, stringResource(id = R.string.preference_rotate_with_orientation))
            ),
            selectedValue = rotationMode,
            onValueSelected = { _, v -> onRotationModeChanged(v) }
        )
    }
    if (magnifyingFactor != null) {
        ListSetting(
            name = stringResource(id = R.string.preference_magnifying_factor),
            values = listOf(0, 1).map { Pair(it, it.toString()) },
            selectedValue = magnifyingFactor,
            onValueSelected = { _, v -> onMagnifyingFactorChanged(v) }
        )
    }
    if (trackFollowThreshold != null && hasExtendedOffer) {
        val values = listOf(50, 100, 200)
        ListSetting(
            name = stringResource(id = R.string.preference_track_follow),
            values = values.map { Pair(it, UnitFormatter.formatDistance(it.toDouble())) },
            selectedValue = trackFollowThreshold,
            onValueSelected = { _, v -> onTrackFollowThresholdChanged(v) }
        )
    }
}
