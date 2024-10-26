package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

/**
 * The domain representation of a route.
 */
class Route(
    id: String? = null,
    initialName: String? = null,
    initialVisibility: Boolean = true,
    initialMarkers: List<Marker> = emptyList(),
    initialColor: String? = null, // In the format "#AARRGGBB"
    val elevationTrusted: Boolean = false,
) {
    val id: String = id ?: UUID.randomUUID().toString()

    val name: MutableStateFlow<String> = MutableStateFlow(initialName ?: "")
    val visible: MutableStateFlow<Boolean> = MutableStateFlow(initialVisibility)
    val color: MutableStateFlow<String> = MutableStateFlow(initialColor ?: colorRoute)

    val routeMarkersFlow: Flow<Marker> = MutableSharedFlow<Marker>(
        replay = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.DROP_OLDEST
    ).apply {
        for (marker in initialMarkers) {
            tryEmit(marker)
        }
    }

    val routeMarkers: List<Marker>
        get() = (routeMarkersFlow as MutableSharedFlow<Marker>).replayCache

    fun addMarker(marker: Marker) {
        (routeMarkersFlow as MutableSharedFlow<Marker>).tryEmit(marker)
    }

    fun toggleVisibility() {
        visible.value = !visible.value
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Route

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

private const val colorRoute = "#3F51B5"    // default route color
