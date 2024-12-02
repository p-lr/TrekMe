@file:OptIn(ExperimentalMaterial3Api::class)

package com.peterlaurence.trekme.features.map.presentation.ui.trackcreate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.map.presentation.ui.trackcreate.component.TrackLines
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.Loading
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.TrackCreateViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.TrackSegmentState
import kotlinx.coroutines.flow.StateFlow
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun TrackCreateStateful(
    viewModel: TrackCreateViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    when (uiState) {
        Loading -> LoadingScreen()
        is MapUiState -> {
            val mapUiState = (uiState as MapUiState)
            TrackCreateScaffold(
                mapState = mapUiState.mapState,
                trackState = mapUiState.trackCreateLayer.trackState,
                snackbarHostState = snackbarHostState,
                hasUndoState = mapUiState.trackCreateLayer.hasUndoState,
                hasRedoState = mapUiState.trackCreateLayer.hasRedoState,
                onUndo = mapUiState.trackCreateLayer::undo,
                onRedo = mapUiState.trackCreateLayer::reDo,
                onClose = {
                    onBack()  // TODO : ask confirmation
                }
            )
        }
    }
}

@Composable
private fun TrackCreateScaffold(
    mapState: MapState,
    trackState: StateFlow<List<TrackSegmentState>>,
    snackbarHostState: SnackbarHostState,
    hasUndoState: StateFlow<Boolean>,
    hasRedoState: StateFlow<Boolean>,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            TopBar(onCloseClick = onClose)
        },
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            TrackCreateScreen(
                mapState = mapState,
                trackState = trackState
            )

            FabRow(
                Modifier.align(Alignment.BottomEnd),
                hasUndoState,
                hasRedoState,
                onUndo,
                onRedo
            )
        }
    }
}

@Composable
private fun TopBar(onCloseClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.track_create_title),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_dialog))
            }
        },
    )
}

@Composable
private fun TrackCreateScreen(
    mapState: MapState,
    trackState: StateFlow<List<TrackSegmentState>>
) {
    MapUI(
        state = mapState
    ) {
        TrackLines(
            mapState = mapState,
            trackState = trackState
        )
    }
}

@Composable
private fun FabRow(
    modifier: Modifier = Modifier,
    hasUndoState: StateFlow<Boolean>,
    hasRedoState: StateFlow<Boolean>,
    onUndo: () -> Unit,
    onReDo: () -> Unit,
) {
    val hasUndo by hasUndoState.collectAsState()
    val hasRedo by hasRedoState.collectAsState()

    /* Using a surface to consume clicks around the FABs. */
    Surface(modifier = modifier, color = Color.Transparent) {
        Row(Modifier.padding(16.dp)) {
            if (hasUndo) {
                FloatingActionButton(
                    onClick = onUndo,
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_undo_white_24dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .alpha(disabledAlpha)
                        .semantics { role = Role.Image }
                        .size(56.dp),
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_undo_white_24dp),
                        modifier = Modifier.padding(16.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            if (hasRedo) {
                FloatingActionButton(
                    onClick = onReDo,
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_redo_white_24px),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }
            } else {
                Surface(
                    modifier = Modifier
                        .alpha(disabledAlpha)
                        .semantics { role = Role.Image }
                        .size(56.dp),
                    shape = CircleShape
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_redo_white_24px),
                        modifier = Modifier.padding(16.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

private const val disabledAlpha = 0.4f