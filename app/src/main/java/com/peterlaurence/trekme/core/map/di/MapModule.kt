package com.peterlaurence.trekme.core.map.di

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordParserImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.map.data.dao.*
import com.peterlaurence.trekme.core.map.data.models.RuntimeTypeAdapterFactory
import com.peterlaurence.trekme.core.map.domain.dao.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.util.HashMap
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapModule {
    @Singleton
    @Provides
    fun provideGson(): Gson {
        val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
            init {
                put(MercatorProjection.NAME, MercatorProjection::class.java)
                put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
            }
        }
        val factory = RuntimeTypeAdapterFactory.of(
            Projection::class.java, "projection_name"
        )
        for ((key, value) in projectionHashMap) {
            factory.registerSubtype(value, key)
        }
        return GsonBuilder().serializeNulls().setPrettyPrinting().registerTypeAdapterFactory(factory)
            .create()
    }

    @MapJson
    @Singleton
    @Provides
    fun provideJson(): Json {
        return Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    @Singleton
    @Provides
    fun provideMapSaverDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ): MapSaverDao {
        return MapSaverDaoImpl(fileBasedMapRegistry, mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideGetMarkersForMapDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ) : MarkersDao {
        return MarkersDaoImpl(fileBasedMapRegistry, mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideGetLandmarksForMapDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ) : LandmarksDao {
        return LandmarksDaoImpl(fileBasedMapRegistry, mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideMapLoaderDao(
        registry: FileBasedMapRegistry,
        gson: Gson,
        @MapJson json: Json
    ): MapLoaderDao {
        return MapLoaderDaoFileBased(registry, gson, json, Dispatchers.IO)
    }

    @Singleton
    @Provides
    fun provideMapDeleteDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : MapDeleteDao = MapDeleteDaoImpl(fileBasedMapRegistry, ioDispatcher)

    @Singleton
    @Provides
    fun provideMapRenameDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao
    ) : MapRenameDao {
        return MapRenameDaoImpl(fileBasedMapRegistry, mainDispatcher, ioDispatcher, mapSaverDao)
    }

    @Singleton
    @Provides
    fun provideMapSetThumbnailDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        mapSaverDao: MapSaverDao,
        app: Application
    ): MapSetThumbnailDao {
        return MapSetThumbnailDaoImpl(fileBasedMapRegistry, Dispatchers.Default, mapSaverDao, app.contentResolver)
    }

    @Singleton
    @Provides
    fun provideMapSizeComputeDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
    ): MapSizeComputeDao = MapSizeComputeDaoImpl(fileBasedMapRegistry, Dispatchers.Default)

    @Singleton
    @Provides
    fun provideArchiveMapDao(
        fileBasedMapRegistry: FileBasedMapRegistry,
        app: Application
    ): ArchiveMapDao {
        return ArchiveMapDaoImpl(fileBasedMapRegistry, app, Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun providesGpxDao(@IoDispatcher ioDispatcher: CoroutineDispatcher): GeoRecordParser {
        return GeoRecordParserImpl(ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideMapSource(
        fileBasedMapRegistry: FileBasedMapRegistry,
        @IoDispatcher dispatcher: CoroutineDispatcher,
        @MapJson json: Json
    ): UpdateElevationFixDao {
        return UpdateElevationFixDaoImpl(fileBasedMapRegistry, dispatcher, json)
    }

    @Singleton
    @Provides
    fun provideMapSeekerDao(
        mapLoaderDao: MapLoaderDao,
        mapSaverDao: MapSaverDao,
        fileBasedMapRegistry: FileBasedMapRegistry
    ): MapSeekerDao {
        return MapSeekerDaoImpl(mapLoaderDao, mapSaverDao, fileBasedMapRegistry)
    }
}

/**
 * To be used for serialization / deserialization of file-based maps
 */
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MapJson