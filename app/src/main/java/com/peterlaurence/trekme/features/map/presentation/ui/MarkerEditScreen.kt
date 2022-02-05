package com.peterlaurence.trekme.features.map.presentation.ui

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.features.common.presentation.ui.TextFieldCustom
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents

@Composable
fun MarkerEditScreen(
    marker: Marker,
    mapId: Int,
    markerId: String,
    mapFeatureEvents: MapFeatureEvents,
    mapInteractor: MapInteractor,
    onBackAction: () -> Unit
) {
    var name by remember {
        mutableStateOf(marker.name ?: "")
    }

    var latField by remember {
        mutableStateOf(marker.lat.toString())
    }

    var lonField by remember {
        mutableStateOf(marker.lon.toString())
    }

    var commentField by remember {
        mutableStateOf(marker.comment ?: "")
    }

    val onMarkerMove = remember {
        { mapFeatureEvents.postMarkerMovedEvent(marker, mapId, markerId) }
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
    ) {
        MarkerEditUi(
            name = name,
            latitudeField = latField,
            longitudeField = lonField,
            commentField = commentField,
            onNameChange = {
                name = it
                marker.name = it
                mapInteractor.saveMarkers(mapId)
            },
            onLatChange = {
                latField = it
                runCatching {
                    marker.lat = it.toDouble()
                }
                onMarkerMove()
                mapInteractor.saveMarkers(mapId)
            },
            onLonChange = {
                lonField = it
                runCatching {
                    marker.lon = it.toDouble()
                }
                onMarkerMove()
                mapInteractor.saveMarkers(mapId)
            },
            onCommentChange = {
                commentField = it
                marker.comment = it
                mapInteractor.saveMarkers(mapId)
            }
        )
    }
}

@Composable
fun MarkerEditUi(
    name: String,
    latitudeField: String,
    longitudeField: String,
    commentField: String,
    onNameChange: (String) -> Unit,
    onLatChange: (String) -> Unit,
    onLonChange: (String) -> Unit,
    onCommentChange: (String) -> Unit
) {
    val accentColor = colorResource(id = R.color.colorAccent)

    Column(
        Modifier
            .fillMaxWidth()
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
                .heightIn(min = 200.dp)
                .border(
                    if (hasFocus) 2.dp else 1.dp,
                    MaterialTheme.colors.primary,
                    RoundedCornerShape(10.dp)
                )

        ) {
            BasicTextField(
                value = commentField,
                onValueChange = onCommentChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .onFocusChanged {
                        hasFocus = it.hasFocus
                    }
            )
        }
    }
}


