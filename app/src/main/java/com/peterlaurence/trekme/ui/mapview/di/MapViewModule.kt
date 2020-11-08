package com.peterlaurence.trekme.ui.mapview.di

import com.peterlaurence.trekme.ui.mapview.events.MapViewEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object MapViewModule {
    @ActivityRetainedScoped
    @Provides
    fun bindEventBus(): MapViewEventBus = MapViewEventBus()
}