package com.peterlaurence.trekme.core.map.di

import android.app.Application
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordParserImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.map.data.dao.*
import com.peterlaurence.trekme.core.map.domain.dao.*
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {
    @MapJson
    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @Singleton
    @Provides
    fun provideMapSaverDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): MapSaverDao {
        return MapSaverDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMarkerDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ) : MarkersDao {
        return MarkersDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideLandmarkDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ) : LandmarksDao {
        return LandmarksDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideBeaconDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ) : BeaconDao {
        return BeaconsDaoImpl(mainDispatcher, ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapLoaderDao(
        mapSaverDao: MapSaverDao,
        @MapJson json: Json
    ): MapLoaderDao {
        return MapLoaderDaoFileBased(mapSaverDao, json, Dispatchers.IO)
    }

    @Singleton
    @Provides
    fun provideMapDeleteDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : MapDeleteDao = MapDeleteDaoImpl(ioDispatcher)

    @Singleton
    @Provides
    fun provideMapRenameDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao
    ) : MapRenameDao {
        return MapRenameDaoImpl(mainDispatcher, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideMapSetThumbnailDao(
        mapSaverDao: MapSaverDao,
        app: Application
    ): MapSetThumbnailDao {
        return MapSetThumbnailDaoImpl(Dispatchers.Default, mapSaverDao, app.contentResolver)
    }

    @Singleton
    @Provides
    fun provideMapSizeComputeDao(
        @MapJson json: Json,
    ): UpdateMapSizeInBytesDao = UpdateMapSizeInBytesDaoImpl(json, Dispatchers.Default)

    @Singleton
    @Provides
    fun provideArchiveMapDao(
        app: Application
    ): ArchiveMapDao {
        return ArchiveMapDaoImpl(app, Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun providesGeoRecordParser(@IoDispatcher ioDispatcher: CoroutineDispatcher): GeoRecordParser {
        return GeoRecordParserImpl(ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideUpdateElevationFixDao(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): UpdateElevationFixDao {
        return UpdateElevationFixDaoImpl(dispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMissingTilesCountDao(
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): MissingTilesCountDao {
        return MissingTilesCountDaoImpl(dispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapSeekerDao(
        mapLoaderDao: MapLoaderDao,
        mapSaverDao: MapSaverDao,
    ): MapSeekerDao {
        return MapSeekerDaoImpl(mapLoaderDao, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideRouteDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): RouteDao {
        return RouteDaoImpl(ioDispatcher, mainDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapDownloadDao(
        settings: Settings,
    ): MapDownloadDao {
        return MapDownloadDaoImpl(settings)
    }

    @Singleton
    @Provides
    fun provideExcursionRefDao(
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): ExcursionRefDao {
        return ExcursionRefDaoImpl(ioDispatcher, json)
    }

    @Singleton
    @Provides
    fun provideCheckTileStreamProviderDao(): CheckTileStreamProviderDao {
        return CheckTileStreamProviderDaoImpl()
    }

    @Singleton
    @Provides
    fun provideTagVerifierDao(@IoDispatcher ioDispatcher: CoroutineDispatcher): MapTagDao {
        return MapTagDaoImpl(ioDispatcher)
    }
}

/**
 * To be used for serialization / deserialization of file-based maps
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MapJson