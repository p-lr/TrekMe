package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.data.models.ExcursionRefFileBased
import com.peterlaurence.trekme.core.map.data.models.ExcursionRefKtx
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import java.io.File

fun Pair<ExcursionRefKtx, File>.toDomain(): ExcursionRef {
    return ExcursionRefFileBased(
        id = first.id,
        file = second,
        initialName = first.name,
        initialColor = first.color,
        initialVisibility = first.visible
    )
}

fun ExcursionRefFileBased.toData(): ExcursionRefKtx {
    return ExcursionRefKtx(id = id, name = name.value, visible = visible.value, color = color.value)
}