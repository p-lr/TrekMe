package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.text.TextFieldCustom
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorIndicator
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkerEditViewModel
import com.peterlaurence.trekme.util.encodeColor
import com.peterlaurence.trekme.util.parseColorL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerEditStateful(
    viewModel: MarkerEditViewModel = hiltViewModel(),
    onBackAction: () -> Unit
) {
    val markerState by viewModel.markerState.collectAsState()
    val marker = markerState ?: return

    var name by remember(marker) { mutableStateOf(marker.name) }
    var latField by remember { mutableStateOf(marker.lat.toString()) }
    var lonField by remember { mutableStateOf(marker.lon.toString()) }
    var commentField by remember { mutableStateOf(marker.comment) }
    var colorField by remember { mutableStateOf(marker.color) }

    val saveMarker by rememberUpdatedState {
        viewModel.saveMarker(
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
            color = colorField,
            onNameChange = {
                name = it
                saveMarker()
            },
            onChooseColor = { isShowingColorPicker = true },
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
            initColor = parseColorL(marker.color),
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
    color: String,
    onNameChange: (String) -> Unit = {},
    onChooseColor: () -> Unit = {},
    onLatChange: (String) -> Unit = {},
    onLonChange: (String) -> Unit = {},
    onCommentChange: (String) -> Unit = {}
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

        Button(
            onClick = onChooseColor,
            modifier = Modifier.padding(start = 16.dp, top = 0.dp, bottom = 8.dp)
        ) {
            ColorIndicator(color = color, onClick = onChooseColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(id = R.string.change_color))
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


@Preview(widthDp = 350, heightDp = 500, showBackground = true)
@Composable
private fun MarkerEditPreview() {
    TrekMeTheme {
        WaypointEditScreen(
            name = "A marker",
            latitudeField = "12.57",
            longitudeField = "46.58",
            commentField = "A comment",
            color = "#ffa5bef4"
        )
    }
}


