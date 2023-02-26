package com.peterlaurence.trekme.core.geocoding.di

import com.peterlaurence.trekme.core.geocoding.data.Nominatim
import com.peterlaurence.trekme.core.geocoding.data.Photon
import com.peterlaurence.trekme.core.geocoding.domain.model.GeocodingBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
class GeocodingModule {
    private val httpClient = OkHttpClient()

    @Provides
    @PhotonBackend
    fun providePhotonBackend(): GeocodingBackend {
        return Photon(httpClient)
    }

    @Provides
    @NominatimBackend
    fun provideNominatimBackend(): GeocodingBackend {
        return Nominatim(httpClient)
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class PhotonBackend

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class NominatimBackend