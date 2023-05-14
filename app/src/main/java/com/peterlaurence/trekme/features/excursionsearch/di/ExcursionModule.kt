package com.peterlaurence.trekme.features.excursionsearch.di

import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.features.excursionsearch.data.api.ExcursionApiImpl
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.OkHttpClient


@Module
@InstallIn(ActivityRetainedComponent::class)
class ExcursionModule {
    private val httpClient = OkHttpClient()

    @Provides
    @ActivityRetainedScoped
    fun provideExcursionApi(geoRecordParser: GeoRecordParser): ExcursionApi {
        return ExcursionApiImpl(httpClient, geoRecordParser)
    }
}