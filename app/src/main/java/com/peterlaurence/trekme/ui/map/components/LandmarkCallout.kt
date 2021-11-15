package com.peterlaurence.trekme.ui.map.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.common.Callout

@Composable
fun LandmarkCallout(
    subTitle: String,
    shouldAnimate: Boolean,
    onAnimationDone: () -> Unit,
    onMoveAction: () -> Unit,
    onDeleteAction: () -> Unit
) {
    Callout(
        shouldAnimate = shouldAnimate,
        onAnimationDone = onAnimationDone
    ) {
        Column(
            Modifier.size(140.dp, 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.callout_landmark_title),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(text = subTitle, fontSize = 10.sp)
            Divider()
            Row {
                Icon(
                    painterResource(id = R.drawable.cursor_move),
                    contentDescription = null,
                    Modifier
                        .padding(start = 8.dp)
                        .clickable {
                            onMoveAction()
                        },
                    tint = Color(0xFF2196F3)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painterResource(id = R.drawable.ic_delete_forever_black_24dp),
                    contentDescription = null,
                    Modifier
                        .padding(end = 8.dp)
                        .clickable {
                            onDeleteAction()
                        },
                    tint = Color(0xFF2196F3)
                )
            }
        }
    }
}
