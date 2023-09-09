package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.features.common.presentation.ui.screens.ErrorScreen
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ButtonSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.EditTextSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.HeaderSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.ListSetting
import com.peterlaurence.trekme.features.common.presentation.ui.settings.SettingDivider
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import com.peterlaurence.trekme.util.isFrench
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * UI that shows the settings for a given map. It provides the abilities to :
 *
 *  * Change map thumbnail image
 *  * Map calibration
 *
 *  * Choose the projection
 *  * Define the number of calibration point
 *  * Define the calibration points
 *
 *  * Change the map name
 *  * Save the map
 *
 * @since 16/04/2016 - Converted to Kotlin on 11/11/2020 - Converted to compose on 08/09/2023
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSettingsStateful(
    viewModel: MapSettingsViewModel = hiltViewModel(),
    onNavigateToCalibration: () -> Unit,
    onBackClick: () -> Unit
) {
    val settingMap by viewModel.mapFlow.collectAsStateWithLifecycle()
    val map = settingMap

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

    var isShowingAdvancedSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            var expandedMenu by remember { mutableStateOf(false) }
            TopAppBar(
                title = {
                    Text(
                        text = map?.name ?: stringResource(id = R.string.map_settings_frgmt_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "")
                    }
                },
                actions = {
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
                                                stringResource(id = R.string.map_settings_show_advanced)
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (map != null) {
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                ThumbnailSetting(viewModel, map)
                if (isShowingAdvancedSettings) {
                    SettingDivider()
                    CalibrationSetting(viewModel, map, onNavigateToCalibration)
                }
                SettingDivider()
                MapSettings(viewModel, map)
            }
        } else {
            ErrorScreen(message = stringResource(id = R.string.map_settings_error))
        }
    }
}

@Composable
private fun ThumbnailSetting(viewModel: MapSettingsViewModel, map: Map) {
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        /* Check if the request code is the one we are interested in */
        if (result.resultCode == Activity.RESULT_OK) {
            if (result.data == null) return@rememberLauncherForActivityResult
            val uri = result.data?.data
            if (uri != null) {
                viewModel.setMapImage(map, uri)
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
    viewModel: MapSettingsViewModel,
    map: Map,
    onNavigateToCalibration: () -> Unit
) {
    HeaderSetting(name = stringResource(id = R.string.calibration_preferences_category))
    ProjectionSetting(viewModel, map)
    CalibrationPointsSetting(viewModel, map)
    ButtonSetting(
        name = stringResource(id = R.string.calibration_button_txt),
        enabled = true,
        onClick = onNavigateToCalibration
    )
}

@Composable
private fun ProjectionSetting(viewModel: MapSettingsViewModel, map: Map) {
    val values = listOf(
        MercatorProjection.NAME to stringResource(id = R.string.pseudo_mercator),
        UniversalTransverseMercator.NAME to stringResource(id = R.string.utm),
        null to stringResource(id = R.string.projection_none)
    )
    var subTitle: String? by remember { mutableStateOf(values.toMap()[map.projection?.name]) }

    ListSetting(
        name = stringResource(id = R.string.projection_preferences_title),
        values = values,
        selectedIndex = values.indexOfFirst {
            val projName = it.first
            if (projName != null) {
                map.projection?.let { p ->
                    p.name == projName
                } ?: false
            } else true
        },
        subTitle = subTitle,
        onValueSelected = { _: Int, v: String? ->
            subTitle = values.toMap()[v]
            viewModel.setProjection(map, v)
        }
    )
}

@Composable
private fun CalibrationPointsSetting(viewModel: MapSettingsViewModel, map: Map) {
    val values = listOf(
        2 to "2",
        3 to "3",
        4 to "4"
    )
    var subTitle by remember { mutableStateOf(values.toMap()[map.calibrationPointsNumber]) }

    ListSetting(
        name = stringResource(id = R.string.points_number_preferences_title),
        values = values,
        selectedIndex = values.indexOfFirst {
            it.first == map.calibrationPointsNumber
        },
        subTitle = subTitle,
        onValueSelected = { _, v ->
            subTitle = values.toMap()[v]
            viewModel.setCalibrationPointsNumber(map, v)
        }
    )
}

@Composable
private fun MapSettings(viewModel: MapSettingsViewModel, map: Map) {
    HeaderSetting(name = stringResource(id = R.string.map_summary_category))
    ChangeNameSetting(viewModel, map)
    ComputeSizeSetting(viewModel, map)
    SaveSetting(viewModel, map)
}

@Composable
private fun ChangeNameSetting(viewModel: MapSettingsViewModel, map: Map) {
    EditTextSetting(
        name = stringResource(id = R.string.map_title),
        value = map.name,
        onValueChanged = {
            viewModel.renameMap(map, it)
        }
    )
}

@Composable
private fun ComputeSizeSetting(viewModel: MapSettingsViewModel, map: Map) {
    val mapSizeState by viewModel.mapSize.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ButtonSetting(
        name = if (mapSizeState.getOrNull() != null) {
            stringResource(id = R.string.map_size_string)
        } else {
            stringResource(id = R.string.map_size_compute_string)
        },
        enabled = mapSizeState.isSuccess && mapSizeState.getOrNull() == null,
        subTitle = if (mapSizeState.isLoading) {
            stringResource(id = R.string.map_size_computing)
        } else {
            mapSizeState.getOrNull()?.formatSize(context)
        },
        onClick = {
            viewModel.computeMapSize(map)
        }
    )
}

@Composable
private fun SaveSetting(viewModel: MapSettingsViewModel, map: Map) {
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
                    viewModel.archiveMap(map, uri)
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

/**
 * Converts a quantity expected to be in bytes into a [String]. For example:
 * 12_500 -> "12.5 Ko" (in French), or "12.5 KB" (in English)
 */
private fun Long.formatSize(context: Context): String {
    val byteStr = if (isFrench(context)) {
        "o"
    } else {
        "B"
    }
    var divider = 1.0
    val prefix = when {
        this < 1000 -> {
            ""
        }

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