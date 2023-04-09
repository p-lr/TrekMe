package com.peterlaurence.trekme.core.excursion.data.model

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionPhoto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A file-based implementation of [ExcursionPhoto].
 */
@Serializable
data class Photo(
    override val id: String,
    override val name: String,
    @SerialName("file-name")
    val fileName: String
) : ExcursionPhoto