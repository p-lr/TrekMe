package com.peterlaurence.trekme.features.maplist.presentation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.buttons.OutlinedButtonColored
import com.peterlaurence.trekme.features.common.presentation.ui.theme.accentColor
import com.peterlaurence.trekme.features.common.presentation.ui.theme.textColor
import com.peterlaurence.trekme.features.maplist.presentation.ui.screens.MapListIntents
import com.peterlaurence.trekme.features.maplist.presentation.viewmodel.MapStub
import com.peterlaurence.trekme.util.pxToDp
import java.util.UUID

@Composable
internal fun MapCard(
    modifier: Modifier = Modifier,
    mapStub: MapStub,
    intents: MapListIntents
) {
    Card(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clickable { intents.onMapClicked(mapStub.mapId) }
        ) {
            Row {
                Column(
                    Modifier
                        .padding(start = 16.dp, top = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = mapStub.title,
                        color = textColor(),
                        fontSize = 24.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ImagePlaceHolder(mapStub, intents::onSetMapImage)
            }
            ButtonRow(
                Modifier.align(Alignment.BottomStart), mapStub.isFavorite,
                onMapSettings = { intents::onMapSettings.invoke(mapStub.mapId) },
                onDelete = { intents::onMapDelete.invoke(mapStub.mapId) },
                onMapFavorite = { intents::onMapFavorite.invoke(mapStub.mapId) }
            )
        }
    }
}

@Composable
private fun ImagePlaceHolder(mapStub: MapStub, onSetMapImage: (UUID, Uri) -> Unit) {
    val addImageDialogState = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSetMapImage(mapStub.mapId, uri)
        }
    }

    Box(modifier = Modifier.padding(top = 16.dp, end = 16.dp)) {
        val image = mapStub.image
        if (image != null) {
            Image(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp)),
                bitmap = image.asImageBitmap(),
                contentDescription = null
            )
        } else {
            OutlinedButton(
                modifier = Modifier.size(pxToDp(256).dp),
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
        modifier.padding(start = 8.dp, end = 0.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = accentColor()
        TextButton(onClick = onMapSettings) {
            Text(
                text = stringResource(id = R.string.map_manage_btn_string).uppercase(),
                color = color
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isFavorite) {
            IconButton(onClick = onMapFavorite) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_star_24),
                    tint = color,
                    contentDescription = null
                )
            }
        } else {
            IconButton(onClick = onMapFavorite) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_star_border_24),
                    tint = color,
                    contentDescription = null
                )
            }
        }
        IconButton(onClick = {
            deleteDialogState.value = true
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                tint = color,
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
            confirmColorBackground = colorResource(id = R.color.colorAccentRed)
        )
    }
}




