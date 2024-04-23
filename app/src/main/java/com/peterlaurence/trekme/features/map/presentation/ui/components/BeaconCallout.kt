package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.Callout

@Composable
fun BeaconCallout(
    size: DpSize,
    title: String,
    subTitle: String,
    shouldAnimate: Boolean,
    onAnimationDone: () -> Unit,
    onEditAction: () -> Unit,
    onMoveAction: () -> Unit,
    onDeleteAction: () -> Unit
) {
    var isShowingHelpDialog by remember { mutableStateOf(false) }

    Callout(
        shouldAnimate = shouldAnimate,
        onAnimationDone = onAnimationDone,
        elevation = 0.dp,
    ) {
        Column(
            Modifier.size(size),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier
                        .padding(top = 11.dp, start = 24.dp, end = 30.dp)
                        .align(Alignment.TopCenter),
                    fontWeight = FontWeight.Medium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )
                Image(
                    painter = painterResource(id = R.drawable.help_circle_outline),
                    modifier = Modifier
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape)
                        .clickable {
                            isShowingHelpDialog = true
                        }
                        .padding(0.dp)
                        .alpha(1f)
                        .size(24.dp)
                        .align(Alignment.TopEnd),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                    contentDescription = null
                )
            }

            Text(
                text = subTitle,
                modifier = Modifier.padding(vertical = 4.dp),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(thickness = 0.5.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_edit_black_24dp),
                    contentDescription = stringResource(id = R.string.map_move_landmark),
                    Modifier
                        .padding(top = 10.dp, bottom = 10.dp, start = 24.dp)
                        .size(24.dp)
                        .clickable {
                            onEditAction()
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(
                    Modifier
                        .height(16.dp)
                        .width(1.dp), thickness = 0.5.dp
                )
                Icon(
                    painterResource(id = R.drawable.cursor_move),
                    contentDescription = stringResource(id = R.string.map_move_landmark),
                    Modifier
                        .padding(top = 10.dp, bottom = 10.dp, start = 24.dp)
                        .size(24.dp)
                        .clickable {
                            onMoveAction()
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(
                    Modifier
                        .height(16.dp)
                        .width(1.dp), thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                    contentDescription = stringResource(id = R.string.map_delete_landmark),
                    Modifier
                        .padding(top = 10.dp, bottom = 10.dp, end = 24.dp)
                        .size(24.dp)
                        .clickable {
                            onDeleteAction()
                        },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (isShowingHelpDialog) {
        AlertDialog(
            text = {
                Text(text = stringResource(id = R.string.beacon_help))
            },
            onDismissRequest = { isShowingHelpDialog = false },
            confirmButton = {
                Text(
                    text = stringResource(id = R.string.ok_dialog),
                    modifier = Modifier.clickable { isShowingHelpDialog = false }
                )
            }
        )
    }
}

@Preview
@Composable
private fun BeaconCalloutPreview() {
    TrekMeTheme {
        BeaconCallout(
            size = DpSize(200.dp, 120.dp),
            title = "A beacon",
            subTitle = "Lat : -21.2059 Lon : 55.6268",
            shouldAnimate = false,
            onAnimationDone = {},
            onEditAction = {},
            onMoveAction = {},
            onDeleteAction = {}
        )
    }
}