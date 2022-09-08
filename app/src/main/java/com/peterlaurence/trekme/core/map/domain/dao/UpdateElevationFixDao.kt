package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map

interface UpdateElevationFixDao {

    /**
     * TODO: for instance, takes a [Map] as parameter. But should take an id once clean arch refactor
     * is done (a Map should no longer reference a directory).
     */
    suspend fun setElevationFix(map: Map, fix: Int): Boolean
}