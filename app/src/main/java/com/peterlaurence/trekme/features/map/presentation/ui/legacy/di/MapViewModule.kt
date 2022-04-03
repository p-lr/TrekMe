package com.peterlaurence.trekme.features.map.presentation.ui.legacy.di

import com.peterlaurence.trekme.features.map.presentation.ui.legacy.events.TracksEventBus
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
    fun bindEventBus(): TracksEventBus = TracksEventBus()
}