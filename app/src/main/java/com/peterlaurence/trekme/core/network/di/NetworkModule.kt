package com.peterlaurence.trekme.core.network.di

import com.peterlaurence.trekme.core.network.data.datasource.HasInternetDatasourceImpl
import com.peterlaurence.trekme.core.network.domain.model.HasInternetDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun provideHasInternetDataSource(): HasInternetDataSource = HasInternetDatasourceImpl()
}