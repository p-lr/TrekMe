package com.peterlaurence.trekme.core.map.domain

import java.io.Serializable

data class Route (
    val id: String? = null,
    var name: String? = null,
    var visible: Boolean = true,
    val routeMarkers: MutableList<Marker> = mutableListOf(),
    var color: String? = null,
    var elevationTrusted: Boolean = false,
) : Serializable {
    val compositeId: String
        get() = id ?: name + routeMarkers.size

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
