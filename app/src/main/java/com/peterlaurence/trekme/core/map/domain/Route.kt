package com.peterlaurence.trekme.core.map.domain

import java.io.Serializable

data class Route (
    val id: String? = null,
    var name: String? = null,
    var visible: Boolean = true,
    private val markers: MutableList<Marker> = mutableListOf(),
    var color: String? = null,
    var elevationTrusted: Boolean = false,
) : Serializable {
    val compositeId: String
        get() = id ?: name + routeMarkers.size

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
