package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.dao.FileBasedMapRegistry
import dagger.hilt.android.scopes.ActivityRetainedScoped
import ovh.plrapps.mapcompose.core.TileStreamProvider
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

@ActivityRetainedScoped
class MapComposeTileStreamProviderInteractor @Inject constructor(
    private val fileBasedMapRegistry: FileBasedMapRegistry
) {
    fun makeTileStreamProvider(map: Map): TileStreamProvider {
        return TileStreamProvider { row, col, zoomLvl ->
            val relativePathString =
                "$zoomLvl${File.separator}$row${File.separator}$col${map.imageExtension}"

            @Suppress("BlockingMethodInNonBlockingContext")
            try {
                val directory = fileBasedMapRegistry.getRootFolder(map.id)!!
                FileInputStream(File(directory, relativePathString))
            } catch (e: Exception) {
                null
            }
        }
    }
}



