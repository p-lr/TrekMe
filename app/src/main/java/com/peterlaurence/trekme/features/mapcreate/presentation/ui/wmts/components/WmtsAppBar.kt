package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.features.mapcreate.presentation.ui.getTitleForSource
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Collapsed
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Empty
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.SearchMode
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.TopBarState

/**
 * This app bar includes a search mode triggered when "search" button is clicked. In search mode, the
 * text input field occupies all horizontal space (other buttons are hidden). The search can be left
 * using the navigation icon.
 *
 * @since 2021/08/28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WmtsAppBar(
    state: TopBarState,
    wmtsSource: WmtsSource?,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    onBack: () -> Unit,
    onQueryTextSubmit: (String) -> Unit,
    onZoomOnPosition: () -> Unit,
    onShowLayerOverlay: () -> Unit,
    onUseTrack: () -> Unit,
    onNavigateToShop: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var isShowingTrackImportRedirection by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = wmtsSource?.let { getTitleForSource(it) } ?: "",
            )
        },
        navigationIcon = {
            IconButton(onClick = if (state is SearchMode) onCloseSearch else onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
            }
        },
        actions = {
            when (state) {
                is Empty -> {
                }
                is Collapsed -> {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onZoomOnPosition) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                            contentDescription = null,
                        )
                    }
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
                            if (state.hasOverlayLayers) {
                                DropdownMenuItem(
                                    onClick = onShowLayerOverlay,
                                    text = { Text(stringResource(id = R.string.mapcreate_overlay_layers)) }
                                )
                            }

                            DropdownMenuItem(
                                onClick = {
                                    if (state.hasTrackImport) {
                                        onUseTrack()
                                    } else {
                                        isShowingTrackImportRedirection = true
                                    }
                                },
                                text = {
                                    if (state.hasTrackImport) {
                                        Text(stringResource(id = R.string.mapcreate_from_track))
                                    } else {
                                        Row {
                                            Text(stringResource(id = R.string.mapcreate_from_track))
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_lock),
                                                modifier = Modifier
                                                    .padding(start = 8.dp)
                                                    .size(18.dp),
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                contentDescription = null
                                            )
                                        }
                                    }

                                }
                            )
                        }
                    }
                }
                is SearchMode -> {
                    SearchView(state.textValueState, onQueryTextSubmit)
                }
            }
        }
    )

    if (isShowingTrackImportRedirection) {
        AlertDialog(
            title = {
                Text(
                    stringResource(id = R.string.map_settings_trekme_extended_title),
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(id = R.string.mapcreate_from_track_rationale), Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(24.dp))
                    Image(
                        painter = painterResource(id = R.drawable.create_from_track),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)),
                        contentDescription = null
                    )
                }
            },
            onDismissRequest = { isShowingTrackImportRedirection = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        isShowingTrackImportRedirection = false
                        onNavigateToShop()
                    }
                ) {
                    Text(stringResource(id = R.string.ok_dialog))
                }
            },
            dismissButton = {
                TextButton(onClick = { isShowingTrackImportRedirection = false }) {
                    Text(text = stringResource(id = R.string.cancel_dialog_string))
                }
            }
        )
    }
}

@Composable
fun SearchView(state: MutableState<TextFieldValue>, onTextChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }

    TextField(
        value = state.value,
        onValueChange = { value ->
            state.value = value
            onTextChange(value.text)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 58.dp, end = 8.dp)
            .focusRequester(focusRequester),
        textStyle = TextStyle(fontSize = 18.sp),
        placeholder = {
            Text(
                text = stringResource(id = R.string.search_hint),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.alpha(0.5f)
            )
        },
        trailingIcon = {
            if (state.value != TextFieldValue("")) {
                IconButton(
                    onClick = {
                        state.value = TextFieldValue("")
                    }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "",
                        modifier = Modifier
                            .padding(5.dp)
                            .size(24.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RectangleShape,
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            cursorColor = MaterialTheme.colorScheme.tertiary,
            selectionColors = TextSelectionColors(
                handleColor = MaterialTheme.colorScheme.tertiary,
                backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            ),
            focusedIndicatorColor = MaterialTheme.colorScheme.tertiary
        )
    )

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { }
    }
}