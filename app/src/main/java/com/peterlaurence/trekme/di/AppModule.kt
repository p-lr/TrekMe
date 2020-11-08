package com.peterlaurence.trekme.di

import android.app.Application
import android.content.Context
import com.peterlaurence.trekme.billing.ign.Billing
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.TrekMeContextAndroid
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.repositories.recording.GpxRecordRepository
import com.peterlaurence.trekme.viewmodel.common.LocationProvider
import com.peterlaurence.trekme.viewmodel.common.LocationSource
import com.peterlaurence.trekme.viewmodel.common.getLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Module to tell Hilt how to provide instances of types that cannot be constructor-injected.
 *
 * As these types are scoped to the application lifecycle using @Singleton, they're installed
 * in Hilt's ApplicationComponent.
 */
@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun bindTrekMeContext(): TrekMeContext = TrekMeContextAndroid()

    @Singleton
    @Provides
    fun bindLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return getLocationProvider(LocationSource.GOOGLE_FUSE, context)
    }

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
    fun bindMapRepository(): MapRepository = MapRepository()

    @Singleton
    @Provides
    fun bindGpxRecordRepository(): GpxRecordRepository = GpxRecordRepository()

    @Singleton
    @Provides
    fun bindDownloadRepository(): DownloadRepository = DownloadRepository()

    @Singleton
    @Provides
    fun bindAppEventBus(): AppEventBus = AppEventBus()
}