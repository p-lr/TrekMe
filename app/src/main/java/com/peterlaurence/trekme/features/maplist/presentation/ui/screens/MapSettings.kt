package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.CreationData
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.features.common.domain.util.makeMapForComposePreview
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ButtonSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ButtonSettingWithLock
import com.peterlaurence.trekme.features.common.presentation.ui.settings.EditTextSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.HeaderSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ListSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.SettingDivider
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import com.peterlaurence.trekme.util.ResultL
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.isFrench
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * UI that shows the settings for a given map. It provides the abilities to :
 *
 *  * Change map thumbnail image
 *  * Map calibration
 *
 *  * Change the map name
 *  * Compute map size
 *  * Save the map
 *
 *  * Repair the map
 *  * Update the map
 *
 * @since 16/04/2016 - Converted to Kotlin on 11/11/2020 - Converted to compose on 08/09/2023
 */
@Composable
fun MapSettingsStateful(
    viewModel: MapSettingsViewModel = hiltViewModel(),
    onNavigateToCalibration: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBackClick: () -> Unit
) {
    val settingMap by viewModel.mapFlow.collectAsStateWithLifecycle()
    val map = settingMap

    val hasExtendedOffer by viewModel.hasExtendedOffer.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val successMsg = stringResource(id = R.string.map_image_import_ok)
    val failureMsg = stringResource(id = R.string.map_image_import_error)
    LaunchedEffectWithLifecycle(flow = viewModel.mapImageImportEvent) { success ->
        if (success) {
            snackbarHostState.showSnackbar(successMsg)
        } else {
            snackbarHostState.showSnackbar(failureMsg)
        }
    }

    if (map != null) {
        val name by map.name.collectAsStateWithLifecycle()
        val mapUpdateState = viewModel.mapUpdateStateFlow
        MapSettingsScreen(
            map = map,
            name = name,
            mapUpdateState = mapUpdateState,
            hasExtendedOffer = hasExtendedOffer,
            snackbarHostState = snackbarHostState,
            mapSizeState = viewModel.mapSize,
            onSetImage = { uri ->
                viewModel.setMapImage(map, uri)
            },
            onSetProjection = { projectionName ->
                viewModel.setProjection(map, projectionName)
            },
            onNavigateToCalibration = onNavigateToCalibration,
            onSetCalibrationPointNumber = { n ->
                viewModel.setCalibrationPointsNumber(map, n)
            },
            onMapRename = {
                viewModel.renameMap(map, it)
            },
            onComputeMapSize = {
                viewModel.computeMapSize(map)
            },
            onArchiveMap = { uri ->
                viewModel.archiveMap(map, uri)
            },
            onStartRepair = {
                viewModel.update(map, repairOnly = true)
            },
            onStartUpdate = {
                viewModel.update(map, repairOnly = false)
            },
            onNavigateToShop = onNavigateToShop,
            onBackClick = onBackClick
        )
    } else {
        MapSettingsErrorScreen(onBackClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSettingsErrorScreen(onBackClick: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.map_settings_frgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        }
    ) { padding ->
        ErrorScreen(
            Modifier.padding(padding),
            message = stringResource(id = R.string.map_settings_error)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapSettingsScreen(
    map: Map,
    name: String,
    mapUpdateState: StateFlow<MapSettingsViewModel.MapUpdateState?>,
    hasExtendedOffer: Boolean,
    snackbarHostState: SnackbarHostState,
    mapSizeState: MutableStateFlow<ResultL<Long?>>,
    onSetImage: (Uri) -> Unit,
    onSetProjection: (String?) -> Unit,
    onNavigateToCalibration: () -> Unit,
    onSetCalibrationPointNumber: (Int) -> Unit,
    onMapRename: (String) -> Unit,
    onComputeMapSize: () -> Unit,
    onArchiveMap: (Uri) -> Unit,
    onStartRepair: () -> Unit,
    onStartUpdate: () -> Unit,
    onNavigateToShop: () -> Unit,
    onBackClick: () -> Unit
) {
    var isShowingAdvancedSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            var expandedMenu by remember { mutableStateOf(false) }
            /* Displaying calibration options is disabled for now */
            val dropDownMenu: @Composable () -> Unit = {
                IconButton(
                    onClick = { expandedMenu = true },
                    modifier = Modifier.width(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                    Box(
                        Modifier
                            .height(24.dp)
                            .wrapContentSize(Alignment.BottomEnd, true)
                    ) {
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            offset = DpOffset(0.dp, 0.dp)
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expandedMenu = false
                                    isShowingAdvancedSettings = !isShowingAdvancedSettings
                                },
                                text = {
                                    Text(
                                        text = if (isShowingAdvancedSettings) {
                                            stringResource(id = R.string.map_settings_hide_advanced)
                                        } else {
                                            stringResource(id = R.string.settings_show_advanced)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            TopAppBar(
                title = { Text(text = name) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
                actions = {
                    /* Displaying calibration options is disabled for now */
                    //dropDownMenu()
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            Modifier
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            ThumbnailSetting(onSetImage)
            if (isShowingAdvancedSettings) {
                SettingDivider()
                CalibrationSetting(
                    map,
                    onSetProjection,
                    onSetCalibrationPointNumber,
                    onNavigateToCalibration
                )
            }
            SettingDivider()
            MapSettings(name, mapSizeState, onMapRename, onComputeMapSize, onArchiveMap)
            SettingDivider()
            MapRepairSetting(
                map,
                hasExtendedOffer,
                mapUpdateState,
                onNavigateToShop,
                onStartRepair,
                onStartUpdate
            )
        }
    }
}

@Composable
private fun ThumbnailSetting(onSetImage: (Uri) -> Unit) {
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        /* Check if the request code is the one we are interested in */
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data == null) return@rememberLauncherForActivityResult
            val uri = result.data?.data
            if (uri != null) {
                onSetImage(uri)
            }
        }
    }

    HeaderSetting(name = stringResource(id = R.string.image_preferences_category))
    ButtonSetting(
        name = stringResource(id = R.string.image_change_btn_txt),
        enabled = true,
        onClick = {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            /* Search for all documents available via installed storage providers */
            intent.type = "image/*"
            resultLauncher.launch(intent)
        }
    )
}

@Composable
private fun CalibrationSetting(
    map: Map,
    onSetProjection: (String?) -> Unit,
    onSetCalibrationPointNumber: (Int) -> Unit,
    onNavigateToCalibration: () -> Unit
) {
    HeaderSetting(name = stringResource(id = R.string.calibration_preferences_category))
    ProjectionSetting(map, onSetProjection)
    CalibrationPointsSetting(map, onSetCalibrationPointNumber)
    ButtonSetting(
        name = stringResource(id = R.string.calibration_button_txt),
        enabled = true,
        onClick = onNavigateToCalibration
    )
}

@Composable
private fun ProjectionSetting(map: Map, onSetProjection: (String?) -> Unit) {
    val values = listOf(
        MercatorProjection.NAME to stringResource(id = R.string.pseudo_mercator),
        null to stringResource(id = R.string.projection_none)
    )

    ListSetting(
        name = stringResource(id = R.string.projection_preferences_title),
        values = values,
        selectedValue = map.projection?.name,
        showSubtitle = true,
        onValueSelected = { _: Int, v: String? ->
            onSetProjection(v)
        }
    )
}

@Composable
private fun CalibrationPointsSetting(map: Map, onSetCalibrationPointNumber: (Int) -> Unit) {
    val values = listOf(
        2 to "2",
        3 to "3",
        4 to "4"
    )

    ListSetting(
        name = stringResource(id = R.string.points_number_preferences_title),
        values = values,
        selectedValue = map.calibrationPointsNumber,
        showSubtitle = true,
        onValueSelected = { _, v ->
            onSetCalibrationPointNumber(v)
        }
    )
}

@Composable
private fun MapSettings(
    name: String,
    mapSizeState: MutableStateFlow<ResultL<Long?>>,
    onMapRename: (String) -> Unit,
    onComputeMapSize: () -> Unit,
    onArchiveMap: (Uri) -> Unit
) {
    HeaderSetting(name = stringResource(id = R.string.map_summary_category))
    ChangeNameSetting(name, onMapRename)
    ComputeSizeSetting(mapSizeState, onComputeMapSize)
    SaveSetting(onArchiveMap)
}

@Composable
private fun ChangeNameSetting(name: String, onMapRename: (String) -> Unit) {
    EditTextSetting(
        name = stringResource(id = R.string.map_title),
        value = name,
        onValueChanged = onMapRename
    )
}

@Composable
private fun ComputeSizeSetting(
    mapSizeState: MutableStateFlow<ResultL<Long?>>,
    onComputeMapSize: () -> Unit
) {
    val mapSizeResultL by mapSizeState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ButtonSetting(
        name = if (mapSizeResultL.getOrNull() != null) {
            stringResource(id = R.string.map_size_string)
        } else {
            stringResource(id = R.string.map_size_compute_string)
        },
        enabled = mapSizeResultL.isSuccess && mapSizeResultL.getOrNull() == null,
        subTitle = if (mapSizeResultL.isLoading) {
            stringResource(id = R.string.map_size_computing)
        } else {
            mapSizeResultL.getOrNull()?.formatSize(context)
        },
        onClick = onComputeMapSize
    )
}

@Composable
private fun SaveSetting(onArchiveMap: (Uri) -> Unit) {
    var isShowingModal by remember { mutableStateOf(false) }
    ButtonSetting(
        name = stringResource(id = R.string.map_save_string),
        enabled = true,
        onClick = { isShowingModal = true }
    )

    val mapSaveLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            /* After the user selected a folder in which to archive a map, call the relevant view-model */
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data == null) return@rememberLauncherForActivityResult
                val uri = result.data?.data
                if (uri != null) {
                    onArchiveMap(uri)
                }
            }
        }

    if (isShowingModal) {
        AlertDialog(
            title = {
                Text(
                    stringResource(id = R.string.archive_dialog_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(stringResource(id = R.string.archive_dialog_description))
            },
            onDismissRequest = { isShowingModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingModal = false
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        mapSaveLauncher.launch(intent)
                    }
                ) {
                    Text(stringResource(id = R.string.ok_dialog))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingModal = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

@Composable
private fun MapRepairSetting(
    map: Map,
    hasExtendedOffer: Boolean,
    mapUpdateState: StateFlow<MapSettingsViewModel.MapUpdateState?>,
    onNavigateToShop: () -> Unit,
    onStartRepair: () -> Unit,
    onStartUpdate: () -> Unit
) {
    val creationData = map.creationData
    if (creationData != null) {
        val missingTilesCount by map.missingTilesCount.collectAsStateWithLifecycle()
        val updateState by mapUpdateState.collectAsStateWithLifecycle()
        HeaderSetting(name = stringResource(id = R.string.map_update_category))
        AnalyseAndRepair(
            missingTilesCount = missingTilesCount,
            progress = updateState?.let { if (it.mapId == map.id && it.repairOnly) it.progress else null },
            hasExtendedOffer = hasExtendedOffer,
            onNavigateToShop = onNavigateToShop,
            onStartRepair = onStartRepair
        )
        UpdateButton(
            map = map,
            progress = updateState?.let { if (it.mapId == map.id && !it.repairOnly) it.progress else null },
            creationData = creationData,
            hasExtendedOffer = hasExtendedOffer,
            onNavigateToShop = onNavigateToShop,
            onStartUpdate = onStartUpdate
        )
    }
}

@Composable
private fun AnalyseAndRepair(
    missingTilesCount: Long?,
    progress: Float?,
    hasExtendedOffer: Boolean,
    onNavigateToShop: () -> Unit,
    onStartRepair: () -> Unit
) {
    var isShowingClickRationaleData by remember { mutableStateOf(false) }

    ButtonSettingWithLock(
        title = {
            if (progress != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.map_analyze_and_repair_progress))
                    Spacer(modifier = Modifier.width(24.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            } else {
                if (missingTilesCount == 0L) {
                    Text(stringResource(id = R.string.map_no_missing_tile))
                } else {
                    Text(stringResource(id = R.string.map_analyze_and_repair))
                }
            }
        },
        subTitle = if (missingTilesCount != null) {
            stringResource(id = R.string.map_missing_tiles).format(missingTilesCount)
        } else null,
        enabled = progress == null,
        isLocked = !hasExtendedOffer,
        lockedRationale = stringResource(id = R.string.map_repair_rationale),
        onNavigateToShop = onNavigateToShop,
        onClick = { isShowingClickRationaleData = true }
    )

    if (isShowingClickRationaleData) {
        AlertDialog(
            text = { Text(stringResource(id = R.string.map_repair_rationale_subscribed_user)) },
            onDismissRequest = { isShowingClickRationaleData = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingClickRationaleData = false
                        onStartRepair()
                    }
                ) {
                    Text(stringResource(id = R.string.map_repair_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingClickRationaleData = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

@Composable
private fun UpdateButton(
    map: Map,
    progress: Float?,
    creationData: CreationData,
    hasExtendedOffer: Boolean,
    onNavigateToShop: () -> Unit,
    onStartUpdate: () -> Unit
) {
    var isShowingClickRationaleData by remember { mutableStateOf(false) }
    val lastUpdateDate by map.lastUpdateDate.collectAsStateWithLifecycle()
    val lastUptDate = lastUpdateDate
    val subtitle = if (lastUptDate != null) {
        stringResource(id = R.string.map_updated_on).format(formatIsoDate(lastUptDate))
    } else {
        stringResource(id = R.string.map_created_on).format(formatIsoDate(creationData.creationDate))
    }

    ButtonSettingWithLock(
        title = {
            if (progress == null) {
                Text(stringResource(id = R.string.map_update))
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.map_update_progress))
                    Spacer(modifier = Modifier.width(24.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        },
        subTitle = subtitle,
        enabled = progress == null,
        isLocked = !hasExtendedOffer,
        lockedRationale = stringResource(id = R.string.map_update_rationale),
        onNavigateToShop = onNavigateToShop,
        onClick = { isShowingClickRationaleData = true }
    )

    if (isShowingClickRationaleData) {
        AlertDialog(
            text = { Text(stringResource(id = R.string.map_update_rationale)) },
            onDismissRequest = { isShowingClickRationaleData = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingClickRationaleData = false
                        onStartUpdate()
                    }
                ) {
                    Text(stringResource(id = R.string.map_update_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingClickRationaleData = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

/**
 * Converts a quantity expected to be in bytes into a [String]. For example:
 * 12_500 -> "12.5 Ko" (in French), or "12.5 KB" (in English)
 */
private fun Long.formatSize(context: Context): String {
    val byteStr = if (isFrench(context)) "o" else "B"
    var divider = 1.0
    val prefix = when {
        this < 1000 -> ""
        this < 500_000 -> {
            divider = 1000.0
            "K"
        }

        this < 500_000_000 -> {
            divider = 1000_000.0
            "M"
        }

        else -> {
            divider = 1_000_000_000.0
            "G"
        }
    }
    val number = BigDecimal(this / divider).setScale(2, RoundingMode.HALF_EVEN)
    return "$number $prefix$byteStr"
}

private fun formatIsoDate(epochSeconds: Long): String {
    return runCatching {
        DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC"))
            .format(Instant.ofEpochSecond(epochSeconds))
    }.getOrElse { "" }
}

@Preview
@Composable
private fun MapScreenPreview() {
    TrekMeTheme {
        val map = makeMapForComposePreview()
        val name by map.name.collectAsStateWithLifecycle()

        val mapSizeState: MutableStateFlow<ResultL<Long?>> =
            remember { MutableStateFlow(ResultL.success(125468L)) }

        MapSettingsScreen(
            map = map,
            name = name,
            mapUpdateState = MutableStateFlow(null),
            hasExtendedOffer = false,
            snackbarHostState = remember { SnackbarHostState() },
            mapSizeState = mapSizeState,
            onSetImage = {},
            onSetProjection = {},
            onNavigateToCalibration = {},
            onSetCalibrationPointNumber = {},
            onMapRename = {},
            onComputeMapSize = {},
            onArchiveMap = {},
            onStartRepair = {},
            onStartUpdate = {},
            onNavigateToShop = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun MapScreenRepairPendingPreview() {
    TrekMeTheme {
        val map = makeMapForComposePreview()
        val name by map.name.collectAsStateWithLifecycle()

        val mapSizeState: MutableStateFlow<ResultL<Long?>> =
            remember { MutableStateFlow(ResultL.success(125468L)) }

        MapSettingsScreen(
            map = map,
            name = name,
            mapUpdateState = MutableStateFlow(
                MapSettingsViewModel.MapUpdateState(
                    map.id,
                    0.4f,
                    true
                )
            ),
            hasExtendedOffer = false,
            snackbarHostState = remember { SnackbarHostState() },
            mapSizeState = mapSizeState,
            onSetImage = {},
            onSetProjection = {},
            onNavigateToCalibration = {},
            onSetCalibrationPointNumber = {},
            onMapRename = {},
            onComputeMapSize = {},
            onArchiveMap = {},
            onStartRepair = {},
            onStartUpdate = {},
            onNavigateToShop = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun MapScreenErrorPreview() {
    TrekMeTheme {
        MapSettingsErrorScreen()
    }
}
