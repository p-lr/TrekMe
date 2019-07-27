package com.peterlaurence.mapview.paths


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.ScaleChangeListener

/**
 * This is a custom view that uses, using [Canvas.drawLines] to draw a path.
 * This method is much more efficient as it's hardware accelerated, although the result is not as
 * neat as the output of [Path].
 *
 * @author peterLaurence on 19/02/17 -- Converted to Kotlin on 26/07/19
 */
class PathView(context: Context) : View(context), ScaleChangeListener {
    private val strokeWidthDefault: Float

    var scale = 1f
        set(scale) {
            field = scale
            invalidate()
        }

    private var shouldDraw = true

    private var pathList: List<DrawablePath>? = null

    private val defaultPaint = Paint()

    /**
     * The color of the default [Paint] that is assigned to a [DrawablePath] if its [DrawablePath.paint]
     * property isn't set.
     */
    var color: Int
        get() = defaultPaint.color
        set(value) {
            defaultPaint.color = value
        }

    init {
        setWillNotDraw(false)

        val metrics = resources.displayMetrics
        strokeWidthDefault = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP.toFloat(), metrics)

        defaultPaint.style = Paint.Style.STROKE
        defaultPaint.color = DEFAULT_STROKE_COLOR
        defaultPaint.strokeWidth = strokeWidthDefault
        // As of 22/06/19, the settings below cause performance drop when long Routes are rendered
        //        mDefaultPaint.setAntiAlias(true);
        //        mDefaultPaint.setStrokeJoin(Paint.Join.ROUND);
        //        mDefaultPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    fun updatePaths(pathList: List<DrawablePath>) {
        this.pathList = pathList
        invalidate()
    }

    override fun onScaleChanged(scale: Float) {
        this.scale = scale
    }

    fun setShouldDraw(shouldDraw: Boolean) {
        this.shouldDraw = shouldDraw
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        val pathList = this.pathList
        if (shouldDraw && pathList != null) {
            canvas.scale(scale, scale)
            for (path in pathList) {
                /* If no Paint is defined, take the default one */
                if (path.paint == null) {
                    path.paint = defaultPaint
                }

                /* If no width is defined, take the default one */
                val width = path.width ?: strokeWidthDefault

                if (path.visible) {
                    path.paint?.let {
                        it.strokeWidth = width / scale
                        canvas.drawLines(path.path, it)
                    }
                }
            }
        }
        super.onDraw(canvas)
    }

    interface DrawablePath {
        /**
         * Whether or not this path should be drawn
         */
        val visible: Boolean
        /**
         * The path. See [Canvas.drawLines].
         */
        var path: FloatArray
        /**
         * The paint to be used for this path.
         */
        var paint: Paint?
        /**
         * The width of the path
         */
        val width: Float?
    }
}

private const val DEFAULT_STROKE_COLOR = -0xc0ae4b
private const val DEFAULT_STROKE_WIDTH_DP = 4

/**
 * Helper function to correctly add a [PathView] to the [MapView].
 */
fun MapView.addPathView(pathView: PathView) {
    addView(pathView, 1)
    addScaleChangeListener(pathView)
}

/**
 * Helper function to correctly remove a [PathView] from the [MapView].
 */
fun MapView.removePathView(pathView: PathView) {
    removeView(pathView)
    removeScaleChangeListener(pathView)
}