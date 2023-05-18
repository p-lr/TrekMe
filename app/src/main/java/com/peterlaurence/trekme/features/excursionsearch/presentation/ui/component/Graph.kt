package com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component

import android.view.MotionEvent
import android.view.animation.PathInterpolator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.util.NiceScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.floor

/**
 * A graph which uses cubic bezier curves if the dataset has less than 50 points, and straight lines
 * otherwise.
 * Interpolation is done accordingly.
 * [xValues] and [yValues] must be of the same size and non-empty.
 *
 * @param verticalPadding The padding in [Dp] applied to the top and the bottom of the graph
 * @param verticalSpacingY The minimum spacing in [Dp] between ticks of the vertical axis
 * @param startPadding The padding in [Dp] applied to the start of the graph. By design, there is no
 * end padding.
 * @param onCursorMove A callback invoked when the user scrolls the graph horizontally.
 */
@OptIn(ExperimentalTextApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Graph(
    modifier: Modifier = Modifier,
    xValues: List<Double>,
    yValues: List<Double>,
    verticalSpacingY: Dp,
    verticalPadding: Dp,
    startPadding: Dp = 8.dp,
    onCursorMove: (x: Double, y: Double) -> Unit = { _, _ -> }
) {
    // Safety
    if (xValues.isEmpty() || yValues.isEmpty() || xValues.size != yValues.size) return

    val density = LocalDensity.current

    val yMin = remember(yValues) { yValues.minOrNull() } ?: return
    val yMax = remember(yValues) { yValues.maxOrNull() } ?: return
    val yRange = remember(yValues) { yMin..yMax }

    val xMin = remember(xValues) { xValues.minOrNull() } ?: return
    val xMax = remember(xValues) { xValues.maxOrNull() } ?: return

    val isUsingCubic = xValues.size < 50

    val pathInterpolator = remember { PathInterpolator(0.5f, 0f, 0.5f, 1f) }
    val interpolator = { x: Double ->
        val greaterIndex = xValues.indexOfFirst { it >= x }
        val y = if (greaterIndex >= 1) {
            val left = xValues[greaterIndex - 1]
            val right = xValues[greaterIndex]
            val dy = yValues[greaterIndex] - yValues[greaterIndex - 1]
            if (isUsingCubic) {
                val t = pathInterpolator.getInterpolation(((x - left) / (right - left)).toFloat())
                t * dy + yValues[greaterIndex - 1]
            } else {
                (x - left) * dy / (right - left) + yValues[greaterIndex - 1]
            }
        } else {
            yValues[0]
        }
        onCursorMove(x, y)
    }

    var cursorX: Float? by remember { mutableStateOf(null) }

    val xToPxFlow = remember { MutableStateFlow<((Float) -> Double)?>(null) }

    LaunchedEffect(key1 = cursorX) {
        val c = cursorX ?: return@LaunchedEffect
        xToPxFlow.value?.also { pxToX ->
            val x = pxToX(c)
            interpolator(x)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    cursorX?.also { c ->
                        cursorX = c + delta
                    }
                }
            )
            .pointerInteropFilter {
                when (it.action) {
                    MotionEvent.ACTION_DOWN -> {
                        cursorX = it.x
                    }
                }
                false
            },
        contentAlignment = Alignment.Center
    ) {
        val availableDpYaxis = remember {
            maxHeight - verticalPadding
        }
        val nDivY = remember {
            floor(availableDpYaxis / verticalSpacingY).toInt()
        }

        val niceScale = remember {
            NiceScale(minPoint = yMin, maxPoint = yMax, maxTicks = nDivY)
        }

        val startPaddingPx = remember(density) {
            density.run { startPadding.toPx() }
        }

        val labelMarginEnd = remember(density) {
            density.run { 4.dp.toPx() }
        }

        val yToPx = remember(density) {
            val vPx = density.run { verticalPadding.toPx() }
            val h = density.run { maxHeight.toPx() }
            val a = (2 * vPx - h) / (yMax - yMin)
            val b = vPx - a * yMax

            return@remember { y: Double -> (a * y + b).toFloat() }
        }

        val textMeasurer = rememberTextMeasurer()
        val strokeColor = MaterialTheme.colorScheme.primary
        val labelColor = MaterialTheme.colorScheme.onPrimaryContainer

        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            /* Draw elevation ticks labels */
            var labelWidth = 0
            for (i in niceScale.niceMin.toInt()..niceScale.niceMax.toInt() step niceScale.tickSpacing.toInt()) {
                val ele = i.toDouble()
                val label = UnitFormatter.formatElevation(ele)
                val res = textMeasurer.measure(
                    label,
                    style = TextStyle(color = labelColor, fontSize = 10.sp)
                )
                if (res.size.width > labelWidth) {
                    labelWidth = res.size.width
                }
                if (ele in yRange) {
                    drawText(
                        res,
                        topLeft = Offset(startPaddingPx, yToPx(i.toDouble()) - res.size.height / 2)
                    )
                }
            }

            val startOffset = startPaddingPx + labelWidth.toFloat() + labelMarginEnd
            val a = (size.width - startOffset) / (xMax - xMin)
            val b = size.width - a * xMax
            val xToPx = { x: Double ->
                (a * x + b).toFloat()
            }
            xToPxFlow.value = { px ->
                (px / a - b / a).coerceIn(xMin, xMax)
            }

            /* Draw elevation ticks lines */
            for (i in niceScale.niceMin.toInt()..niceScale.niceMax.toInt() step niceScale.tickSpacing.toInt()) {
                val ele = i.toDouble()

                if (ele in yRange) {
                    drawLine(
                        Color.Gray,
                        start = Offset(xToPx(xMin), yToPx(i.toDouble())),
                        end = Offset(size.width, yToPx(i.toDouble()))
                    )
                }
            }

            cursorX?.also {
                val x = it.coerceAtLeast(startOffset)
                drawLine(
                    Color.Gray,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height)
                )
            }


            val path = Path().apply {
                moveTo(xToPx(xValues[0]), yToPx(yValues[0]))
                for (i in 1..xValues.lastIndex) {
                    if (isUsingCubic) {
                        val ctrlX = (xToPx(xValues[i - 1]) + xToPx(xValues[i])) / 2
                        cubicTo(
                            x1 = ctrlX,
                            y1 = yToPx(yValues[i - 1]),
                            x2 = ctrlX,
                            y2 = yToPx(yValues[i]),
                            x3 = xToPx(xValues[i]),
                            y3 = yToPx(yValues[i])
                        )
                    } else {
                        lineTo(xToPx(xValues[i]), yToPx(yValues[i]))
                    }
                }
            }

            drawPath(
                path,
                color = strokeColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }
    }

}

@Preview(widthDp = 450, heightDp = 350, showBackground = true)
@Composable
private fun GraphPreview() {
    val xValues = remember {
        listOf(0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0)
    }

    val yValues = remember {
        listOf(980.0, 1043.0, 1142.0, 1432.0, 1352.0, 1321.0, 1189.0)
    }

    TrekMeTheme {
        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Graph(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    xValues = xValues,
                    yValues = yValues,
                    verticalSpacingY = 20.dp,
                    verticalPadding = 16.dp,
                    onCursorMove = { x, y ->
                        println("cursor move x=$x y=$y")
                    }
                )
            }
        }
    }
}