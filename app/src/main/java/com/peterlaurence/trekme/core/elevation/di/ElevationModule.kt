package com.peterlaurence.trekme.core.elevation.di

import com.peterlaurence.trekme.core.elevation.data.datasource.IgnElevationDataSource
import com.peterlaurence.trekme.core.elevation.domain.datasource.ElevationDataSource
import com.peterlaurence.trekme.di.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ElevationModule {
    @Singleton
    @Provides
    fun bindElevationDataSource(
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ElevationDataSource {
        return IgnElevationDataSource(ioDispatcher)
    }
}