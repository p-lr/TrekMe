package com.peterlaurence.trekme.features.maplist.presentation.ui.screens

import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.scrollbar.drawVerticalScrollbar
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.model.MapItem
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.DownloadCard
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.WelcomeScreen
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.MapCard
import com.peterlaurence.trekme.features.maplist.presentation.ui.components.PendingScreen
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.DownloadState
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapListState
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapListViewModel
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapSettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

@Composable
fun MapListStateful(
    mapListViewModel: MapListViewModel = hiltViewModel(),
    mapSettingsViewModel: MapSettingsViewModel = hiltViewModel(),
    onNavigateToMapCreate: () -> Unit,
    onNavigateToMapSettings: () -> Unit,
    onNavigateToMap: (UUID) -> Unit,
    onNavigateToExcursionSearch: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val intents = object : MapListIntents {
        override fun onMapClicked(mapId: UUID) {
            mapListViewModel.setMap(mapId)
            onNavigateToMap(mapId)
        }

        override fun onMapFavorite(mapId: UUID) {
            mapListViewModel.toggleFavorite(mapId)
        }

        override fun onMapSettings(mapId: UUID) {
            mapListViewModel.onMapSettings(mapId)

            onNavigateToMapSettings()
        }

        override fun onSetMapImage(mapId: UUID, uri: Uri) {
            mapSettingsViewModel.setMapImage(mapId, uri)
        }

        override fun onMapDelete(mapId: UUID) {
            mapListViewModel.deleteMap(mapId)
        }

        override fun navigateToMapCreate(showOnBoarding: Boolean) {
            /* First, let the app globally know about the user choice */
            mapListViewModel.onNavigateToMapCreate(showOnBoarding)

            onNavigateToMapCreate()
        }

        override fun navigateToExcursionSearch() {
            onNavigateToExcursionSearch()
        }

        override fun onCancelDownload() {
            mapListViewModel.onCancelDownload()
        }
    }

    val mapListState by mapListViewModel.mapListState.collectAsState()
    val downloadState by mapListViewModel.downloadState.collectAsState()
    MapListUi(mapListState, downloadState, intents, onMainMenuClick = onMainMenuClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapListUi(
    state: MapListState,
    downloadState: DownloadState,
    intents: MapListIntents,
    onMainMenuClick: () -> Unit
) {
    Scaffold(
        topBar = {
            // Prevent the user from accessing the main menu while maps aren't loaded yet.
            if (!state.isMapListInitializing) {
                TopAppBar(
                    title = { Text(text = stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = onMainMenuClick) {
                            Icon(Icons.Filled.Menu, contentDescription = "")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            Modifier.padding(paddingValues)
        ) {
            if (downloadState.isDownloadPending) {
                DownloadCard(
                    Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                    downloadState.downloadProgress,
                    intents::onCancelDownload
                )
            }

            if (state.isMapListInitializing && !downloadState.isDownloadPending) {
                PendingScreen()
            } else {
                val listState = rememberLazyListState()
                if (state.mapItems.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .drawVerticalScrollbar(listState),
                        state = listState,
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.mapItems, key = { it.mapId }) { map ->
                            AnimatedMapCard(map, intents)
                        }
                    }
                } else {
                    WelcomeScreen(
                        onGoToMapCreation = intents::navigateToMapCreate,
                        onGoToExcursionSearch = intents::navigateToExcursionSearch
                    )
                }
            }
        }
    }
}

@Composable
private fun LazyItemScope.AnimatedMapCard(mapItem: MapItem, intents: MapListIntents) {
    MapCard(Modifier.animateItem(), mapItem, intents)
}

interface MapListIntents {
    fun onMapClicked(mapId: UUID)
    fun onMapFavorite(mapId: UUID)
    fun onMapSettings(mapId: UUID)
    fun onSetMapImage(mapId: UUID, uri: Uri)
    fun onMapDelete(mapId: UUID)
    fun navigateToMapCreate(showOnBoarding: Boolean)
    fun navigateToExcursionSearch()
    fun onCancelDownload()
}

@Preview(heightDp = 450)
@Preview(heightDp = 450, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapListPreview() {
    fun makeItem(name: String): MapItem {
        return MapItem(
            UUID.randomUUID(),
            titleFlow = MutableStateFlow(name),
            isDownloadPending = MutableStateFlow(false),
            image = MutableStateFlow(null)
        )
    }

    val mapList = listOf(
        makeItem("A map 1"),
        makeItem("A map 2"),
        makeItem("A map 3"),
        makeItem("A map 4")
    )

    var mapListState by remember {
        mutableStateOf(
            MapListState(mapList, false)
        )
    }

    val intents = object : MapListIntents {
        override fun onMapClicked(mapId: UUID) {
        }

        override fun onMapFavorite(mapId: UUID) {
            val newList = mapListState.mapItems.map {
                if (it.mapId == mapId) {
                    it.copy(isFavorite = !it.isFavorite)
                } else it
            }.sortedByDescending { it.isFavorite }
            mapListState = MapListState(newList, false)
        }

        override fun onMapSettings(mapId: UUID) {
        }

        override fun onSetMapImage(mapId: UUID, uri: Uri) {
        }

        override fun onMapDelete(mapId: UUID) {
            val newList = mapListState.mapItems.filter {
                it.mapId != mapId
            }
            mapListState = MapListState(newList, false)
        }

        override fun navigateToMapCreate(showOnBoarding: Boolean) {
        }

        override fun navigateToExcursionSearch() {
        }

        override fun onCancelDownload() {
        }
    }

    TrekMeTheme {
        MapListUi(
            state = mapListState,
            downloadState = DownloadState(25, true),
            intents = intents,
            onMainMenuClick = {}
        )
    }
}

@Preview(heightDp = 450)
@Composable
private fun PendingPreview() {
    TrekMeTheme {
        PendingScreen()
    }
}