package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.features.common.presentation.ui.text.TextFieldCustom
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.map.domain.interactors.BeaconInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.screens.DistanceUnit.*
import com.peterlaurence.trekme.util.capitalize
import java.util.*

@Composable
fun BeaconEditStateful(
    beacon: Beacon,
    mapId: UUID,
    beaconInteractor: BeaconInteractor,
    onBackAction: () -> Unit
) {
    var name by remember { mutableStateOf(beacon.name) }
    var radius by remember { mutableStateOf(beacon.radius.toString()) }
    var distanceUnit by remember { mutableStateOf(Meters) }
    var latField by remember { mutableStateOf(beacon.lat.toString()) }
    var lonField by remember { mutableStateOf(beacon.lon.toString()) }
    var commentField by remember { mutableStateOf(beacon.comment) }

    fun makeBeacon(): Beacon {
        return Beacon(
            beacon.id, name,
            lat = latField.toDoubleOrNull() ?: beacon.lat,
            lon = lonField.toDoubleOrNull() ?: beacon.lon,
            radius = radius.toDoubleOrNull()?.let {
                convertToMeters(it, distanceUnit)
            }?.toFloat() ?: beacon.radius,
            comment = commentField
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackAction) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        BeaconEditScreen(
            modifier = Modifier.padding(paddingValues),
            name = name,
            radius = radius,
            distanceUnit = distanceUnit,
            latitudeField = latField,
            longitudeField = lonField,
            commentField = commentField,
            onNameChange = {
                name = it
                beaconInteractor.saveBeacon(mapId, makeBeacon())
            },
            onRadiusChange = { str ->
                val dotStr = str.replace(',', '.')
                val res = buildString {
                    var hasDot = false
                    dotStr.forEachIndexed { index, c ->
                        if (c == '.') {
                            if (index == 0) append('0')
                            if (!hasDot) append('.')
                            hasDot = true
                        } else {
                            if (c.isDigit()) append(c)
                        }
                    }
                }

                radius = res
                beaconInteractor.saveBeacon(mapId, makeBeacon())
            },
            onDistanceUnitChange = {
                distanceUnit = it
                beaconInteractor.saveBeacon(mapId, makeBeacon())
            },
            onLatChange = {
                val newLat = it.toDoubleOrNull()
                if (newLat != null) {
                    latField = it
                    beaconInteractor.saveBeacon(mapId, makeBeacon())
                }
            },
            onLonChange = {
                val newLon = it.toDoubleOrNull()
                if (newLon != null) {
                    lonField = it
                    beaconInteractor.saveBeacon(mapId, makeBeacon())
                }
            },
            onCommentChange = {
                commentField = it
                beaconInteractor.saveBeacon(mapId, makeBeacon())
            }
        )
    }
}

