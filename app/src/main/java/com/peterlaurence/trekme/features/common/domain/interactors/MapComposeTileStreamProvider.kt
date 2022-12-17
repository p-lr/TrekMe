package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.models.Map
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ovh.plrapps.mapcompose.core.TileStreamProvider
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

@ActivityRetainedScoped
class MapComposeTileStreamProviderInteractor @Inject constructor(
) {
    // TODO: this interactor should delegate to a DAO, as knowing about a specific Map implementation
    // shouldn't be done in domain layer.
    fun makeTileStreamProvider(map: Map): TileStreamProvider {
        return TileStreamProvider { row, col, zoomLvl ->
            val relativePathString =
                "$zoomLvl${File.separator}$row${File.separator}$col${map.imageExtension}"

            @Suppress("BlockingMethodInNonBlockingContext")
            try {
                val directory = (map as MapFileBased).folder
                FileInputStream(File(directory, relativePathString))
            } catch (e: Exception) {
                null
            }
        }
    }
}



