package com.peterlaurence.trekme.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import kotlinx.coroutines.delay

@Composable
fun OnBoardingTip(
    modifier: Modifier = Modifier,
    text: String,
    delayMs: Long = 0L,
    popupOrigin: PopupOrigin = PopupOrigin.BottomStart,
    onAcknowledge: () -> Unit
) {
    var shouldAnimate by remember { mutableStateOf(true) }

    Box(modifier) {
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
                    LightBulbAnimated()
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = text,
                        modifier = Modifier
                            .alpha(0.87f)
                            .align(alignment = Alignment.CenterVertically),
                        fontSize = 16.sp,
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

/**
 * Create a heart-beat effect, and guarantee synchrony between delayed animations.
 */
@Composable
private fun LightBulbAnimated() {
    val animationValues = (0..1).map {
        var animatedValue by remember { mutableStateOf(0f) }

        LaunchedEffect(key1 = Unit) {
            // Delaying the animation (so not using the delay api of TweenSpec, which applies a delay
            // at each animation iteration, which eventually loses synchrony with other animation).
            delay(150L * it)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                )
            ) { value, _ -> animatedValue = value }
        }
        animatedValue
    }

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(50.dp)) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            animationValues.forEach {
                drawCircle(
                    color = Color(0xff2196f3),
                    center = Offset(x = canvasWidth / 2, y = canvasHeight / 2),
                    radius = size.minDimension * it / 2,
                    alpha = 1f - it
                )
            }
        }

        Image(
            painterResource(id = R.drawable.light_bulb_simple),
            modifier = Modifier
                .size(50.dp)
                .padding(top = 12.dp),
            contentDescription = null
        )
    }
}