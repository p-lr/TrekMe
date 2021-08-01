package com.peterlaurence.trekme.ui.maplist.screens

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.findFragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.findNavController
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.ui.maplist.MapListFragment
import com.peterlaurence.trekme.ui.maplist.MapListFragmentDirections
import com.peterlaurence.trekme.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.viewmodel.maplist.MapListViewModel

@Composable
fun MapListUI(mapList: List<Map>, intents: MapListIntents) {
    LazyColumn(contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(mapList) { map ->
            MapCard(map, intents)
        }
    }
}

@Composable
private fun MapCard(map: Map, intents: MapListIntents) {
    Card {
        Box(Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clickable { intents.onMapClicked(map) }
        ) {
            Row {
                Column(Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .weight(1f)) {
                    Text(
                            text = map.name,
                            fontSize = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                    )
                }
                ImagePlaceHolder(map)
            }
            ButtonRow(Modifier.align(Alignment.BottomStart), map.isFavorite,
                    { intents::onMapSettings.invoke(map) },
                    { intents::onMapDelete.invoke(map.id) },
                    { intents::onMapFavorite.invoke(map) }
            )
        }

    }
}

@Composable
private fun ImagePlaceHolder(map: Map) {
    Box(modifier = Modifier) {
        if (map.image != null) {
            Image(modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .clip(RoundedCornerShape(10.dp)),
                    bitmap = map.image.asImageBitmap(),
                    contentDescription = null
            )
        }
    }
}

@Composable
private fun ButtonRow(modifier: Modifier, isFavorite: Boolean,
                      onMapSettings: () -> Unit,
                      onDelete: () -> Unit,
                      onMapFavorite: () -> Unit
) {
    val openDialog = remember { mutableStateOf(false) }

    Row(modifier.padding(start = 8.dp, end = 0.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        val color = colorResource(id = R.color.colorAccent)
        TextButton(onClick = onMapSettings) {
            Text(text = stringResource(id = R.string.map_manage_btn_string).uppercase(),
                    color = color)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isFavorite) {
            IconButton(onClick = onMapFavorite) {
                Icon(painter = painterResource(id = R.drawable.ic_baseline_star_24),
                        tint = color,
                        contentDescription = null)
            }
        } else {
            IconButton(onClick = onMapFavorite) {
                Icon(painter = painterResource(id = R.drawable.ic_baseline_star_border_24),
                        tint = color,
                        contentDescription = null)
            }
        }
        IconButton(onClick = {
            openDialog.value = true
        }) {
            Icon(painter = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                    tint = color,
                    contentDescription = null)
        }
    }

    if (openDialog.value) {
        ShowDialog(openDialog, onDelete)
    }
}

@Composable
private fun ShowDialog(openDialog: MutableState<Boolean>, onDeletePressed: () -> Unit) {
    AlertDialog(
            onDismissRequest = { openDialog.value = false },
            text = {
                Text(
                        text = stringResource(id = R.string.map_delete_question),
                        textAlign = TextAlign.Justify)
            },
            confirmButton = {
                OutlinedButton(
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(id = R.color.colorAccentRed)),
                        onClick = {
                            openDialog.value = false
                            onDeletePressed()
                        }) {
                    Text(stringResource(id = R.string.delete_dialog))
                }
            },
            dismissButton = {
                OutlinedButton(
                        onClick = {
                            openDialog.value = false
                        }) {
                    Text(stringResource(id = R.string.cancel_dialog_string))
                }
            }
    )
}

class MapListView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    @Composable
    override fun Content() {
        val viewModel: MapListViewModel = viewModel(findFragment<MapListFragment>().requireActivity())

        val intents = object : MapListIntents {
            override fun onMapClicked(map: Map) {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.mapListFragment) {
                    viewModel.setMap(map)
                    val action = MapListFragmentDirections.actionMapListFragmentToMapViewFragment()
                    navController.navigate(action)
                }
            }

            override fun onMapFavorite(map: Map) {
                viewModel.toggleFavorite(map)
            }

            override fun onMapSettings(map: Map) {
                viewModel.onMapSettings(map)

                /* Navigate to the MapSettingsFragment*/
                val action = MapListFragmentDirections.actionMapListFragmentToMapSettingsFragment(map.id)
                findNavController().navigate(action)
            }

            override fun onMapDelete(mapId: Int) {
                viewModel.deleteMap(mapId)
            }
        }

        val mapList by viewModel.mapState
        TrekMeTheme {
            MapListUI(mapList, intents)
        }
    }
}

interface MapListIntents {
    fun onMapClicked(map: Map)
    fun onMapFavorite(map: Map)
    fun onMapSettings(map: Map)
    fun onMapDelete(mapId: Int)

}