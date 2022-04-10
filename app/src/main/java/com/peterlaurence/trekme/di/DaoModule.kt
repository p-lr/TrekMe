package com.peterlaurence.trekme.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.peterlaurence.trekme.core.map.data.dao.LandmarksDaoImpl
import com.peterlaurence.trekme.core.map.data.models.RuntimeTypeAdapterFactory
import com.peterlaurence.trekme.core.map.data.dao.MarkersDaoImpl
import com.peterlaurence.trekme.core.map.data.dao.MapSaverDaoImpl
import com.peterlaurence.trekme.core.map.domain.dao.LandmarksDao
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
}