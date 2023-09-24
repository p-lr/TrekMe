package com.peterlaurence.trekme.features.record.di

import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.features.common.data.dao.IgnApiDao
import com.peterlaurence.trekme.features.record.data.datasource.AppRecordRestorer
import com.peterlaurence.trekme.features.record.data.datasource.IgnElevationDataSource
import com.peterlaurence.trekme.features.record.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.features.record.domain.model.ElevationStateOwner
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordStateOwner
import com.peterlaurence.trekme.features.record.domain.model.RecordRestorer
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import com.peterlaurence.trekme.features.record.domain.repositories.GpxRecordStateOwnerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Singleton
    @Provides
    fun bindElevationRepository(elevationDataSource: ElevationDataSource): ElevationRepository {
        return ElevationRepository(Dispatchers.Default, Dispatchers.IO, elevationDataSource)
    }

    @Singleton
    @Provides
    fun bindElevationStateOwner(elevationRepository: ElevationRepository): ElevationStateOwner {
        return elevationRepository
    }

    @Singleton
    @Provides
    fun bindElevationDataSource(
        ignApiDao: IgnApiDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ElevationDataSource {
        return IgnElevationDataSource(ignApiDao, ioDispatcher)
    }

    @Singleton
    @Provides
    fun bindGpxRecordStateOwner(): GpxRecordStateOwner = GpxRecordStateOwnerImpl()

    @Singleton
    @Provides
    fun bindRecordRestorer(settings: Settings): RecordRestorer = AppRecordRestorer(settings)
}