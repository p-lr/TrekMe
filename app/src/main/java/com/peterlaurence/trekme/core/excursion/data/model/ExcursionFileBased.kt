package com.peterlaurence.trekme.core.excursion.data.model

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import java.io.File

class ExcursionFileBased(
    private val root: File,
    private val _id: String,
    private val _title: String,
    private val _description: String,
    private val _type: ExcursionType
) : Excursion {
    override val id: String
        get() = _id
    override val title: String
        get() = _title
    override val type: ExcursionType
        get() = _type
    override val description: String
        get() = _description
}