package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.text.TextFieldCustom
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorIndicator
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.ExcursionWaypointEditViewModel
import com.peterlaurence.trekme.util.encodeColor
import com.peterlaurence.trekme.util.parseColorL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcursionWaypointEditStateful(
    viewModel: ExcursionWaypointEditViewModel = hiltViewModel(),
    onBackAction: () -> Unit
) {
    val waypointState by viewModel.waypointState.collectAsState()
    val waypoint = waypointState ?: return
    val defaultColorState by viewModel.defaultColorState.collectAsState()
    val defaultColor = defaultColorState ?: return

    var name by remember(waypoint) { mutableStateOf(waypoint.name) }
    var latField by remember(waypoint) { mutableStateOf(waypoint.latitude.toString()) }
    var lonField by remember(waypoint) { mutableStateOf(waypoint.longitude.toString()) }
    var commentField by remember(waypoint) { mutableStateOf(waypoint.comment) }
    var colorField by remember { mutableStateOf(waypoint.color) }

    val saveMarker by rememberUpdatedState {
        viewModel.saveWaypoint(
            lat = latField.toDoubleOrNull(),
            lon = lonField.toDoubleOrNull(),
            name = name,
            comment = commentField,
            color = colorField
        )
    }

    var isShowingColorPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.marker_edit_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackAction) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                }
            )
        }
    ) { paddingValues ->
        WaypointEditScreen(
            modifier = Modifier.padding(paddingValues),
            name = name,
            latitudeField = latField,
            longitudeField = lonField,
            commentField = commentField,
            isUsingExcursionColor = waypoint.color == null,
            color = colorField ?: defaultColor,
            onNameChange = {
                name = it
                saveMarker()
            },
            onChooseColor = {
                isShowingColorPicker = true
            },
            onToggleUseExcursionColor = {
                if (it) {
                    colorField = null
                    saveMarker()
                } else {
                    isShowingColorPicker = true
                }
            },
            onLatChange = {
                val newLat = it.toDoubleOrNull()
                if (newLat != null) {
                    latField = it
                    saveMarker()
                }
            },
            onLonChange = {
                val newLon = it.toDoubleOrNull()
                if (newLon != null) {
                    lonField = it
                    saveMarker()
                }
            },
            onCommentChange = {
                commentField = it
                saveMarker()
            }
        )
    }

    if (isShowingColorPicker) {
        ColorPicker(
            initColor = parseColorL(colorField ?: defaultColor),
            onColorPicked = { c ->
                colorField = encodeColor(c)
                saveMarker()
                isShowingColorPicker = false
            },
            onCancel = { isShowingColorPicker = false }
        )
    }
}

@Composable
private fun WaypointEditScreen(
    modifier: Modifier = Modifier,
    name: String,
    latitudeField: String,
    longitudeField: String,
    commentField: String,
    isUsingExcursionColor: Boolean,
    color: String,
    onNameChange: (String) -> Unit = {},
    onChooseColor: () -> Unit = {},
    onLatChange: (String) -> Unit = {},
    onLonChange: (String) -> Unit = {},
    onCommentChange: (String) -> Unit = {},
    onToggleUseExcursionColor: (Boolean) -> Unit = {}
) {
    Column(
        modifier
            .fillMaxSize()
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
            text = stringResource(id = R.string.color_label),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable {
                    onToggleUseExcursionColor(!isUsingExcursionColor)
                }
        ) {
            Checkbox(
                checked = isUsingExcursionColor,
                onCheckedChange = onToggleUseExcursionColor
            )
            Text(text = stringResource(id = R.string.use_same_color_as_track))
        }

        if (!isUsingExcursionColor) {
            Button(
                onClick = onChooseColor,
                modifier = Modifier.padding(start = 16.dp, top = 0.dp, bottom = 8.dp)
            ) {
                ColorIndicator(color = color, onClick = onChooseColor)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(id = R.string.change_color))
            }
        }

        Text(
            text = stringResource(id = R.string.wgs84_label),
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row {
            TextFieldCustom(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = latitudeField,
                label = stringResource(id = R.string.latitude_short),
                onTextChange = onLatChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            TextFieldCustom(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                text = longitudeField,
                label = stringResource(id = R.string.longitude_short),
                onTextChange = onLonChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        Text(
            text = stringResource(id = R.string.comment_label),
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        var hasFocus by remember { mutableStateOf(false) }
        Box(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .border(
                    if (hasFocus) 2.dp else 1.dp,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(10.dp)
                )

        ) {
            BasicTextField(
                value = commentField,
                onValueChange = onCommentChange,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier
                    .heightIn(min = 200.dp)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onFocusChanged {
                        hasFocus = it.hasFocus
                    },
                singleLine = false,
                maxLines = 10,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}