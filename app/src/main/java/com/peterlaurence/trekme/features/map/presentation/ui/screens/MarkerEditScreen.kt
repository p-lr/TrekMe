package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.common.presentation.ui.text.TextFieldCustom
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.map.domain.interactors.MarkerInteractor
import java.util.*

@Composable
fun MarkerEditStateful(
    marker: Marker,
    mapId: UUID,
    markerInteractor: MarkerInteractor,
    onBackAction: () -> Unit
) {
    var name by remember { mutableStateOf(marker.name) }
    var latField by remember { mutableStateOf(marker.lat.toString()) }
    var lonField by remember { mutableStateOf(marker.lon.toString()) }
    var commentField by remember { mutableStateOf(marker.comment) }

    fun makeMarker(): Marker {
        return Marker(
            marker.id,
            lat = latField.toDoubleOrNull() ?: marker.lat,
            lon = lonField.toDoubleOrNull() ?: marker.lon,
            name = name,
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
        MarkerEditScreen(
            modifier = Modifier.padding(paddingValues),
            name = name,
            latitudeField = latField,
            longitudeField = lonField,
            commentField = commentField,
            onNameChange = {
                name = it
                markerInteractor.saveMarker(mapId, makeMarker())
            },
            onLatChange = {
                val newLat = it.toDoubleOrNull()
                if (newLat != null) {
                    latField = it
                    markerInteractor.saveMarker(mapId, makeMarker())
                }
            },
            onLonChange = {
                val newLon = it.toDoubleOrNull()
                if (newLon != null) {
                    lonField = it
                    markerInteractor.saveMarker(mapId, makeMarker())
                }
            },
            onCommentChange = {
                commentField = it
                markerInteractor.saveMarker(mapId, makeMarker())
            }
        )
    }
}

@Composable
private fun MarkerEditScreen(
    modifier: Modifier = Modifier,
    name: String,
    latitudeField: String,
    longitudeField: String,
    commentField: String,
    onNameChange: (String) -> Unit = {},
    onLatChange: (String) -> Unit = {},
    onLonChange: (String) -> Unit = {},
    onCommentChange: (String) -> Unit = {}
) {
    val accentColor = accentColor()

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
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
            text = stringResource(id = R.string.wgs84_label),
            fontWeight = FontWeight.Bold,
            color = accentColor
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
            modifier = Modifier.padding(top = 20.dp),
            fontWeight = FontWeight.Bold,
            color = accentColor
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

@Preview(widthDp = 350, heightDp = 500)
@Composable
private fun MarkerEditPreview() {
    TrekMeTheme {
        MarkerEditScreen(
            name = "A marker",
            latitudeField = "12.57",
            longitudeField = "46.58",
            commentField = "A comment"
        )
    }
}


