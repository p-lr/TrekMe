package com.peterlaurence.trekme.core.map.data.models

import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Route
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class ExcursionRefFileBased(
    override val id: String,
    val file: File,
    initialName: String,
    initialVisibility: Boolean = true,
    initialColor: String? = null, // In the format "#AARRGGBB"
) : ExcursionRef {
    override val name: MutableStateFlow<String> = MutableStateFlow(initialName)
    override val visible: MutableStateFlow<Boolean> = MutableStateFlow(initialVisibility)
    override val color: MutableStateFlow<String> = MutableStateFlow(
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