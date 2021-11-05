package com.peterlaurence.trekme.ui.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.peterlaurence.trekme.R
import kotlinx.coroutines.delay

@Composable
fun OnBoardingTip(
    modifier: Modifier = Modifier,
    text: String,
    delayMs: Long = 0L,
    popupOrigin: PopupOrigin = PopupOrigin.BottomStart,
    onAcknowledge: () -> Unit,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    var shouldAnimate by remember { mutableStateOf(true) }

    Box(modifier) {
        Callout(
            shape = shape,
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

/**
 * A custom [Shape] with a nub, to indicate the origination of the dialog (like in comics dialog
 * bubble).
 * It's customizable. [relativePosition] defines the relative position of the nub. In the example
 * below, [nubPosition] is BOTTOM, and [relativePosition] is roughly 0.5f.
 *
 *    0f                       1f
 * 0f ---------------------------
 *    |                         |
 *    |                         |
 * 1f ---------------------------
 *               \/
 *
 * @param relativePosition Must be between 0f and 1f
 * @param offset the offset in pixels of the nub's tip, defaults to 0f
 */
class DialogShape(
    private val cornerRadius: Float = 10f,
    private val nubPosition: NubPosition, private val relativePosition: Float,
    private val nubWidth: Float = 40f, private val nubHeight: Float = 50f,
    private val offset: Float = 0f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = Path().apply {
                addRoundRect(
                    RoundRect(0f, 0f, size.width, size.height, CornerRadius(cornerRadius))
                )
                op(this, createNubPath(size), operation = PathOperation.Xor)
            }
        )
    }

    private fun createNubPath(size: Size): Path {
        return runCatching {
            Path().apply {
                when (nubPosition) {
                    NubPosition.LEFT -> {
                        val startY = (size.height * relativePosition).coerceIn(
                            cornerRadius,
                            size.height - cornerRadius - nubWidth
                        )
                        val endY =
                            (startY + nubWidth).coerceIn(cornerRadius, size.height - cornerRadius)
                        moveTo(0f, startY)
                        lineTo(0f, endY)
                        lineTo(-nubHeight, startY + (endY - startY) / 2f + offset)
                    }
                    NubPosition.TOP -> {
                        val startX = (size.width * relativePosition).coerceIn(
                            cornerRadius,
                            size.width - cornerRadius - nubWidth
                        )
                        val endX =
                            (startX + nubWidth).coerceIn(cornerRadius, size.width - cornerRadius)
                        moveTo(startX, 0f)
                        lineTo(endX, 0f)
                        lineTo(startX + (endX - startX) / 2f + offset, -nubHeight)
                    }
                    NubPosition.RIGHT -> {
                        val startY = (size.height * relativePosition).coerceIn(
                            cornerRadius,
                            size.height - cornerRadius - nubWidth
                        )
                        val endY =
                            (startY + nubWidth).coerceIn(cornerRadius, size.height - cornerRadius)
                        moveTo(size.width, startY)
                        lineTo(size.width, endY)
                        lineTo(size.width + nubHeight, startY + (endY - startY) / 2f + offset)
                    }
                    NubPosition.BOTTOM -> {
                        val startX = (size.width * relativePosition).coerceIn(
                            cornerRadius,
                            size.width - cornerRadius - nubWidth
                        )
                        val endX =
                            (startX + nubWidth).coerceIn(cornerRadius, size.width - cornerRadius)
                        moveTo(startX, size.height)
                        lineTo(endX, size.height)
                        lineTo(startX + (endX - startX) / 2f + offset, size.height + nubHeight)
                    }
                }
                close()
            }
        }.getOrElse {
            // coerceIn(..) calls might throw IllegalArgumentException when composition happens when
            // the size is (0, 0). In this case, we don't care about the returned Path.
            Path()
        }
    }

    enum class NubPosition {
        LEFT, TOP, RIGHT, BOTTOM
    }
}