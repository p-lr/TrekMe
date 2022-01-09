package com.peterlaurence.trekme.core.map.domain

import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

/**
 * The domain representation of a route.
 */
class Route (
    var name: String? = null,
    initialVisibility: Boolean = true,
    private val markers: MutableList<Marker> = mutableListOf(),
    initialColor: String? = null, // In the format "#AARRGGBB"
    var elevationTrusted: Boolean = false,
) {
    val id: String = UUID.randomUUID().toString()

    val visible: MutableStateFlow<Boolean> = MutableStateFlow(initialVisibility)
    val color: MutableStateFlow<String?> = MutableStateFlow(initialColor)

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
