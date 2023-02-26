package com.peterlaurence.trekme.features.excursionsearch.presentation.model

import android.os.Parcelable
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import kotlinx.parcelize.Parcelize


sealed interface ExcursionCategoryChoice : Parcelable {
    @Parcelize
    object All : ExcursionCategoryChoice

    @Parcelize
    data class Single(val choice: ExcursionCategory) : ExcursionCategoryChoice
}

