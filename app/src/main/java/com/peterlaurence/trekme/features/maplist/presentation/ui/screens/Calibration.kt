package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.CalibrationMethod
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.CalibrationViewModel
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.EmptyMap
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.Loading
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapUiState
import ovh.plrapps.mapcompose.ui.MapUI

@Composable
fun CalibrationStateful(
    viewModel: CalibrationViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column {
        Calibration()

        when (uiState) {
            EmptyMap -> {}
            Loading -> {}
            is MapUiState -> MapUI(state = (uiState as MapUiState).mapState)
        }
    }
}

@Composable
private fun Calibration() {
    var selected by remember {
        mutableStateOf(PointId.One)
    }
    var latitude by remember {
        mutableStateOf("")
    }
    var longitude by remember {
        mutableStateOf("")
    }

    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            Modifier
                .weight(1f)
        ) {
            TextField(
                latitude, onValueChange = { latitude = it },
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth(1f),
                leadingIcon = { Text(text = stringResource(id = R.string.latitude_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent
                )
            )

            TextField(
                longitude, onValueChange = { longitude = it },
                modifier = Modifier.height(48.dp),
                leadingIcon = { Text(text = stringResource(id = R.string.longitude_short)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent
                )
            )
        }

        PointSelector(
            Modifier.padding(start = 9.dp),
            CalibrationMethod.SIMPLE_2_POINTS,
            activePointId = selected,
            onPointSelection = {
                selected = it
            }
        )

        FloatingActionButton(
            onClick = { /*TODO*/ },
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

private enum class PointId {
    One, Two, Three, Four
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
    Calibration()
}