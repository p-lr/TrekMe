package com.peterlaurence.trekme.di

import android.app.Application
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordDaoImpl
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordDao
import com.peterlaurence.trekme.core.map.data.dao.*
import com.peterlaurence.trekme.core.map.data.models.RuntimeTypeAdapterFactory
import com.peterlaurence.trekme.core.map.domain.dao.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.HashMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {
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

    @Singleton
    @Provides
    fun provideMapSaverDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ): MapSaverDao {
        return MapSaverDaoImpl(mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideGetMarkersForMapDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ) : MarkersDao {
        return MarkersDaoImpl(mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideGetLandmarksForMapDao(
        @MainDispatcher mainDispatcher: CoroutineDispatcher,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        gson: Gson
    ) : LandmarksDao {
        return LandmarksDaoImpl(mainDispatcher, ioDispatcher, gson)
    }

    @Singleton
    @Provides
    fun provideMapLoaderDao(
        gson: Gson,
    ): MapLoaderDao {
        return MapLoaderDaoImpl(gson, Dispatchers.Default)
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
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        mapSaverDao: MapSaverDao
    ) : MapRenameDao {
        return MapRenameDaoImpl(mainDispatcher, ioDispatcher, mapSaverDao)
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
    fun provideMapSizeComputeDao(): MapSizeComputeDao = MapSizeComputeDaoImpl(Dispatchers.Default)

    @Singleton
    @Provides
    fun provideArchiveMapDao(app: Application): ArchiveMapDao {
        return ArchiveMapDaoImpl(app, Dispatchers.Default)
    }

    @Singleton
    @Provides
    fun providesGpxDao(@IoDispatcher ioDispatcher: CoroutineDispatcher): GeoRecordDao {
        return GeoRecordDaoImpl(ioDispatcher)
    }
}