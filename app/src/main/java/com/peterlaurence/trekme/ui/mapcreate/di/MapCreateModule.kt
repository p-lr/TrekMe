package com.peterlaurence.trekme.ui.mapcreate.di

import com.peterlaurence.trekme.ui.mapcreate.events.MapCreateEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object MapCreateModule {
    @ActivityRetainedScoped
    @Provides
    fun bindEventBus(): MapCreateEventBus = MapCreateEventBus()
}