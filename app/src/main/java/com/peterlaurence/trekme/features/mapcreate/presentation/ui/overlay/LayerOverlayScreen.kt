package com.peterlaurence.trekme.features.mapcreate.presentation.ui.overlay

import android.content.Context
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.wmts.domain.model.LayerProperties
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import com.peterlaurence.trekme.core.wmts.domain.model.ignCadastre
import com.peterlaurence.trekme.core.wmts.domain.model.ignRoad
import com.peterlaurence.trekme.core.wmts.domain.model.ignSlopes
import com.peterlaurence.trekme.databinding.FragmentLayerOverlayBinding
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
        onMove = { from, to -> viewModel.moveLayer(wmtsSource, from, to) },
        onSwiped = { pos -> viewModel.removeLayer(wmtsSource, pos) },
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
    onMove: (Int, Int) -> Unit,
    onSwiped: (Int) -> Unit,
    onUpdateOpacity: (opacity: Float, layerId: String) -> Unit,
    onAddLayer: () -> Unit,
    onBack: () -> Unit
) {
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
    ) { paddingValues ->

        val context = LocalContext.current
        AndroidViewBinding(
            factory = { inflater, vg, attach ->
                FragmentLayerOverlayBinding.inflate(inflater, vg, attach).also { b ->
                    /* Init recycler view and adapter */
                    val recyclerView = b.recyclerView
                    val llm = LinearLayoutManager(context)
                    recyclerView.layoutManager = llm

                    val itemTouchHelper = makeItemTouchHelper(onMove, onSwiped)
                    itemTouchHelper.attachToRecyclerView(recyclerView)
                    val adapter = LayerOverlayAdapter(
                        itemTouchHelper,
                        onOpacityUpdate = onUpdateOpacity
                    )
                    recyclerView.adapter = adapter

                    b.addLayerFab.setOnClickListener {
                        onAddLayer()
                    }
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (layerProperties.isEmpty()) {
                header.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
            } else {
                header.visibility = View.VISIBLE
                emptyMessage.visibility = View.GONE
                val dataSet = layerProperties.mapNotNull { property ->
                    val name =
                        translateLayerName(property.layer.id, context) ?: return@mapNotNull null
                    LayerInfo(name, property)
                }
                (recyclerView.adapter as? LayerOverlayAdapter)?.setLayerInfo(dataSet)
            }
        }
    }
}

private fun makeItemTouchHelper(
    onMove: (Int, Int) -> Unit,
    onSwiped: (Int) -> Unit,
): ItemTouchHelper {
    val simpleTouchCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    ) {
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder?.itemView?.alpha = 0.5f
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.alpha = 1.0f
        }

        override fun isLongPressDragEnabled(): Boolean {
            return false
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            onMove(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            onSwiped(viewHolder.bindingAdapterPosition)
        }
    }

    return ItemTouchHelper(simpleTouchCallback)
}

private val layerIdToResId = mapOf(
    ignRoad to R.string.layer_ign_roads,
    ignSlopes to R.string.layer_ign_slopes,
    ignCadastre to R.string.layer_ign_cadastre
)

private fun translateLayerName(layerId: String, context: Context): String? {
    val res = layerIdToResId[layerId] ?: return null
    return context.getString(res)
}

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

data class LayerInfo(val name: String, val properties: LayerProperties)