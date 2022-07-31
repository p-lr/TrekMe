package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import javax.inject.Inject

class GetMapInteractor @Inject constructor(
    private val mapRepository: MapRepository
) {
    fun getMap(id: Int): Map? = mapRepository.getMap(id)

    fun getMapList(): List<Map> = mapRepository.getCurrentMapList()
}