package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.map.data.models.ExcursionRefKtx
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef

fun ExcursionRefKtx.toDomain(): ExcursionRef {
    return ExcursionRef(
        id = id,
        initialName = name,
        initialColor = color,
        initialVisibility = visible
    )
}