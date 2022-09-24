package com.peterlaurence.trekme.features.mapimport.di

import android.app.Application
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.features.mapimport.data.dao.MapArchiveRegistryImpl
import com.peterlaurence.trekme.features.mapimport.data.dao.UnarchiveDaoImpl
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveRegistry
import com.peterlaurence.trekme.features.mapimport.domain.dao.UnarchiveDao
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchiveStateOwner
import com.peterlaurence.trekme.features.mapimport.domain.repository.MapArchiveRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapImportModule {
    @Provides
    @Singleton
    fun providesMapArchiveStateOwner(repository: MapArchiveRepository): MapArchiveStateOwner {
        return repository
    }

    @Provides
    @Singleton
    fun provideMapArchiveRegistry() : MapArchiveRegistry {
        return MapArchiveRegistryImpl()
    }

    @Provides
    @Singleton
    fun provideUnarchiveDao(
        app: Application,
        @IoDispatcher
        ioDispatcher: CoroutineDispatcher,
        settings: Settings,
        mapArchiveRegistry: MapArchiveRegistry,
        mapRepository: MapRepository,
        mapSeekerDao: MapSeekerDao
    ): UnarchiveDao {
        return UnarchiveDaoImpl(app, ioDispatcher, settings, mapArchiveRegistry, mapRepository, mapSeekerDao)
    }

}