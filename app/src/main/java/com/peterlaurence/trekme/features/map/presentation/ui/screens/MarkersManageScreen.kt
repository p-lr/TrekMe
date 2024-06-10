package com.peterlaurence.trekme.features.map.presentation.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionPhoto
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.ColorPicker
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkersManageViewModel
import com.peterlaurence.trekme.util.darkenColor
import com.peterlaurence.trekme.util.encodeColor
import com.peterlaurence.trekme.util.parseColor
import com.peterlaurence.trekme.util.randomString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

@Composable
fun MarkersManageStateful(
    viewModel: MarkersManageViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val markers by viewModel.getMarkersFlow().collectAsState()
    val excursionWaypoints by viewModel.excursionWaypointFlow.collectAsState()
    // if changing excursion waypoint color causes recomposition of the whole screen and maybe a flicker,
    // then try observing each excursion using the state below.
//    val waypoints by viewModel.getExcursionWaypointsFlow2().collectAsState()
    var search by remember { mutableStateOf("") }

    var searchJob1: Job? = null
    var selectedMarkerIds by rememberSaveable(markers) { mutableStateOf<List<String>>(emptyList()) }

    val filteredMarkers by produceState(
        initialValue = markers.map { SelectableMarker(it, isSelected = false) },
        key1 = search,
        key2 = markers,
        key3 = selectedMarkerIds
    ) {
        searchJob1?.cancel()
        searchJob1 = launch(Dispatchers.Default) {
            val lowerCase = search.lowercase().trim()
            value = markers.filter {
                it.name.lowercase().contains(lowerCase)
            }.map {
                SelectableMarker(it, isSelected = it.id in selectedMarkerIds)
            }
        }
    }

    var searchJob2: Job? = null
    var selectedWaypoints by rememberSaveable(excursionWaypoints) {
        mutableStateOf<List<String>>(emptyList())
    }

    val filteredWaypoints by produceState(
        initialValue = excursionWaypoints.mapValues {
            it.value.map { wpt -> SelectableWaypoint(wpt, isSelected = false) }
        },
        key1 = search,
        key2 = excursionWaypoints,
        key3 = selectedWaypoints
    ) {
        searchJob2?.cancel()
        searchJob2 = launch(Dispatchers.Default) {
            value = if (search.isNotEmpty()) {
                val lowerCase = search.lowercase().trim()
                excursionWaypoints.mapValues {
                    it.value.filter { wpt ->
                        wpt.name.lowercase().contains(lowerCase)
                    }.map { wpt ->
                        SelectableWaypoint(wpt, isSelected = wpt.id in selectedWaypoints)
                    }
                }
            } else excursionWaypoints.mapValues {
                it.value.map { wpt ->
                    SelectableWaypoint(wpt, isSelected = wpt.id in selectedWaypoints)
                }
            }
        }
    }

    val isSelectionMode by remember {
        derivedStateOf {
            filteredMarkers.any { it.isSelected }
                    || filteredWaypoints.any { it.value.any { wpt -> wpt.isSelected } }
        }
    }

    fun setMarkerSelection(marker: Marker, selection: Boolean) {
        if (selection && marker.id !in selectedMarkerIds) {
            selectedMarkerIds += marker.id
        } else {
            selectedMarkerIds -= marker.id
        }
    }

    fun setWaypointSelection(waypoint: ExcursionWaypoint, selection: Boolean) {
        if (selection && waypoint.id !in selectedWaypoints) {
            selectedWaypoints += waypoint.id
        } else {
            selectedWaypoints -= waypoint.id
        }
    }

    MarkersManageScreen(
        markers = filteredMarkers,
        excursionWaypoints = filteredWaypoints,
        isSelectionMode = isSelectionMode,
        hasMarkers = markers.isNotEmpty() || excursionWaypoints.any { it.value.isNotEmpty() },
        onNewSearch = { search = it },
        onGoToMarker = {
            onBackClick()
            viewModel.goToMarker(it)
        },
        onGoToExcursionWaypoint = { excursionRef, wpt ->
            onBackClick()
            viewModel.goToExcursionWaypoint(excursionRef, wpt)
        },
        onDeleteMarker = {
            viewModel.deleteMarker(it)
        },
        onDeleteWaypoint = { excursionRef, wpt ->
            viewModel.deleteWaypoint(excursionRef.id, wpt)
        },
        onToggleMarkerSelection = { marker, s -> setMarkerSelection(marker, s) },
        onToggleWaypointSelection = { wpt, s -> setWaypointSelection(wpt, s) },
        onToggleSelectAll = {
            if (isSelectionMode) {
                selectedMarkerIds = emptyList()
                selectedWaypoints = emptyList()
            } else {
                selectedMarkerIds = filteredMarkers.map { it.marker.id }
                selectedWaypoints = filteredWaypoints.flatMap { it.value.map { wpt -> wpt.waypoint.id } }
            }
        },
        onChangeColor = { color ->
            viewModel.updateMarkersColor(filteredMarkers.filter { it.isSelected }.map { it.marker }, color)
            filteredWaypoints.forEach { (excursionRef, wpts) ->
                viewModel.updateWaypointsColor(
                    excursionRef.id, wpts.filter { it.isSelected }.map { it.waypoint }, color
                )
            }
        },
        onDeleteSelected = {
            viewModel.deleteMarkers(filteredMarkers.filter { it.isSelected }.map { it.marker })
            filteredWaypoints.forEach { (excursionRef, wpts) ->
                viewModel.deleteWaypoints(excursionRef.id, wpts.filter { it.isSelected }.map { it.waypoint })
            }
        },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkersManageScreen(
    markers: List<SelectableMarker>,
    excursionWaypoints: Map<ExcursionRef, List<SelectableWaypoint>>,
    isSelectionMode: Boolean,
    hasMarkers: Boolean,
    onNewSearch: (String) -> Unit,
    onGoToMarker: (Marker) -> Unit,
    onDeleteMarker: (Marker) -> Unit,
    onDeleteWaypoint: (excursionRef: ExcursionRef, ExcursionWaypoint) -> Unit,
    onGoToExcursionWaypoint: (excursionRef: ExcursionRef, ExcursionWaypoint) -> Unit,
    onToggleMarkerSelection: (Marker, Boolean) -> Unit,
    onToggleWaypointSelection: (ExcursionWaypoint, Boolean) -> Unit,
    onToggleSelectAll: () -> Unit,
    onChangeColor: (color: String) -> Unit,
    onDeleteSelected: () -> Unit,
    onBackClick: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            MarkersTopAppBar(
                isSelectionMode = isSelectionMode,
                hasMarkers = hasMarkers,
                onToggleSelectAll = onToggleSelectAll,
                onChangeColor = { showColorPicker = true },
                onDeleteSelected = onDeleteSelected,
                onBackClick
            )
        }
    ) { paddingValues ->
        if (hasMarkers) {
            Column(
                Modifier.padding(paddingValues)
            ) {
                SearchBar(onNewSearch = onNewSearch)

                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(markers, key = { it.marker.id }) {
                        PinCard(
                            modifier = Modifier.animateItemPlacement(),
                            name = it.marker.name,
                            color = it.marker.color,
                            selected = if (isSelectionMode) it.isSelected else null,
                            onToggleSelection = { selected ->
                                onToggleMarkerSelection(it.marker, selected)
                            },
                            onGoToPin = { onGoToMarker(it.marker) },
                            onDelete = { onDeleteMarker(it.marker) }
                        )
                    }
                    for (excursion in excursionWaypoints.keys) {
                        val name = excursion.name.value
                        val wpts = excursionWaypoints[excursion]
                        if (wpts.isNullOrEmpty()) continue
                        item(key = excursion.id) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        items(wpts, key = { it.waypoint.id }) {
                            val excursionColor by excursion.color.collectAsState()
                            PinCard(
                                modifier = Modifier.animateItemPlacement(),
                                name = it.waypoint.name,
                                color = it.waypoint.color ?: excursionColor,
                                selected = if (isSelectionMode) it.isSelected else null,
                                onToggleSelection = { selected ->
                                    onToggleWaypointSelection(it.waypoint, selected)
                                },
                                onGoToPin = { onGoToExcursionWaypoint(excursion, it.waypoint) },
                                onDelete = { onDeleteWaypoint(excursion, it.waypoint) }
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                stringResource(id = R.string.no_markers_message),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
            )
        }
    }

    if (showColorPicker) {
        ColorPicker(
            onColorPicked = {
                onChangeColor(encodeColor(it))
                showColorPicker = false
            },
            onCancel = { showColorPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkersTopAppBar(
    isSelectionMode: Boolean,
    hasMarkers: Boolean,
    onToggleSelectAll: () -> Unit,
    onChangeColor: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.markers_manage_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        },
        actions = {
            if (!hasMarkers) return@TopAppBar
            var expanded by remember { mutableStateOf(false) }

            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.width(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                )
            }
            Box(
                Modifier
                    .height(24.dp)
                    .wrapContentSize(Alignment.BottomEnd, true)
            ) {
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        onClick = {
                            if (isSelectionMode) {
                                expanded = false
                                onToggleSelectAll()
                            } else onToggleSelectAll()
                        },
                        text = {
                            if (isSelectionMode) {
                                Text(stringResource(id = R.string.markers_manage_deselect_all))
                            } else {
                                Text(stringResource(id = R.string.markers_manage_select_all))
                            }
                        }
                    )
                    if (isSelectionMode) {
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onChangeColor()
                            },
                            text = { Text(stringResource(id = R.string.markers_manage_change_color)) }
                        )
                        DropdownMenuItem(
                            onClick = {
                                expanded = false
                                onDeleteSelected()
                            },
                            text = { Text(stringResource(id = R.string.markers_manage_delete)) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    onNewSearch: (String) -> Unit,
) {
    var searchText by rememberSaveable {
        mutableStateOf("")
    }
    Surface(
        Modifier
            .padding(start = 8.dp, end = 8.dp)
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(40.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(50)
            ),
        shape = RoundedCornerShape(50),
    ) {
        BasicTextField(
            value = searchText,
            onValueChange = {
                searchText = it
                onNewSearch(it)
            },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            Row(
                modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_search_24),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentDescription = stringResource(id = R.string.search_hint)
                )

                Box(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchText.isEmpty()) {
                        Text(
                            stringResource(id = R.string.excursion_search_button),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.67f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }

                // Trailing icon
                if (searchText.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchText = ""
                            onNewSearch("")
                        }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.close_circle_outline),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinCard(
    modifier: Modifier = Modifier,
    name: String,
    color: String,
    selected: Boolean?,
    onToggleSelection: (Boolean) -> Unit,
    onGoToPin: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.pointerInput(selected) {
            detectTapGestures(
                onLongPress = { onToggleSelection(if (selected == null) true else !selected) },
                onTap = {
                    if (selected != null) {
                        onToggleSelection(!selected)
                    }
                }
            )
        },
        elevation = if (selected != null && selected) CardDefaults.elevatedCardElevation(
            defaultElevation = 3.dp
        ) else CardDefaults.elevatedCardElevation()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backgroundColor = parseColor(color)
            val strokeColor = darkenColor(backgroundColor, 0.15f)

            Marker(
                modifier = Modifier
                    .graphicsLayer {
                        clip = true
                        translationY = 12.dp.toPx()
                    }
                    .padding(horizontal = 8.dp),
                backgroundColor = Color(backgroundColor),
                strokeColor = Color(strokeColor),
                isStatic = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (name.isNotEmpty()) {
                Text(text = name)
            } else {
                Text(
                    text = stringResource(id = R.string.markers_manage_no_name),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            if (selected != null) {
                IconButton(
                    onClick = { onToggleSelection(!selected) },
                ) {
                    Icon(
                        painterResource(id = if (selected) R.drawable.ic_check_circle else R.drawable.ic_circle_outline),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = { expandedMenu = true },
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                    )
                }
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
                                onGoToPin()
                            },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(id = R.string.markers_manage_goto))
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            expandedMenu = false
                                            onGoToPin()
                                        }
                                    ) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_gps_fixed_24dp),
                                            contentDescription = stringResource(id = R.string.open_dialog)
                                        )
                                    }
                                }
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                expandedMenu = false
                                onToggleSelection(true)
                            },
                            text = {
                                Text(stringResource(id = R.string.markers_manage_select))
                                Spacer(Modifier.weight(1f))
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                expandedMenu = false
                                // TODO
                            },
                            text = {
                                Text(stringResource(id = R.string.markers_manage_edit))
                                Spacer(Modifier.weight(1f))
                            }
                        )

                        DropdownMenuItem(
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            },
                            text = {
                                Text(stringResource(id = R.string.delete_dialog))
                                Spacer(Modifier.weight(1f))
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class SelectableMarker(val marker: Marker, val isSelected: Boolean)
private data class SelectableWaypoint(val waypoint: ExcursionWaypoint, val isSelected: Boolean)

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MarkersManagePreview() {
    TrekMeTheme {
        val markers = buildList {
            repeat(3) {
                add(Marker(lat = 12.6, lon = 2.57, name = "marker-$it"))
                add(Marker(lat = 12.6, lon = 2.57))
            }
        }

        val ref1: ExcursionRef = object : ExcursionRef {
            override val id: String = "id"
            override val name: MutableStateFlow<String> = MutableStateFlow("Excursion name")
            override val visible: MutableStateFlow<Boolean> = MutableStateFlow(true)
            override val color: MutableStateFlow<String> = MutableStateFlow("#2196f3")
        }

        fun makeExcursionWaypoint(): ExcursionWaypoint {
            return object : ExcursionWaypoint {
                override val id: String = UUID.randomUUID().toString()
                override val name: String = randomString(6)
                override val latitude: Double = 0.0
                override val longitude: Double = 0.0
                override val elevation: Double? = null
                override val comment: String = randomString(10)
                override val photos: List<ExcursionPhoto> = emptyList()
                override val color: String? = if (Random.nextBoolean()) "#ffffff" else null
            }
        }

        val excursionWaypoints = buildMap {
            put(ref1, buildList {
                repeat(5) {
                    add(makeExcursionWaypoint())
                }
            })
        }

        MarkersManageScreen(
            markers = markers.map { SelectableMarker(it, isSelected = true) },
            excursionWaypoints = excursionWaypoints.mapValues {
                 it.value.map { wpt -> SelectableWaypoint(wpt, false) }
            },
            isSelectionMode = false,
            hasMarkers = true,
            onNewSearch = {},
            onGoToMarker = {},
            onGoToExcursionWaypoint = { _, _ -> },
            onDeleteMarker = {},
            onDeleteWaypoint = { _, _ -> },
            onToggleMarkerSelection = { _, _ -> },
            onToggleWaypointSelection = { _, _ -> },
            onToggleSelectAll = {},
            onChangeColor = {},
            onDeleteSelected = {},
            onBackClick = {}
        )
    }
}
