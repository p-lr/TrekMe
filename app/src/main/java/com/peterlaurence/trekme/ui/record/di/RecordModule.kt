package com.peterlaurence.trekme.ui.record.di

import com.peterlaurence.trekme.ui.record.events.RecordEventBus
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