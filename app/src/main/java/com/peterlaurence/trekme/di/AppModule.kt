package com.peterlaurence.trekme.di

import android.app.Application
import android.content.Context
import com.peterlaurence.trekme.billing.ign.Billing
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.TrekMeContextAndroid
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.ign.IgnApiRepository
import com.peterlaurence.trekme.repositories.location.GoogleLocationSource
import com.peterlaurence.trekme.repositories.location.LocationSource
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.repositories.recording.ElevationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Module to tell Hilt how to provide instances of types that cannot be constructor-injected.
 *
 * As these types are scoped to the application lifecycle using @Singleton, they're installed
 * in Hilt's ApplicationComponent.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun bindTrekMeContext(): TrekMeContext = TrekMeContextAndroid()

    /**
     * A single instance of [Billing] is used across the app. This object isn't expensive to create.
     */
    @Singleton
    @Provides
    fun bindBilling(application: Application): Billing {
        return Billing(application)
    }

    @Singleton
    @Provides
    fun bindTrackImporter(): TrackImporter = TrackImporter()

    @Singleton
    @Provides
    fun bindMapRepository(): MapRepository = MapRepository()

    @Singleton
    @Provides
    fun bindIgnApiRepository(): IgnApiRepository = IgnApiRepository()

    @Singleton
    @Provides
    fun bindGpxRecordEvents(): GpxRecordEvents = GpxRecordEvents()

    @Singleton
    @Provides
    fun bindDownloadRepository(): DownloadRepository = DownloadRepository()

    @Singleton
    @Provides
    fun bindElevationRepository(ignApiRepository: IgnApiRepository): ElevationRepository {
        return ElevationRepository(Dispatchers.Default, Dispatchers.IO, ignApiRepository)
    }

    @Singleton
    @Provides
    fun bindAppEventBus(): AppEventBus = AppEventBus()

    @Singleton
    @Provides
    fun bindMapLoader(): MapLoader = MapLoader(Dispatchers.Main, Dispatchers.Default, Dispatchers.IO)

    @Singleton
    @Provides
    fun bindLocationSource(@ApplicationContext context: Context): LocationSource = GoogleLocationSource(context)
}
