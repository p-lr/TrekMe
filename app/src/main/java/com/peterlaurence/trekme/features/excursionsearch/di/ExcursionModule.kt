package com.peterlaurence.trekme.features.excursionsearch.di

import com.peterlaurence.trekme.features.excursionsearch.data.api.ExcursionApiImpl
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient


@Module
@InstallIn(ViewModelComponent::class)
class ExcursionModule {
    private val httpClient = OkHttpClient()

    @Provides
    @ViewModelScoped
    fun provideExcursionApi(): ExcursionApi {
        return ExcursionApiImpl(httpClient)
    }
}