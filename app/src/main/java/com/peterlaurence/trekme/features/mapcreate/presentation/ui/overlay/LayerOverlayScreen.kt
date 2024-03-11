package com.peterlaurence.trekme.features.mapcreate.presentation.ui.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.Cadastre
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.core.wmts.domain.model.LayerPropertiesIgn
import com.peterlaurence.trekme.core.wmts.domain.model.Road
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.core.wmts.domain.model.ignCadastre
import com.peterlaurence.trekme.core.wmts.domain.model.ignRoad
import com.peterlaurence.trekme.core.wmts.domain.model.ignSlopes
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel.LayerOverlayViewModel

@Composable
fun LayerOverlayStateful(
    viewModel: LayerOverlayViewModel,
    wmtsSource: WmtsSource,
    onBack: () -> Unit
) {
    val layerProperties by viewModel.getLayerPropertiesFlow(wmtsSource).collectAsState()
    var isShowingLayerDialog by remember { mutableStateOf(false) }

    LayerOverlayScreen(
        layerProperties = layerProperties,
        onMoveUp = { id -> viewModel.moveLayerUp(wmtsSource, id) },
        onMoveDown = { id -> viewModel.moveLayerDown(wmtsSource, id) },
        onRemove = { id -> viewModel.removeLayer(wmtsSource, id) },
        onUpdateOpacity = { opacity, layerId ->
            viewModel.updateOpacityForLayer(
                wmtsSource,
                layerId,
                opacity
            )
        },
        onAddLayer = { isShowingLayerDialog = true },
        onBack = onBack
    )

    if (isShowingLayerDialog) {
        val ids = viewModel.getAvailableLayersId(wmtsSource)
        val idsAndNames = ids.map { id -> Pair(id, translateLayerName(id)) }

        LayerSelectDialog(
            title = stringResource(id = R.string.add_layer),
            onConfirmPressed = { id ->
                if (id != null) {
                    viewModel.addLayer(wmtsSource, id)
                }
            },
            layers = idsAndNames,
            confirmButtonText = stringResource(id = R.string.ok_dialog),
            cancelButtonText = stringResource(id = R.string.cancel_dialog_string),
            onDismissRequest = { isShowingLayerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerOverlayScreen(
    layerProperties: List<LayerProperties>,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onRemove: (String) -> Unit,
    onUpdateOpacity: (opacity: Float, layerId: String) -> Unit,
    onAddLayer: () -> Unit,
    onBack: () -> Unit
) {
    var selectedLayerId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.overlay_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                    }
                },
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                selectedLayerId?.also { id ->
                    SmallFloatingActionButton(onClick = { onRemove(id) }) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SmallFloatingActionButton(onClick = { onMoveUp(id) }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    SmallFloatingActionButton(onClick = { onMoveDown(id) }) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                FloatingActionButton(onClick = onAddLayer) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_baseline_add_24),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                        contentDescription = null,
                    )
                }
            }
        }
    ) { paddingValues ->
        if (layerProperties.isEmpty()) {
            Column(
                Modifier
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
            ) {
                Spacer(modifier = Modifier.weight(0.5f))
                Text(text = stringResource(id = R.string.overlay_empty))
                Spacer(modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                Modifier
                    .padding(paddingValues)
                    .padding(top = 8.dp)
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.overlay_title),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(id = R.string.overlay_opacity),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    )
                }

                HorizontalDivider(Modifier.fillMaxWidth())

                LazyColumn {
                    items(layerProperties, key = { it.layer.id }) {
                        var opacity by remember(it.layer.id) { mutableFloatStateOf(it.opacity) }
                        BoxWithConstraints(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (it.layer.id == selectedLayerId) MaterialTheme.colorScheme.tertiary.copy(
                                        alpha = 0.15f
                                    ) else Color.Transparent
                                )
                                .clickable { selectedLayerId = it.layer.id }
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = translateLayerName(it.layer.id) ?: "",
                                modifier = Modifier
                                    .requiredWidth(maxWidth / 2)
                                    .align(Alignment.CenterStart)
                            )

                            Slider(
                                value = opacity,
                                onValueChange = { v ->
                                    selectedLayerId = it.layer.id
                                    opacity = v
                                },
                                onValueChangeFinished = {
                                    onUpdateOpacity(opacity, it.layer.id)
                                },
                                modifier = Modifier
                                    .requiredWidth(maxWidth / 2)
                                    .padding(end = 16.dp)
                                    .align(Alignment.CenterEnd),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val layerIdToResId = mapOf(
    ignRoad to R.string.layer_ign_roads,
    ignSlopes to R.string.layer_ign_slopes,
    ignCadastre to R.string.layer_ign_cadastre
)

@Composable
private fun translateLayerName(layerId: String): String? {
    val res = layerIdToResId[layerId] ?: return null
    return stringResource(res)
}

@Composable
private fun LayerSelectDialog(
    title: String? = null,
    onConfirmPressed: (id: String?) -> Unit,
    layers: List<Pair<String, String?>>,
    confirmButtonText: String,
    cancelButtonText: String,
    confirmColorBackground: Color? = null,
    onDismissRequest: () -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = if (title != null) {
            { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
        } else null,
        text = {
            Column {
                layers.forEach { (id, name) ->
                    if (name != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                selectedId = id
                            }
                        ) {
                            RadioButton(
                                selected = id == selectedId,
                                onClick = {
                                    selectedId = id
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    onConfirmPressed(selectedId)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColorBackground ?: MaterialTheme.colorScheme.primary,
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
            ) {
                Text(cancelButtonText)
            }
        }
    )
}

@Preview(locale = "fr")
@Composable
private fun LayerOverlayScreenPreview() {
    TrekMeTheme {
        LayerOverlayScreen(
            layerProperties = listOf(
                LayerPropertiesIgn(Cadastre, 0.4f),
                LayerPropertiesIgn(Road, 0.4f)
            ),
            onAddLayer = {},
            onUpdateOpacity = { _, _ -> },
            onBack = {},
            onMoveDown = {},
            onMoveUp = {},
            onRemove = {}
        )
    }
}