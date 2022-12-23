package com.peterlaurence.trekme.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.TrekMeContextAndroid
import com.peterlaurence.trekme.core.location.*
import com.peterlaurence.trekme.core.location.domain.*
import com.peterlaurence.trekme.core.location.domain.model.*
import com.peterlaurence.trekme.core.orientation.model.OrientationSource
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.core.location.domain.repository.LocationSourceImpl
import com.peterlaurence.trekme.core.location.app.producer.GoogleLocationProducer
import com.peterlaurence.trekme.core.location.app.producer.NmeaOverBluetoothProducer
import com.peterlaurence.trekme.events.maparchive.MapArchiveEvents
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.mapcreate.domain.repository.LayerOverlayRepository
import com.peterlaurence.trekme.features.mapcreate.domain.repository.WmtsSourceRepository
import com.peterlaurence.trekme.features.common.domain.repositories.OnBoardingRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.orientation.app.OrientationSourceImpl
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.data.dao.IgnApiDao
import com.peterlaurence.trekme.features.common.data.dao.OrdnanceSurveyApiDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Qualifier
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

    @Singleton
    @IoDispatcher
    @Provides
    fun bindIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Singleton
    @MainDispatcher
    @Provides
    fun bindMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Singleton
    @DefaultDispatcher
    @Provides
    fun bindDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Singleton // Provide always the same instance
    @ApplicationScope // Must also be injectable at application scope
    @Provides
    fun providesCoroutineScope(): CoroutineScope {
        // Run this code when providing an instance of CoroutineScope
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Singleton
    @Provides
    fun bindMapRepository(): MapRepository = MapRepository()

    @Singleton
    @Provides
    fun provideMapArchiveEvents(): MapArchiveEvents = MapArchiveEvents()

    @Singleton
    @Provides
    fun bindOnBoardingRepository(): OnBoardingRepository = OnBoardingRepository()

    @Singleton
    @Provides
    fun bindIgnApiRepository(): IgnApiDao = IgnApiDao()

    @Singleton
    @Provides
    fun bindOrdnanceSurveyApiRepository(): OrdnanceSurveyApiDao = OrdnanceSurveyApiDao()

    @Singleton
    @Provides
    fun bindWmtsSourceRepository(): WmtsSourceRepository = WmtsSourceRepository()

    @Singleton
    @Provides
    fun bindGpxRecordEvents(): GpxRecordEvents = GpxRecordEvents()

    @Singleton
    @Provides
    fun bindGpsProEvents(): GpsProEvents = GpsProEvents()

    @Singleton
    @Provides
    fun bindDownloadRepository(): DownloadRepository = DownloadRepository()

    @Singleton
    @Provides
    fun bindLayerOverlayRepository(): LayerOverlayRepository = LayerOverlayRepository()

    @Singleton
    @Provides
    fun bindAppEventBus(): AppEventBus = AppEventBus()

    @Singleton
    @Provides
    fun bindLocationSource(
        @ApplicationContext context: Context,
        settings: Settings,
        appEventBus: AppEventBus,
        gpsProEvents: GpsProEvents
    ): LocationSource {
        val modeFlow = settings.getLocationProducerInfo()
        val flowSelector: (LocationProducerInfo) -> Flow<Location> = { mode: LocationProducerInfo ->
            when (mode) {
                InternalGps -> GoogleLocationProducer(context).locationFlow
                is LocationProducerBtInfo -> {
                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val adapter = bluetoothManager?.adapter

                    if (adapter != null) {
                        val connLostMsg = context.getString(R.string.connection_bt_lost_msg)
                        NmeaOverBluetoothProducer(
                            adapter,
                            connLostMsg,
                            mode,
                            appEventBus,
                            gpsProEvents
                        ).locationFlow
                    } else flow {
                        /* Empty flow */
                    }
                }
            }
        }
        return LocationSourceImpl(modeFlow, flowSelector)
    }

    @Singleton
    @Provides
    fun bindOrientationSource(
        @ApplicationContext context: Context
    ): OrientationSource = OrientationSourceImpl(context)
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
