package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationStateful(
    viewModel: CalibrationViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.acknowledgeableEvents

    when (uiState) {
        EmptyMap -> {}
        Loading -> {}
        is MapUiState -> {

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            if (events.isNotEmpty()) {
                val ok = stringResource(id = R.string.ok_dialog)
                val message = when (events.first()) {
                    CalibrationPointSaved -> stringResource(id = R.string.calibration_point_saved)
                    CalibrationError -> stringResource(id = R.string.calibration_status_error)
                }

                SideEffect {
                    scope.launch {
                        /* Dismiss the currently showing snackbar, if any */
                        snackbarHostState.currentSnackbarData?.dismiss()

                        snackbarHostState.showSnackbar(message, actionLabel = ok)
                    }
                    viewModel.acknowledgeEvent()
                }
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(id = R.string.calibration_preferences_category)) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "")
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues)) {
                    val mapUiState = uiState as MapUiState
                    val calibrationPoints = mapUiState.calibrationPoints
                    val calibrationPointModel = calibrationPoints.getOrNull(
                        mapUiState.selected.value.index
                    )

                    Calibration(
                        mapUiState.selected.value,
                        calibrationPointModel,
                        mapUiState.calibrationMethod,
                        onPointSelection = viewModel::onPointSelectionChange,
                        onSave = viewModel::onSave
                    )

                    MapUI(state = mapUiState.mapState)
                }
            }
        }
    }
}

@Composable
private fun Calibration(
    selected: PointId,
    calibrationPointModel: CalibrationPointModel?,
    calibrationMethod: CalibrationMethod,
    onPointSelection: (PointId) -> Unit,
    onSave: (PointId) -> Unit
) {
    Row(
        Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .height(96.dp)
                .weight(1f),
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            Row {
                LatLonPrefix(stringResource(id = R.string.latitude_short))
                LatLonTextField(
                    value = calibrationPointModel?.lat ?: "",
                    onValueChange = { calibrationPointModel?.lat = it }
                )
            }

            Row {
                LatLonPrefix(stringResource(id = R.string.longitude_short))
                LatLonTextField(
                    value = calibrationPointModel?.lon ?: "",
                    onValueChange = { calibrationPointModel?.lon = it }
                )
            }
        }

        PointSelector(
            Modifier.padding(start = 9.dp),
            calibrationMethod,
            activePointId = selected,
            onPointSelection = onPointSelection
        )

        FloatingActionButton(
            onClick = { onSave(selected) },
            Modifier
                .padding(start = 9.dp)
                .size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_save_white_24dp),
                contentDescription = stringResource(id = R.string.save_action)
            )
        }
    }
}

@Composable
private fun PointSelector(
    modifier: Modifier = Modifier,
    calibrationMethod: CalibrationMethod,
    activePointId: PointId,
    onPointSelection: (PointId) -> Unit
) {
    Column(modifier) {
        Row {
            IconButton(
                onClick = { onPointSelection(PointId.One) }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_one_black_24dp),
                    contentDescription = stringResource(id = R.string.first_calibration_pt),
                    tint = if (activePointId == PointId.One) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                )
            }

            val enabled = calibrationMethod != CalibrationMethod.SIMPLE_2_POINTS
            IconButton(
                onClick = { onPointSelection(PointId.Three) },
                enabled = calibrationMethod != CalibrationMethod.SIMPLE_2_POINTS
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_3_black_24dp),
                    contentDescription = stringResource(id = R.string.third_calibration_pt),
                    tint = if (activePointId == PointId.Three) {
                        MaterialTheme.colorScheme.primary
                    } else if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    }
                )
            }
        }
        Row {
            val enabled = calibrationMethod == CalibrationMethod.CALIBRATION_4_POINTS
            IconButton(
                onClick = { onPointSelection(PointId.Four) },
                enabled = enabled
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_4_black_24dp),
                    contentDescription = stringResource(id = R.string.fourth_calibration_pt),
                    tint = if (activePointId == PointId.Four) {
                        MaterialTheme.colorScheme.primary
                    } else if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    }
                )
            }

            IconButton(onClick = { onPointSelection(PointId.Two) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_two_black_24dp),
                    contentDescription = stringResource(id = R.string.second_calibration_pt),
                    tint = if (activePointId == PointId.Two) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun LatLonTextField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val unfocusedColor = MaterialTheme.colorScheme.onSurface
    val focusedColor = MaterialTheme.colorScheme.primary

    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth(1f)
            .onFocusChanged {
                focused = it.isFocused
            },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        decorationBox = { innerTextField ->
            Column {
                innerTextField()
                Divider(
                    color = if (focused) focusedColor else unfocusedColor,
                    thickness = if (focused) 2.dp else 1.dp
                )
                if (!focused) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        },
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
    )
}

@Composable
private fun LatLonPrefix(text: String) {
    Text(
        modifier = Modifier.padding(end = 8.dp),
        text = text,
        fontSize = 14.sp
    )
}


@Preview
@Composable
private fun PointSelector2PointsPreview() {
    var selected by remember {
        mutableStateOf(PointId.One)
    }
    PointSelector(
        calibrationMethod = CalibrationMethod.SIMPLE_2_POINTS,
        activePointId = selected,
        onPointSelection = {
            selected = it
        }
    )
}

@Preview
@Composable
private fun CalibrationPreview() {
    Calibration(PointId.One, null, CalibrationMethod.SIMPLE_2_POINTS, {}, {})
}