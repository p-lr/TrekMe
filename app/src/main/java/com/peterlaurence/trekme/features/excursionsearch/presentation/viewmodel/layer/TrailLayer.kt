package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.excursion.domain.model.OsmTrailGroup
import com.peterlaurence.trekme.core.excursion.domain.model.TrailDetail
import com.peterlaurence.trekme.core.excursion.domain.model.TrailSearchItem
import com.peterlaurence.trekme.core.map.domain.models.BoundingBoxNormalized
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.TrailRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.BoundingBox
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.idleStateFlow
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.onPathClickTraversal
import ovh.plrapps.mapcompose.api.removeAllPaths
import ovh.plrapps.mapcompose.api.visibleBoundingBox
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.paths.PathDataBuilder
import ovh.plrapps.mapcompose.ui.state.MapState

class TrailLayer(
    scope: CoroutineScope,
    private val mapStateFlow: Flow<MapState>,
    private val trailRepository: TrailRepository,
    onLoadingChanged: (Boolean) -> Unit,
    onPathsClicked: (List<Pair<TrailSearchItem, Color>>) -> Unit
) {
    private var trailSearchItemById = mapOf<String, TrailSearchItem>()

    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                mapState.onPathClickTraversal { ids, x, y ->
                    onPathsClicked(
                        ids.mapNotNull {
                            val id = getTrailDetailIdFromPathId(it)
                            val trailDetail = trailSearchItemById[id] ?: return@mapNotNull null
                            val color = getColorFromId(it) ?: return@mapNotNull null
                            trailDetail to color
                        }.distinctBy {
                            it.first.id
                        }
                    )
                }

                mapState.idleStateFlow().collect { idle ->
                    if (idle) {
                        val bb = mapState.visibleBoundingBox().toDomain()

                        onLoadingChanged(true)
                        val searchItems = trailRepository.search(bb)
                        trailSearchItemById = searchItems.associateBy{ it.id }

                        val details = trailRepository.getDetails(bb, searchItems.map { it.id })
                        onLoadingChanged(false)
                        updatePaths(mapState, details, searchItems)
                    }
                }
            }
        }
    }

    /**
     * Transactionally removes all paths and replaces them with the new ones.
     */
    private fun updatePaths(mapState: MapState, details: List<TrailDetail>, searchItems: List<TrailSearchItem>) {
        Snapshot.withMutableSnapshot {
            mapState.removeAllPaths()
            details.forEach { detail ->
                var lastIndex: Int? = null
                var builder: PathDataBuilder? = null
                val group = searchItems.firstOrNull { it.id == detail.id }?.group
                val prop = getProperties(group)
                detail.iteratePoints { index, x, y ->
                    if (index != lastIndex) {
                        builder?.also {
                            val pathData = it.build()
                            val idx = lastIndex
                            if (pathData != null && idx != null) {
                                val id = makeId(detail.id, prop.color, idx)
                                addPath(mapState, id = id, pathData, color = prop.color, width = prop.width, zIndex = prop.zIndex)
                            }
                        }
                        builder = mapState.makePathDataBuilder()
                        lastIndex = index
                    }
                    builder?.addPoint(x, y)
                }
                builder?.also {
                    val pathData = it.build()
                    val index = lastIndex
                    if (pathData != null && index != null) {
                        val id = makeId(detail.id, prop.color, index)
                        addPath(mapState, id = id, pathData, color = prop.color, width = prop.width, zIndex = prop.zIndex)
                    }
                }
            }
        }
    }

    private fun addPath(mapState: MapState, id: String, pathData: PathData, color: Color?, width: Dp?, zIndex: Float) {
        mapState.addPath(id, pathData, color = color, width = width, zIndex = zIndex, clickable = true)
    }

    private fun getProperties(group: OsmTrailGroup?): PathProperties {
        return when (group) {
            OsmTrailGroup.International -> PathProperties(color = Color(179, 3, 3, 205), width = 6.5.dp, zIndex = 0f)
            OsmTrailGroup.National -> PathProperties(color = Color(20, 46, 235), width = 6.5.dp, zIndex = 1f)
            OsmTrailGroup.Regional -> PathProperties(color = Color(252, 163, 5), width = 4.5.dp, zIndex = 2f)
            OsmTrailGroup.Local -> PathProperties(color = Color(140, 0, 219), width = 3.dp, zIndex = 3f)
            null -> PathProperties(null, null, 0f)
        }
    }

    private data class PathProperties(val color: Color?, val width: Dp?, val zIndex: Float, val alpha: Float? = null)

    private fun BoundingBox.toDomain(): BoundingBoxNormalized {
        return BoundingBoxNormalized(xLeft, yBottom, xRight, yTop)
    }

    private fun makeId(trailDetailId: String, color: Color?, index: Int): String {
        return "$trailDetailId-${color?.value}-$index"
    }

    private fun getColorFromId(id: String): Color? {
        return runCatching {
            val colorLong = id.split("-")[1]
            Color(colorLong.toULong())
        }.getOrNull()
    }

    private fun getTrailDetailIdFromPathId(id: String): String {
        return id.substringBefore("-", "")
    }
}