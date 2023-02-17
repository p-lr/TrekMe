package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Collapsed
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.Empty
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.SearchMode
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.TopBarState

/**
 * This top app bar implements [material design](https://material.io/components/app-bars-top).
 * It includes a search mode triggered when "search" button is clicked. In search mode, the
 * text input field occupies all horizontal space (other buttons are hidden). The search can be left
 * using the navigation icon.
 *
 * @since 2021/08/28
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBar(
    state: TopBarState,
    onSearchClick: () -> Unit,
    onCloseSearch: () -> Unit,
    onMenuClick: () -> Unit,
    onQueryTextSubmit: (String) -> Unit,
    onLayerSelection: () -> Unit,
    onZoomOnPosition: () -> Unit,
    onShowLayerOverlay: () -> Unit,
    onUseTrack: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
        },
        navigationIcon = if (state is SearchMode) {
            {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "")
                }
            }
        } else {
            {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.Menu, contentDescription = "")
                }
            }
        },
        actions = {
            when (state) {
                is Empty -> {
                }
                is Collapsed -> {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_search_24),
                            contentDescription = null,
                        )
                    }
                    if (state.hasPrimaryLayers) {
                        IconButton(onClick = onLayerSelection) {
                            Icon(
                                painter = painterResource(id = R.drawable.layer),
                                contentDescription = null,
                            )
                        }
                    }
                    IconButton(onClick = onZoomOnPosition) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gps_fixed_24dp),
                            contentDescription = null,
                        )
                    }
                    if (state.hasOverflowMenu) {
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

                                if (state.hasTrackImport) {
                                    DropdownMenuItem(
                                        onClick = onUseTrack,
                                        text = { Text(stringResource(id = R.string.mapcreate_from_track)) }
                                    )
                                }
                            }
                        }
                    }
                }
                is SearchMode -> {
                    SearchView(state.textValueState, onQueryTextSubmit)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(horizontal = 8.dp)
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
        colors = TextFieldDefaults.textFieldColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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