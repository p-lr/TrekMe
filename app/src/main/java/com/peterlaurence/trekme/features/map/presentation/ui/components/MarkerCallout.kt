package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.Callout

@Composable
fun MarkerCallout(
    title: String,
    subTitle: String,
    shouldAnimate: Boolean,
    onAnimationDone: () -> Unit,
    onEditAction: () -> Unit,
    onMoveAction: () -> Unit,
    onDeleteAction: () -> Unit,
    onStartItinerary: () -> Unit
) {
    Callout(
        shouldAnimate = shouldAnimate,
        onAnimationDone = onAnimationDone,
        rightContent = {
            IconButton(
                onClick = onStartItinerary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_itinerary),
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    contentDescription = null
                )
            }
        }
    ) {
        Column(
            Modifier.width(
                (markerCalloutWidthDp - 44).dp, // 44 is the width of the right content including margin
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 11.dp, start = 8.dp, end = 8.dp),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = LocalTextStyle.current.copy(hyphens = Hyphens.Auto),
                fontSize = 14.sp
            )

            Text(
                text = subTitle,
                modifier = Modifier.padding(vertical = 4.dp),
                fontSize = 10.sp
            )
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
}

const val markerCalloutWidthDp = 244
const val markerCalloutHeightDp = 120

@Preview
@Composable
private fun MarkerCalloutPreview() {
    TrekMeTheme {
        MarkerCallout(
            title = "Les petites chutes souterraines du Plateau des Cascades",
            subTitle = "Lat : -21.2059 Lon : 55.6268",
            shouldAnimate = false,
            onAnimationDone = {},
            onEditAction = {},
            onMoveAction = {},
            onDeleteAction = {},
            onStartItinerary = {}
        )
    }
}