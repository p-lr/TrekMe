package com.peterlaurence.trekme.core.excursion.domain.dao

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import kotlinx.coroutines.flow.StateFlow

interface ExcursionDao {
    suspend fun getExcursionsFlow(): StateFlow<List<Excursion>>
}