package com.peterlaurence.trekme.features.record.di

import com.peterlaurence.trekme.core.repositories.api.IgnApiRepository
import com.peterlaurence.trekme.features.record.domain.repositories.ElevationRepository
import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
object RecordModule {
    @ActivityRetainedScoped
    @Provides
    fun bindEventBus(): RecordEventBus = RecordEventBus()

    @ActivityRetainedScoped
    @Provides
    fun bindElevationRepository(ignApiRepository: IgnApiRepository): ElevationRepository {
        return ElevationRepository(Dispatchers.Default, Dispatchers.IO, ignApiRepository)
    }
}