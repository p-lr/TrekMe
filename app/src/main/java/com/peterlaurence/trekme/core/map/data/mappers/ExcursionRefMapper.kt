package com.peterlaurence.trekme.core.map.data.mappers

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.map.data.models.ExcursionRefFileBased
import com.peterlaurence.trekme.core.map.data.models.ExcursionRefKtx
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import java.io.File

fun makeDomainExcursionRef(refKtx: ExcursionRefKtx, file: File, excursion: Excursion): ExcursionRef {
    return ExcursionRefFileBased(
        id = refKtx.id,
        file = file,
        nameState = excursion.title,
        initialColor = refKtx.color,
        initialVisibility = refKtx.visible
    )
}

fun ExcursionRefFileBased.toData(): ExcursionRefKtx {
    return ExcursionRefKtx(id = id, visible = visible.value, color = color.value)
}