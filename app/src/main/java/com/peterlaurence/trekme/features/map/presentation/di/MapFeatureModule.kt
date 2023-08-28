package com.peterlaurence.trekme.features.map.presentation.di

import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapFeatureModule {
    @Singleton
    @Provides
    fun providesMapFeatureEvents(): MapFeatureEvents = MapFeatureEvents()
}