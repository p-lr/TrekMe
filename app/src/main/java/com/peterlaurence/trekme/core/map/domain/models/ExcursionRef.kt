package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.flow.MutableStateFlow

class ExcursionRef(
    val id: String,
    initialName: String,
    initialVisibility: Boolean = true,
    initialColor: String? = null, // In the format "#AARRGGBB"
) {
    val name: MutableStateFlow<String> = MutableStateFlow(initialName)
    val visible: MutableStateFlow<Boolean> = MutableStateFlow(initialVisibility)
    val color: MutableStateFlow<String> = MutableStateFlow(
        initialColor ?: colorRoute
    )

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