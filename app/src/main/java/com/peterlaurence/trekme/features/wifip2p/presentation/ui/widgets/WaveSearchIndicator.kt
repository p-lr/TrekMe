package com.peterlaurence.trekme.features.wifip2p.presentation.ui.widgets

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import kotlinx.coroutines.delay

/**
 * An indicator which animates a wave (like a sonar).
 * It nicely represents a discovery action.
 *
 * @author P.Laurence on 21/04/20
 */
class WaveSearchIndicator @JvmOverloads constructor(
    ctx: Context,
    attr: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageButton(ctx, attr, defStyleAttr) {

    private val avd: AnimatedVectorDrawable =
        ctx.getDrawable(R.drawable.avd_wave_search) as AnimatedVectorDrawable

    init {
        setImageDrawable(avd)
    }

    fun start() = if (!avd.isRunning) avd.start() else Unit
    fun stop() = if (avd.isRunning) avd.stop() else Unit
}

@Composable
fun WaveSearchIndicator(
    modifier: Modifier = Modifier,
    isBeating: Boolean,
    innerRadius: Dp = 24.dp,
    outerRadius: Dp = 128.dp,
    beatingColor: Color = MaterialTheme.colorScheme.primary,
    stoppedColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
) {
    val animationValues = (0..1).map { index ->
        var animatedValue by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(isBeating) {
            if (!isBeating) return@LaunchedEffect
            delay(180L * index)
            animate(
                initialValue = innerRadius / outerRadius,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = EaseOut),
                    repeatMode = RepeatMode.Restart,
                )
            ) { value, _ -> animatedValue = value }
        }
        animatedValue
    }

    Box(
        modifier = modifier.size(outerRadius),
        contentAlignment = Alignment.Center
    ) {
        Circle(size = innerRadius, color = if (isBeating) beatingColor else stoppedColor)
        if (isBeating) {
            animationValues.forEach {
                Wave(
                    Modifier
                        .scale(it)
                        .alpha(1f - it),
                    color = beatingColor
                )
            }
        }
    }
}

@Composable
private fun Circle(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = Color.Gray,
) {
    Box(
        modifier = modifier
            .size(size)
            .clipToBounds()
            .background(color, CircleShape)
    )
}

@Composable
private fun Wave(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
) {
    val stroke = with(LocalDensity.current) { 8.dp.toPx() }
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        drawCircle(color.copy(alpha = 0.3f), style = Stroke(stroke))
    }
}

@Preview
@Composable
private fun WaveSearchPreview() {
    TrekMeTheme {
        Column {
            AndroidView(
                factory = { context ->
                    WaveSearchIndicator(context).apply { start() }
                }
            )

            WaveSearchIndicator(isBeating = true)
        }

    }
}