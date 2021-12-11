package com.peterlaurence.trekme.features.map.presentation.di

import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object MapFeatureModule {
    @ActivityRetainedScoped
    @Provides
    fun providesMapFeatureEvents(): MapFeatureEvents = MapFeatureEvents()
}