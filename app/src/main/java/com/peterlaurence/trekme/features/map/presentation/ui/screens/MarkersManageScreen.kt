package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkersManageViewModel
import com.peterlaurence.trekme.util.darkenColor
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun MarkersManageStateful(
    viewModel: MarkersManageViewModel = hiltViewModel(),
    onNavigateToMap: () -> Unit,
    onBackClick: () -> Unit
) {
    val markers by viewModel.getMarkersFlow().collectAsState()
    var search by remember { mutableStateOf("") }

    var searchJob: Job? = null

    val filteredMarkers by produceState(
        initialValue = markers,
        key1 = search
    ) {
        searchJob?.cancel()
        searchJob = launch(Dispatchers.Default) {
            val lowerCase = search.lowercase().trim()
            value = markers.filter {
                it.name.lowercase().contains(lowerCase)
            }
        }
    }

    MarkersManageScreen(
        markers = filteredMarkers,
        hasMarkers = markers.isNotEmpty(),
        onNewSearch = { search = it },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarkersManageScreen(
    markers: List<Marker>,
    hasMarkers: Boolean,
    onNewSearch: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            MarkersTopAppBar(onBackClick)
        }
    ) { paddingValues ->
        if (hasMarkers) {
            var searchText by remember { mutableStateOf("") }
            Column(
                Modifier.padding(paddingValues)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        onNewSearch(it)
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
                            contentDescription = stringResource(id = R.string.search_hint)
                        )
                    },
                    placeholder = {
                        Text(text = stringResource(id = R.string.search_hint))
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.padding(horizontal = 8.dp)
//                    modifier = Modifier.border(1.dp, Color.DarkGray, shape = RoundedCornerShape(50))
                )

                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(markers, key = { it.id }) {
                        MarkerCard(
                            modifier = Modifier.animateItemPlacement(),
                            marker = it
                        )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkersTopAppBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.markers_manage_title)) },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        },
        actions = { }
    )
}

@Composable
private fun MarkerCard(
    modifier: Modifier = Modifier,
    marker: Marker
) {
    ElevatedCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val backgroundColor = parseColor(marker.color)
            val strokeColor = darkenColor(backgroundColor, 0.15f)

            Marker(
                modifier = Modifier
                    .graphicsLayer {
                        clip = true
                        translationY = 10.dp.toPx()
                    }
                    .padding(horizontal = 8.dp),
                backgroundColor = Color(backgroundColor),
                strokeColor = Color(strokeColor),
                isStatic = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (marker.name.isNotEmpty()) {
                Text(text = marker.name)
            } else {
                Text(
                    text = stringResource(id = R.string.markers_manage_no_name),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            VerticalDivider(Modifier.height(24.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                modifier = Modifier.padding(horizontal = 8.dp),
                contentDescription = stringResource(id = R.string.open_dialog)
            )
        }
    }
}


@Preview
@Composable
private fun MarkersManagePreview() {
    TrekMeTheme {
        val markers = buildList<Marker> {
            repeat(6) {
                add(Marker(lat = 12.6, lon = 2.57, name = "marker-$it"))
                add(Marker(lat = 12.6, lon = 2.57))
            }
        }

        MarkersManageScreen(
            markers = markers,
            hasMarkers = true,
            onNewSearch = {},
            onBackClick = {}
        )
    }
}
