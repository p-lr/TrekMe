package com.peterlaurence.trekme.features.map.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MarkersManageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons

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
            Column {
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
                    }
                )

                LazyColumn {
                    items(markers, key = { it.id }) {
                        Text(text = it.name)
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
private fun MarkersTopAppBar(onBackClick: () -> Unit,) {
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