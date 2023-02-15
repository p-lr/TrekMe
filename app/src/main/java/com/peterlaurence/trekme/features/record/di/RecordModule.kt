package com.peterlaurence.trekme.features.record.di

import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.features.common.data.dao.IgnApiDao
import com.peterlaurence.trekme.features.record.data.datasource.IgnElevationDataSource
import com.peterlaurence.trekme.features.record.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.features.record.domain.model.ElevationStateOwner
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
object RecordModule {
    @ActivityRetainedScoped
    @Provides
    fun bindElevationRepository(elevationDataSource: ElevationDataSource): ElevationRepository {
        return ElevationRepository(Dispatchers.Default, Dispatchers.IO, elevationDataSource)
    }

    @ActivityRetainedScoped
    @Provides
    fun bindElevationStateOwner(elevationRepository: ElevationRepository): ElevationStateOwner {
        return elevationRepository
    }

    @ActivityRetainedScoped
    @Provides
    fun bindElevationDataSource(
        ignApiDao: IgnApiDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ElevationDataSource {
        return IgnElevationDataSource(ignApiDao, ioDispatcher)
    }
}