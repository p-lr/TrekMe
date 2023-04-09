package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionRef
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.models.Map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * For instance, excursions are imported into [Map]s using classic routes. However, this is only
 * done in-memory. Eventually, new data structures specific to excursions will be introduced, to
 * get rid of the current limitations (a route == track segment).
 */
class MapExcursionInteractor @Inject constructor(
    excursionRepository: ExcursionRepository,
    importGeoRecordInteractor: ImportGeoRecordInteractor
) {
    /**
     * TODO: for each excursion, get a GeoRecord and use the importGeoRecordInteractor to import
     * them into the map. This import phase should be done in the RouteLayer, at the same time we
     * import routes.
     * 1. Implement the [ExcursionRepository]. Excursions are expected to be found in "excursions"
     * directory right next to "recordings".
     *
     * 2.
     **/
    fun importExcursions(map: Map) {

    }

    /**
     * Save the color in the "#AARRGGBB" format.
     */
    suspend fun setColor(map: Map, ref: ExcursionRef, color: Long) {
        ref.color.value = '#' + java.lang.Long.toHexString(color)
        // TODO: save changes
    }

    suspend fun rename(map: Map, ref: ExcursionRef, newName: String) {
        ref.name.value = newName
        // TODO: save changes
    }

    suspend fun removeExcursionOnMap(map: Map, ref: ExcursionRef) {
        map.excursionRefs.update {
            it.filter { r -> r.id != ref.id }
        }
        // TODO: save on disk
    }

    suspend fun toggleVisibility(map: Map, ref: ExcursionRef) {
        ref.visible.update { !it }
        // TODO: save on disk
    }
}