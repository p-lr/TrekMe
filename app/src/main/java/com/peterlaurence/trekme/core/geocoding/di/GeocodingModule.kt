package com.peterlaurence.trekme.core.geocoding.di

import android.app.Application
import com.peterlaurence.trekme.core.geocoding.data.Nominatim
import com.peterlaurence.trekme.core.geocoding.data.Photon
import com.peterlaurence.trekme.core.geocoding.domain.model.GeocodingBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
class GeocodingModule {
    @Provides
    @PhotonBackend
    fun providePhotonBackend(httpClient: OkHttpClient): GeocodingBackend {
        return Photon(httpClient)
    }

    @Provides
    @NominatimBackend
    fun provideNominatimBackend(httpClient: OkHttpClient): GeocodingBackend {
        return Nominatim(httpClient)
    }

    @Provides
    fun provideHttpClient(application: Application): OkHttpClient {
        return OkHttpClient.Builder().cache(
            Cache(
                directory = File(application.cacheDir, "geocoding_cache"),
                // $0.05 worth of phone storage in 2020
                maxSize = 50L * 1024L * 1024L // 50 MiB
            )
        ).build()
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class PhotonBackend

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class NominatimBackend