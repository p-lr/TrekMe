package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
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
            if (state !is SearchMode) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                }
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
                    SearchView(state.lastSearch, onBack = onCloseSearch, onQueryTextSubmit)
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
                    Text(
                        stringResource(id = R.string.mapcreate_from_track_rationale),
                        Modifier.fillMaxWidth()
                    )
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
fun SearchView(lastSearch: String, onBack: () -> Unit, onTextChange: (String) -> Unit) {
    var searchText by remember {
        mutableStateOf(TextFieldValue(lastSearch, selection = TextRange(lastSearch.length)))
    }
    Surface(
        Modifier
            .padding(start = 12.dp, end = 12.dp)
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
        val focusRequester = remember { FocusRequester() }
        BasicTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = searchText,
            onValueChange = {
                searchText = it
                onTextChange(it.text)
            },
            singleLine = true,
            textStyle = androidx.compose.material.LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Leading icon
                IconButton(onClick = onBack) {
                    Image(
                        painter = painterResource(id = R.drawable.baseline_arrow_back_24),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }

                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchText.text.isEmpty()) {
                        Text(
                            stringResource(id = R.string.excursion_search_button),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }

                // Trailing icon
                IconButton(onClick = { searchText = TextFieldValue("") }) {
                    Image(
                        painter = painterResource(id = R.drawable.close_circle_outline),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                        contentDescription = null
                    )
                }
            }
        }

        DisposableEffect(Unit) {
            focusRequester.requestFocus()
            onDispose { }
        }
    }
}