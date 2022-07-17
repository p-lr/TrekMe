package com.peterlaurence.trekme.features.record.di

import com.peterlaurence.trekme.features.record.presentation.events.RecordEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object RecordModule {
    @ActivityRetainedScoped
    @Provides
    fun bindEventBus(): RecordEventBus = RecordEventBus()
}