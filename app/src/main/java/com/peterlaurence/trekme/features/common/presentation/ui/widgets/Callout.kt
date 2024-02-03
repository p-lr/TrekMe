package com.peterlaurence.trekme.features.common.presentation.ui.widgets

import android.animation.TimeInterpolator
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A call-out which animates its entry with an overshoot scaling interpolator.
 *
 * @param shouldAnimate Controls whether there should be an entering animation or not.
 * @param delayMs Delay of the entering animation.
 * @param popupOrigin Defines from where the callout will popup (only applicable if [shouldAnimate]
 * is true).
 * @param onAnimationDone Callback invoked when entering animation is done. The parent can use this
 * to change the [shouldAnimate] state.
 */
@Composable
fun Callout(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.elevatedShape,
    shouldAnimate: Boolean = true,
    delayMs: Long = 0,
    elevation: Dp = 3.dp,
    popupOrigin: PopupOrigin = PopupOrigin.BottomCenter,
    onAnimationDone: () -> Unit = {},
    rightContent: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var animVal by remember { mutableFloatStateOf(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(true) {
        if (shouldAnimate) {
            delay(delayMs)
            Animatable(0f).animateTo(
                targetValue = 1f,
                animationSpec = tween(250, easing = overshootEasing)
            ) {
                animVal = value
            }
            onAnimationDone()
        }
    }
    Row(
        modifier = modifier
            .alpha(animVal)
            .graphicsLayer {
                scaleX = animVal
                scaleY = animVal
                transformOrigin = getTransformOrigin(popupOrigin)
            }
    ) {
        ElevatedCard(
            shape = shape,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
            content = content
        )
        rightContent()
    }
}

enum class PopupOrigin {
    TopStart, TopEnd, TopCenter, BottomStart, BottomEnd, BottomCenter
}

private fun getTransformOrigin(popupOrigin: PopupOrigin): TransformOrigin {
    return when (popupOrigin) {
        PopupOrigin.TopStart -> TransformOrigin(0f, 0f)
        PopupOrigin.TopEnd -> TransformOrigin(1f, 0f)
        PopupOrigin.TopCenter -> TransformOrigin(0.5f, 1f)
        PopupOrigin.BottomStart -> TransformOrigin(0f, 1f)
        PopupOrigin.BottomEnd -> TransformOrigin(1f, 1f)
        PopupOrigin.BottomCenter -> TransformOrigin(0.5f, 1f)
    }
}

private val overshootEasing = OvershootInterpolator(1.2f).toEasing()

private fun TimeInterpolator.toEasing() = Easing { x ->
    getInterpolation(x)
}