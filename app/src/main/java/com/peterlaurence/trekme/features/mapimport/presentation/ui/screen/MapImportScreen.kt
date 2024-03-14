package com.peterlaurence.trekme.features.mapimport.presentation.ui.screen

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentGreen
import com.peterlaurence.trekme.features.common.presentation.ui.theme.dark_accentGreen
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipErrorEvent
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipFinishedEvent
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipMapImportedEvent
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipProgressEvent
import com.peterlaurence.trekme.features.mapimport.presentation.viewmodel.MapArchiveUiState
import com.peterlaurence.trekme.features.mapimport.presentation.viewmodel.MapImportViewModel
import com.peterlaurence.trekme.util.compose.LaunchedEffectWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MapImportUiStateful(
    viewModel: MapImportViewModel = hiltViewModel(),
    onShowMapList: () -> Unit,
    onMainMenuClick: () -> Unit
) {
    val isImporting by viewModel.isImporting.collectAsState()
    val archives by viewModel.archivesUiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.seekArchives(uri)
        }
    }

    var selection: UUID? by remember { mutableStateOf(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message = stringResource(id = R.string.snack_msg_show_map_list)
    val ok = stringResource(id = R.string.ok_dialog)
    LaunchedEffectWithLifecycle(viewModel.importSuccessEvent) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(message = message, actionLabel = ok)
            if (result == SnackbarResult.ActionPerformed) {
                onShowMapList()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            MapImportTopBar(onMainMenuClick, onImportClicked = { launcher.launch(null) })
        },
        floatingActionButton = {
            selection?.also {
                FloatingActionButton(
                    onClick = { viewModel.unArchive(it) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_file_download_24dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentDescription = stringResource(
                            id = R.string.add_track_btn_desc
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        if (archives.isEmpty()) {
            MapImportUi(
                Modifier.padding(paddingValues),
                isImporting = isImporting,
                onImportClicked = { launcher.launch(null) })

        } else {
            MapArchiveList(
                Modifier.padding(paddingValues),
                archives = archives,
                selection = selection,
                onArchiveSelected = { selection = it }
            )
        }
    }
}

@Composable
private fun MapArchiveList(
    modifier: Modifier = Modifier,
    archives: List<MapArchiveUiState>,
    selection: UUID?,
    onArchiveSelected: (UUID) -> Unit
) {
    LazyColumn(modifier) {
        itemsIndexed(archives, key = { _, it -> it.id }) { index, mapArchive ->
            val unzipEvent by mapArchive.unzipEvent.collectAsState()
            MapArchiveCard(
                selected = selection == mapArchive.id,
                index = index,
                onArchiveSelected = onArchiveSelected,
                mapArchive = mapArchive,
                unzipEvent = unzipEvent
            )
        }
    }
}

@Composable
private fun MapArchiveCard(
    selected: Boolean,
    index: Int,
    onArchiveSelected: (UUID) -> Unit,
    mapArchive: MapArchiveUiState,
    unzipEvent: UnzipEvent?
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        if (index % 2 == 1) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceColorAtElevation(
            1.dp
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(background)
            .padding(16.dp)
            .clickable { onArchiveSelected(mapArchive.id) },
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = mapArchive.name, fontWeight = FontWeight.Medium)
        when (unzipEvent) {
            is UnzipErrorEvent -> {
                StatusLine(success = false, stringResource(R.string.extraction_error))
            }

            is UnzipFinishedEvent -> {}
            is UnzipMapImportedEvent -> {
                Row {
                    when (unzipEvent.status) {
                        MapParseStatus.NEW_MAP, MapParseStatus.EXISTING_MAP -> {
                            StatusLine(
                                success = true,
                                message = stringResource(R.string.map_import_success)
                            )
                        }

                        MapParseStatus.UNKNOWN_MAP_ORIGIN, MapParseStatus.NO_MAP -> {
                            StatusLine(success = false, stringResource(R.string.map_import_error))
                        }
                    }
                }
            }

            is UnzipProgressEvent -> {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    progress = { unzipEvent.p / 100f },
                    strokeCap = StrokeCap.Round
                )
            }

            null -> {}
        }
    }
}

@Composable
fun StatusLine(success: Boolean, message: String) {
    Row(
        Modifier.padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = if (success) {
                painterResource(id = R.drawable.check)
            } else {
                painterResource(id = R.drawable.ic_error_outline_black_24dp)
            },
            colorFilter = ColorFilter.tint(
                if (success) {
                    if (isSystemInDarkTheme()) dark_accentGreen else accentGreen
                } else MaterialTheme.colorScheme.error
            ),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MapImportTopBar(
    onMainMenuClick: () -> Unit,
    onImportClicked: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(text = stringResource(id = R.string.import_title)) },
        navigationIcon = {
            IconButton(onClick = onMainMenuClick) {
                Icon(Icons.Filled.Menu, contentDescription = "")
            }
        },
        actions = {
            IconButton(
                onClick = { expandedMenu = true },
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
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    offset = DpOffset(0.dp, 0.dp)
                ) {
                    DropdownMenuItem(
                        onClick = {
                            expandedMenu = false
                            onImportClicked()
                        },
                        text = {
                            Text(stringResource(id = R.string.import_folder_select_btn))
                            Spacer(Modifier.weight(1f))
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MapImportUi(
    modifier: Modifier = Modifier,
    isImporting: Boolean,
    onImportClicked: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.import_24dp),
            contentDescription = stringResource(
                id = R.string.recording_import_desc
            ),
            modifier = Modifier
                .size(150.dp)
                .padding(24.dp)
                .alpha(0.5f)
        )

        Text(
            text = stringResource(id = R.string.import_desc),
            modifier = Modifier.padding(horizontal = 48.dp),
            textAlign = TextAlign.Justify
        )

        Button(
            onClick = onImportClicked,
            enabled = !isImporting,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            Text(stringResource(id = R.string.import_folder_select_btn))
        }

        if (isImporting) {
            LinearProgressIndicator()
        }
    }
}

@Preview(showBackground = true, widthDp = 350, heightDp = 400)
@Preview(showBackground = true, widthDp = 350, heightDp = 400, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MapImportUiPreview() {
    TrekMeTheme {
        MapImportUi(isImporting = false, onImportClicked = {})
    }
}

@Preview(showBackground = false)
@Composable
private fun CardPreview() {
    TrekMeTheme {
        val id = UUID.randomUUID()
        MapArchiveCard(
            selected = false,
            index = 0,
            onArchiveSelected = {},
            mapArchive = MapArchiveUiState(id, "A map", MutableStateFlow(null)),
            unzipEvent = UnzipProgressEvent(id, 40)
        )
    }
}

@Preview(showBackground = false)
@Composable
private fun CardPreview2() {
    TrekMeTheme {
        val id = UUID.randomUUID()
        MapArchiveCard(
            selected = false,
            index = 0,
            onArchiveSelected = {},
            mapArchive = MapArchiveUiState(id, "A map", MutableStateFlow(null)),
            unzipEvent = UnzipMapImportedEvent(id, null, MapParseStatus.NEW_MAP)
        )
    }
}

@Preview(showBackground = false)
@Composable
private fun CardPreview3() {
    TrekMeTheme {
        val id = UUID.randomUUID()
        MapArchiveCard(
            selected = false,
            index = 0,
            onArchiveSelected = {},
            mapArchive = MapArchiveUiState(id, "A map", MutableStateFlow(null)),
            unzipEvent = UnzipMapImportedEvent(id, null, MapParseStatus.UNKNOWN_MAP_ORIGIN)
        )
    }
}