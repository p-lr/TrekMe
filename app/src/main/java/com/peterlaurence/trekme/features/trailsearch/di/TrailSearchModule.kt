package com.peterlaurence.trekme.features.trailsearch.di

import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.features.trailsearch.data.api.ExcursionApiImpl
import com.peterlaurence.trekme.features.trailsearch.data.api.TrailApiImpl
import com.peterlaurence.trekme.features.trailsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.features.trailsearch.domain.model.TrailApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.OkHttpClient


@Module
@InstallIn(ActivityRetainedComponent::class)
class TrailSearchModule {
    private val httpClient = OkHttpClient()

    @Provides
    @ActivityRetainedScoped
    fun provideExcursionApi(geoRecordParser: GeoRecordParser): ExcursionApi {
        return ExcursionApiImpl(httpClient, geoRecordParser)
    }

    @Provides
    @ActivityRetainedScoped
    fun provideTrailApi(): TrailApi {
        return TrailApiImpl(httpClient)
    }
}