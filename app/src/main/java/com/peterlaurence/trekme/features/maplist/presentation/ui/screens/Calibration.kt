package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.CalibrationMethod
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun CalibrationStateful(
    viewModel: CalibrationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val events = viewModel.acknowledgeableEvents

    when (uiState) {
        EmptyMap -> {}
        Loading -> {}
        is MapUiState -> {

            val scaffoldState: ScaffoldState = rememberScaffoldState()
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
                        scaffoldState.snackbarHostState.currentSnackbarData?.dismiss()

                        scaffoldState.snackbarHostState
                            .showSnackbar(message, actionLabel = ok)
                    }
                    viewModel.acknowledgeEvent()
                }
            }

            Scaffold(scaffoldState = scaffoldState) {
                Column {
                    val mapUiState = uiState as MapUiState
                    val calibrationPoints = mapUiState.calibrationPoints
                    val calibrationPointModel = calibrationPoints.getOrNull(
                        mapUiState.selected.value.index
                    )
                    val calibrationMethod by mapUiState.calibrationMethodStateFlow.collectAsState()
                    /* Since the number of points can be changed anytime, don't allow selecting an
                     * otherwise impossible point. */
                    if (mapUiState.selected.value.index + 1 > calibrationMethod.pointCount) {
                        viewModel.onPointSelectionChange(PointId.One)
                    }

                    Calibration(
                        mapUiState.selected.value,
                        calibrationPointModel,
                        calibrationMethod,
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
                    value = calibrationPointModel?.lat?.toString() ?: "",
                    onValueChange = { calibrationPointModel?.lat = it.toDoubleOrNull() }
                )
            }

            Row {
                LatLonPrefix(stringResource(id = R.string.longitude_short))
                LatLonTextField(
                    value = calibrationPointModel?.lon?.toString() ?: "",
                    onValueChange = { calibrationPointModel?.lon = it.toDoubleOrNull() }
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
            backgroundColor = colorResource(id = R.color.colorAccent)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_save_white_24dp),
                contentDescription = null,
                tint = Color.White
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
                        colorResource(id = R.color.colorAccent)
                    } else Color.Black
                )
            }

            IconButton(
                onClick = { onPointSelection(PointId.Three) },
                enabled = calibrationMethod != CalibrationMethod.SIMPLE_2_POINTS
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_3_black_24dp),
                    contentDescription = stringResource(id = R.string.third_calibration_pt),
                    tint = if (activePointId == PointId.Three) {
                        colorResource(id = R.color.colorAccent)
                    } else Color.Black.copy(alpha = LocalContentAlpha.current)
                )
            }
        }
        Row {
            IconButton(
                onClick = { onPointSelection(PointId.Four) },
                enabled = calibrationMethod == CalibrationMethod.CALIBRATION_4_POINTS
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_4_black_24dp),
                    contentDescription = stringResource(id = R.string.fourth_calibration_pt),
                    tint = if (activePointId == PointId.Four) {
                        colorResource(id = R.color.colorAccent)
                    } else Color.Black.copy(alpha = LocalContentAlpha.current)
                )
            }

            IconButton(onClick = { onPointSelection(PointId.Two) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_looks_two_black_24dp),
                    contentDescription = stringResource(id = R.string.second_calibration_pt),
                    tint = if (activePointId == PointId.Two) {
                        colorResource(id = R.color.colorAccent)
                    } else Color.Black
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
    val unfocusedColor =
        MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
    val focusedColor = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)

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
        }
    )
}

@Composable
private fun LatLonPrefix(text: String) {
    Text(
        modifier = Modifier.padding(end = 8.dp),
        text = text,
        color = colorResource(id = R.color.colorGrey),
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