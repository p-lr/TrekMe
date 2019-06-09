package com.peterlaurence.mapview

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import com.peterlaurence.mapview.core.TileStreamProvider
import com.peterlaurence.mapview.core.Viewport
import com.peterlaurence.mapview.core.VisibleTilesResolver
import com.peterlaurence.mapview.core.throttle
import com.peterlaurence.mapview.layout.ZoomPanLayout
import com.peterlaurence.mapview.view.TileCanvasView
import com.peterlaurence.mapview.viewmodel.TileCanvasViewModel
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * The [MapView] is a subclass of [ZoomPanLayout] specialized for displaying deepzoom maps.
 *
 * @author peterLaurence on 31/05/2019
 */
class MapView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ZoomPanLayout(context, attrs, defStyleAttr), CoroutineScope {

    private lateinit var visibleTilesResolver: VisibleTilesResolver
    private var job = Job()

    private lateinit var tileStreamProvider: TileStreamProvider
    private var tileSize: Int = 256
    private lateinit var tileCanvasView: TileCanvasView
    private lateinit var tileCanvasViewModel: TileCanvasViewModel

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    /**
     * There are two conventions when using [MapView].
     * 1. The provided [levelCount] will define the zoomLevels index that the provided
     * [tileStreamProvider] will be given for its [TileStreamProvider#zoomLevels]. The zoomLevels
     * will be [0 ; [levelCount]-1].
     *
     * 2. A map is made of levels with level p+1 being twice bigger than level p.
     * The last level will be at scale 1. So all levels have scales between 0 and 1.
     *
     * So it is assumed that the scale of level 1 is twice the scale at level 0, and so on until
     * last level [levelCount] - 1 (which has scale 1).
     *
     * @param fullWidth the width of the map in pixels at scale 1
     * @param fullHeight the height of the map in pixels at scale 1
     * @param tileSize the size of tiles (must be squares)
     * @param tileStreamProvider the tiles provider
     */
    fun configure(levelCount: Int, fullWidth: Int, fullHeight: Int, tileSize: Int, tileStreamProvider: TileStreamProvider) {
        visibleTilesResolver = VisibleTilesResolver(levelCount, fullWidth, fullHeight)
        this.tileStreamProvider = tileStreamProvider
        tileCanvasViewModel = TileCanvasViewModel(this, tileSize, tileStreamProvider)
        this.tileSize = tileSize

        initChildViews()
    }

    private fun initChildViews() {
        /* Remove the TileCanvasView if it was already added */
        if (this::tileCanvasView.isInitialized) {
            removeView(tileCanvasView)
        }
        tileCanvasView = TileCanvasView(context, tileCanvasViewModel, visibleTilesResolver)
    }

    /**
     * Stop everything.
     * [MapView] then does necessary housekeeping. After this call, the [MapView] should be removed
     * from all view trees.
     */
    fun destroy() {
        job.cancel()
    }

    private fun renderVisibleTilesThrottled() {
        throttledTask.offer(Unit)
    }

    private val throttledTask = throttle<Unit> {
        renderVisibleTiles()
    }

    private fun renderVisibleTiles() {
        val viewport = getCurrentViewport()
        val visibleTiles = visibleTilesResolver.getVisibleTiles(viewport)
        tileCanvasViewModel.setVisibleTiles(visibleTiles)
    }


    private fun getCurrentViewport(): Viewport {
        val left = scrollX
        val top = scrollY
        val right = left + width
        val bottom = top + height

        return Viewport(left, top, right, bottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        renderVisibleTilesThrottled()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        renderVisibleTilesThrottled()
    }

    override fun onScaleChanged(currentScale: Float, previousScale: Float) {
        super.onScaleChanged(currentScale, previousScale)
        TODO()
    }

    override fun onSaveInstanceState(): Parcelable? {
        job.cancel()

        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        job = Job()
    }
}

fun main(args: Array<String>) = runBlocking {
    var last: Long = 0
    val scaleChannel = throttle<Int> {
        val now = System.nanoTime() / 1000000
        println("process $it ${now - last}")
        last = now
    }

    (0..100).forEach {
        scaleChannel.send(it)
        delay(3)
    }
}