@Composable
private fun BeaconEditScreen(
    modifier: Modifier = Modifier,
    name: String,
    radius: String,
    distanceUnit: DistanceUnit,
    latitudeField: String,
    longitudeField: String,
    commentField: String,
    onNameChange: (String) -> Unit = {},
    onRadiusChange: (String) -> Unit = {},
    onDistanceUnitChange: (DistanceUnit) -> Unit = {},
    onLatChange: (String) -> Unit = {},
    onLonChange: (String) -> Unit = {},
    onCommentChange: (String) -> Unit = {}
) {
    var isShowingUnitsModal by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        TextFieldCustom(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = name,
            label = stringResource(id = R.string.marker_name_label),
            onTextChange = onNameChange,
            limit = 100
        )

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            text = stringResource(id = R.string.beacon_setting_title),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colors.primary
        )

        Row {
            TextFieldCustom(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = radius,
                label = stringResource(id = R.string.beacon_radius_label),
                onTextChange = onRadiusChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            TextFieldCustom(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = translateUnit(distanceUnit),
                label = stringResource(id = R.string.beacon_unit_label),
                onTextChange = { },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                showClearIcon = false,
                readOnly = true,
                enabled = true,
                interactionSource = remember { MutableInteractionSource() }
                    .also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect {
                                if (it is PressInteraction.Release) {
                                    isShowingUnitsModal = true
                                }
                            }
                        }
                    }
            )

            if (isShowingUnitsModal) {
                Dialog(
                    onDismissRequest = { isShowingUnitsModal = false }) {
                    Card(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            Modifier
                                .padding(top = 16.dp)
                                .width(IntrinsicSize.Min)) {
                            Text(
                                stringResource(id = R.string.beacon_unit_label),
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 21.dp, bottom = 8.dp)
                            )

                            for (unit in values()) {
                                Row(
                                    modifier = Modifier
                                        .clickable {
                                            onDistanceUnitChange(unit)
                                            isShowingUnitsModal = false
                                        }
                                        .padding(start = 8.dp, end = 24.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = distanceUnit == unit,
                                        onClick = {
                                            onDistanceUnitChange(unit)
                                            isShowingUnitsModal = false
                                        },
                                    )
                                    Spacer(modifier = Modifier.width(0.dp))
                                    Text(
                                        translateUnit(unit = unit, shortVariant = false).capitalize(),
                                        color = textColor(),
                                        style = TextStyle(
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.None
                                            )
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            text = stringResource(id = R.string.wgs84_label),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colors.primary
        )

        Row {
            TextFieldCustom(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = latitudeField,
                label = stringResource(id = R.string.latitude_short),
                onTextChange = onLatChange,
            )

            TextFieldCustom(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = longitudeField,
                label = stringResource(id = R.string.longitude_short),
                onTextChange = onLonChange,
            )
        }

        Text(
            text = stringResource(id = R.string.comment_label),
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colors.primary
        )

        var hasFocus by remember { mutableStateOf(false) }
        Box(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .border(
                    if (hasFocus) 2.dp else 1.dp,
                    MaterialTheme.colors.primary,
                    RoundedCornerShape(10.dp)
                )

        ) {
            BasicTextField(
                value = commentField,
                onValueChange = onCommentChange,
                textStyle = TextStyle(color = MaterialTheme.colors.onSurface),
                modifier = Modifier
                    .heightIn(min = 200.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onFocusChanged {
                        hasFocus = it.hasFocus
                    },
                singleLine = false,
                maxLines = 10,
                cursorBrush = SolidColor(MaterialTheme.colors.primary)
            )
        }
    }
}

private enum class DistanceUnit {
    Meters, KiloMeters, Yards, Miles
}

@Composable
private fun translateUnit(unit: DistanceUnit, shortVariant: Boolean = false): String {
    return when (unit) {
        Meters -> {
            if (shortVariant) {
                stringResource(id = R.string.meters_short)
            } else stringResource(id = R.string.meters)
        }
        KiloMeters -> {
            if (shortVariant) {
                stringResource(id = R.string.kilometers_short)
            } else stringResource(id = R.string.kilometers)
        }
        Yards -> {
            if (shortVariant) {
                stringResource(id = R.string.yards_short)
            } else stringResource(id = R.string.yards)

        }
        Miles -> {
            if (shortVariant) {
                stringResource(id = R.string.miles_short)
            } else stringResource(id = R.string.miles)
        }
    }
}

private fun convertToMeters(distance: Double, unit: DistanceUnit): Double {
    return when (unit) {
        Meters -> distance
        KiloMeters -> distance * 1000
        Yards -> distance * 0.9144
        Miles -> distance * 1609.34
    }
}

@Preview(widthDp = 350, heightDp = 500)
@Composable
private fun BeaconEditPreview() {
    TrekMeTheme {
        BeaconEditScreen(
            name = "A beacon",
            radius = "150",
            distanceUnit = Meters,
            latitudeField = "12.57",
            longitudeField = "46.58",
            commentField = "A comment"
        )
    }
}


