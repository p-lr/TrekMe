package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.maplist.presentation.model.MapItem
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.MapListIntents
import com.peterlaurence.trekme.util.pxToDp
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

@Composable
internal fun MapCard(
    modifier: Modifier = Modifier,
    mapItem: MapItem,
    intents: MapListIntents
) {
    val imageSize = pxToDp(256).coerceAtMost(110).dp
    ElevatedCard(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp + imageSize)
                .clickable { intents.onMapClicked(mapItem.mapId) }
        ) {
            val title by mapItem.titleFlow.collectAsStateWithLifecycle()
            val isDownloadPending by mapItem.isDownloadPending.collectAsStateWithLifecycle()

            var isShowingDownloadPendingExplanation by remember { mutableStateOf(false) }

            Row {
                Column(
                    Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDownloadPending) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            Modifier.clickable {
                                isShowingDownloadPendingExplanation = true
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.warning),
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = null
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(id = R.string.download_aborted_warning),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                ImagePlaceHolder(mapItem, imageSize, intents::onSetMapImage)
            }
            ButtonRow(
                Modifier.align(Alignment.BottomStart), mapItem.isFavorite,
                onMapSettings = { intents::onMapSettings.invoke(mapItem.mapId) },
                onDelete = { intents::onMapDelete.invoke(mapItem.mapId) },
                onMapFavorite = { intents::onMapFavorite.invoke(mapItem.mapId) }
            )

            if (isShowingDownloadPendingExplanation) {
                val content = stringResource(id = R.string.download_aborted_explanation).format(
                    stringResource(id = R.string.map_analyze_and_repair)
                )
                AlertDialog(
                    text = {
                        Text(content, fontSize = 16.sp)
                    },
                    onDismissRequest = { isShowingDownloadPendingExplanation = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isShowingDownloadPendingExplanation = false
                            }
                        ) {
                            Text(stringResource(id = R.string.ok_dialog))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ImagePlaceHolder(mapItem: MapItem, imageSize: Dp, onSetMapImage: (UUID, Uri) -> Unit) {
    val addImageDialogState = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSetMapImage(mapItem.mapId, uri)
        }
    }

    Box(modifier = Modifier.padding(top = 16.dp, end = 16.dp)) {
        val imageState = mapItem.image.collectAsStateWithLifecycle()
        val image = imageState.value
        if (image != null) {
            Image(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp)),
                bitmap = image.asImageBitmap(),
                contentDescription = null
            )
        } else {
            OutlinedButton(
                modifier = Modifier.size(imageSize),
                onClick = { addImageDialogState.value = true },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .alpha(0.5f),
                        painter = painterResource(id = R.drawable.ancient_map_squared),
                        contentDescription = null
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.add_circle),
                        modifier = Modifier.size(50.dp),
                        tint = if (isSystemInDarkTheme()) {
                            Color(0x55ffffff)
                        } else {
                            Color(0x55000000)
                        },
                        contentDescription = null
                    )
                }

            }
        }
    }

    if (addImageDialogState.value) {
        ConfirmDialog(
            addImageDialogState,
            onConfirmPressed = { launcher.launch("image/*") },
            contentText = stringResource(id = R.string.choose_map_thumbnail),
            confirmButtonText = stringResource(id = R.string.ok_dialog),
            cancelButtonText = stringResource(id = R.string.cancel_dialog_string)
        )
    }
}

@Composable
private fun ButtonRow(
    modifier: Modifier, isFavorite: Boolean,
    onMapSettings: () -> Unit,
    onDelete: () -> Unit,
    onMapFavorite: () -> Unit
) {
    val deleteDialogState = remember { mutableStateOf(false) }

    Row(
        modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onMapSettings) {
            Text(
                text = stringResource(id = R.string.map_manage_btn_string).uppercase(),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isFavorite) {
            IconButton(onClick = onMapFavorite) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_star_24),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }
        } else {
            IconButton(onClick = onMapFavorite) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_star_border_24),
                    contentDescription = null
                )
            }
        }
        IconButton(onClick = {
            deleteDialogState.value = true
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_outline),
                contentDescription = null
            )
        }
    }

    if (deleteDialogState.value) {
        ConfirmDialog(
            deleteDialogState,
            onConfirmPressed = onDelete,
            contentText = stringResource(id = R.string.map_delete_question),
            confirmButtonText = stringResource(id = R.string.delete_dialog),
            cancelButtonText = stringResource(id = R.string.cancel_dialog_string),
            confirmColorBackground = MaterialTheme.colorScheme.error
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES, showBackground = true)
@Preview(showBackground = true)
@Composable
private fun MapCardPreview() {
    val intents = object : MapListIntents {
        override fun onMapClicked(mapId: UUID) {
        }

        override fun onMapFavorite(mapId: UUID) {
        }

        override fun onMapSettings(mapId: UUID) {
        }

        override fun onSetMapImage(mapId: UUID, uri: Uri) {
        }

        override fun onMapDelete(mapId: UUID) {
        }

        override fun navigateToMapCreate(showOnBoarding: Boolean) {
        }

        override fun navigateToExcursionSearch() {
        }

        override fun onCancelDownload() {
        }
    }
    TrekMeTheme {
        MapCard(
            Modifier.padding(16.dp),
            mapItem = MapItem(
                UUID.randomUUID(),
                titleFlow = MutableStateFlow("Terra Incognita with a long name"),
                isDownloadPending = MutableStateFlow(true),
                image = MutableStateFlow(null)
            ),
            intents = intents
        )
    }
}




