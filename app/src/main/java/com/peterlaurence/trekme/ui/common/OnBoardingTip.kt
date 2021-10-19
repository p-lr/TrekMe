package com.peterlaurence.trekme.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R

@Composable
fun OnBoardingTip(
    modifier: Modifier = Modifier,
    text: String,
    delayMs: Long = 0L,
    popupOrigin: PopupOrigin = PopupOrigin.BottomStart,
    onAcknowledge: () -> Unit
) {
    var shouldAnimate by remember { mutableStateOf(true) }

    Box(modifier.width(310.dp)) {
        Callout(
            popupOrigin = popupOrigin,
            delayMs = delayMs,
            shouldAnimate = shouldAnimate,
            onAnimationDone = { shouldAnimate = false }
        ) {
            Column {
                Row(
                    Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painterResource(id = R.drawable.light_bulb),
                        modifier = Modifier.size(50.dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = text,
                        modifier = Modifier.align(alignment = Alignment.CenterVertically),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Left,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onAcknowledge,
                        modifier = Modifier.padding(top = 0.dp, end = 8.dp, bottom = 8.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = colorResource(id = R.color.colorAccent))
                    ) {
                        Text(text = stringResource(id = R.string.ok_dialog))
                    }
                }
            }
        }
    }
}