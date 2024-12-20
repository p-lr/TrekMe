package com.peterlaurence.trekme.features.record.di

import com.peterlaurence.trekme.core.elevation.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.core.network.domain.model.HasInternetDataSource
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.common.domain.model.RecordingDataStateOwner
import com.peterlaurence.trekme.features.record.data.datasource.AppRecordRestorer
import com.peterlaurence.trekme.features.record.domain.model.ElevationStateOwner
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordStateOwner
import com.peterlaurence.trekme.features.record.domain.model.RecordRestorer
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import com.peterlaurence.trekme.features.record.domain.repositories.GpxRecordStateOwnerImpl
import com.peterlaurence.trekme.features.record.domain.repositories.RecordingDataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecordModule {
    @Singleton
    @Provides
    fun bindElevationRepository(
        hasInternetDataSource: HasInternetDataSource,
        elevationDataSource: ElevationDataSource,
        @ApplicationScope processScope: CoroutineScope
    ): ElevationRepository {
        return ElevationRepository(
            dispatcher = Dispatchers.Default,
            ioDispatcher = Dispatchers.IO,
            hasInternetDataSource = hasInternetDataSource,
            elevationDataSource = elevationDataSource,
            processScope = processScope
        )
    }

    @Singleton
    @Provides
    fun bindElevationStateOwner(elevationRepository: ElevationRepository): ElevationStateOwner {
        return elevationRepository
    }

    @Singleton
    @Provides
    fun bindGpxRecordStateOwner(): GpxRecordStateOwner = GpxRecordStateOwnerImpl()

    @Singleton
    @Provides
    fun bindRecordRestorer(settings: Settings): RecordRestorer = AppRecordRestorer(settings)

    @Singleton
    @Provides
    fun bindRecordingDataStateOwner(repository: RecordingDataRepository): RecordingDataStateOwner {
        return repository
    }
}