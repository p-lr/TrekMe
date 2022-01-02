package com.peterlaurence.trekme.core.map.domain

import java.io.Serializable
import java.util.*

/**
 * The domain representation of a route.
 * TODO: this class shouldn't be serializable. Remove that inheritance after Compose route revamp.
 */
data class Route (
    var name: String? = null,
    var visible: Boolean = true,
    private val markers: MutableList<Marker> = mutableListOf(),
    var color: String? = null, // In the format "#AARRGGBB"
    var elevationTrusted: Boolean = false,
) : Serializable {
    val id: String = UUID.randomUUID().toString()

    /**
     * Keep in mind that iterating the list of markers should be done while holding the monitor
     * of this [Route] object, especially when new markers are concurrently added to this
     * route.
     */
    val routeMarkers: List<Marker> = markers

    fun addMarker(marker: Marker) {
        synchronized(this) {
            markers.add(marker)
        }
    }

    @Transient
    private val dataLock = Any()

    @Transient
    var data: Any? = null
        get() = synchronized(dataLock) {
            field
        }
        set(value) {
            synchronized(dataLock) {
                field = value
            }
        }

    fun toggleVisibility() {
        visible = !visible
    }
}
