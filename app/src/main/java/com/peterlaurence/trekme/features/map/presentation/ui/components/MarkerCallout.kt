package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.common.Callout
import com.peterlaurence.trekme.ui.theme.onSurfaceAccent

@Composable
fun MarkerCallout(
    size: DpSize,
    title: String,
    subTitle: String,
    shouldAnimate: Boolean,
    onAnimationDone: () -> Unit,
    onEditAction: () -> Unit,
    onMoveAction: () -> Unit,
    onDeleteAction: () -> Unit
) {
    Callout(
        shouldAnimate = shouldAnimate,
        onAnimationDone = onAnimationDone
    ) {
        Column(
            Modifier.size(size),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = subTitle,
                modifier = Modifier.padding(vertical = 4.dp),
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Divider(thickness = 0.5.dp)
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
                    tint = onSurfaceAccent()
                )
                Spacer(modifier = Modifier.weight(1f))
                Divider(
                    Modifier
                        .height(16.dp)
                        .width(1.dp), thickness = 0.5.dp)
                Icon(
                    painterResource(id = R.drawable.cursor_move),
                    contentDescription = stringResource(id = R.string.map_move_landmark),
                    Modifier
                        .padding(top = 10.dp, bottom = 10.dp, start = 24.dp)
                        .size(24.dp)
                        .clickable {
                            onMoveAction()
                        },
                    tint = onSurfaceAccent()
                )
                Spacer(modifier = Modifier.weight(1f))
                Divider(
                    Modifier
                        .height(16.dp)
                        .width(1.dp), thickness = 0.5.dp)
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
                    tint = onSurfaceAccent()
                )
            }
        }
    }
}