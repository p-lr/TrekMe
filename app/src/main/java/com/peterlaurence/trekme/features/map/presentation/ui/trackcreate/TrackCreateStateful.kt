@file:OptIn(ExperimentalMaterial3Api::class)

package com.peterlaurence.trekme.features.map.presentation.ui.trackcreate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.screens.LoadingScreen
import com.peterlaurence.trekme.features.map.presentation.ui.trackcreate.component.TrackLines
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.Loading
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.TrackCreateViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.TrackSegmentState
import com.peterlaurence.trekme.util.fileNameAsCurrentDate
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
    var isShowingTrackNameDialog by remember { mutableStateOf(false) }

    when (uiState) {
        Loading -> LoadingScreen()
        is MapUiState -> {
            val mapUiState = (uiState as MapUiState)
            val segmentsState = mapUiState.trackCreateLayer.trackState.collectAsState()
            val isTrackEmpty by remember {
                derivedStateOf {
                    segmentsState.value.isEmpty()
                }
            }
            TrackCreateScaffold(
                mapState = mapUiState.mapState,
                segmentsState = segmentsState,
                isTrackEmpty = isTrackEmpty,
                snackbarHostState = snackbarHostState,
                hasUndoState = mapUiState.trackCreateLayer.hasUndoState,
                hasRedoState = mapUiState.trackCreateLayer.hasRedoState,
                onUndo = mapUiState.trackCreateLayer::undo,
                onRedo = mapUiState.trackCreateLayer::reDo,
                onSave = {
                    if (viewModel.isNewTrack()) {
                        isShowingTrackNameDialog = true
                    } else {
                        // TODO
                    }
                },
                onClose = {
                    onBack()  // TODO : ask confirmation
                }
            )
        }
    }

    if (isShowingTrackNameDialog) {
        TrackCreateDialog(
            onDismissRequest = { isShowingTrackNameDialog = false },
            onSave = { viewModel.save(it) }
        )
    }
}

@Composable
private fun TrackCreateScaffold(
    mapState: MapState,
    segmentsState: State<List<TrackSegmentState>>,
    isTrackEmpty: Boolean,
    snackbarHostState: SnackbarHostState,
    hasUndoState: StateFlow<Boolean>,
    hasRedoState: StateFlow<Boolean>,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit,
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
                segments = segmentsState.value
            )

            if (isTrackEmpty) {
                HelpTrackCreate(Modifier.align(Alignment.TopCenter))
            }

            val hasUndo by hasUndoState.collectAsState()
            val hasRedo by hasRedoState.collectAsState()

            FabSection(
                modifier = Modifier.align(Alignment.BottomEnd),
                hasUndo = hasUndo,
                hasRedo = hasRedo,
                onUndo = onUndo,
                onReDo = onRedo,
                onSave = onSave
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
    segments: List<TrackSegmentState>
) {
    MapUI(
        state = mapState
    ) {
        TrackLines(
            mapState = mapState,
            segments = segments
        )
    }
}

@Composable
private fun FabSection(
    modifier: Modifier = Modifier,
    hasUndo: Boolean,
    hasRedo: Boolean,
    onUndo: () -> Unit,
    onReDo: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.End
    ) {
        var saveExpanded by rememberSaveable { mutableStateOf(true) }
        if (hasUndo) {
            ExtendedFloatingActionButton(
                text = {
                    Text(stringResource(R.string.save_action))
                },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_save_white_24dp),
                        contentDescription = null
                    )
                },
                expanded = saveExpanded,
                modifier = Modifier.padding(end = 16.dp),
                onClick = {
                    saveExpanded = false
                    onSave()
                }
            )
        }

        /* Using a surface to consume clicks around the undo/redo FABs. */
        Surface(color = Color.Transparent) {
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
}

@Composable
private fun HelpTrackCreate(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            stringResource(R.string.track_create_start_help),
            style = TextStyle(hyphens = Hyphens.Auto, lineBreak = LineBreak.Paragraph),
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun TrackCreateDialog(
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit
) {
    var textFieldValue by remember {
        val name = fileNameAsCurrentDate()
        mutableStateOf(
            TextFieldValue(
                name,
                selection = TextRange(name.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.track_create_name_dialog),
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            TextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                modifier = Modifier.focusRequester(focusRequester)
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    onSave(textFieldValue.text)
                }
            ) {
                Text(text = stringResource(id = R.string.save_action))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(text = stringResource(id = R.string.cancel_dialog_string))
            }
        }
    )

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}

private const val disabledAlpha = 0.4f