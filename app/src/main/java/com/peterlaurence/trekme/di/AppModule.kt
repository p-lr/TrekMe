package com.peterlaurence.trekme.di

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.gpspro.buildGpsProBilling
import com.peterlaurence.trekme.billing.ign.buildIgnBilling
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.TrekMeContextAndroid
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.model.InternalGps
import com.peterlaurence.trekme.core.model.LocationProducerBtInfo
import com.peterlaurence.trekme.core.model.LocationProducerInfo
import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.track.TrackImporter
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.repositories.api.IgnApiRepository
import com.peterlaurence.trekme.repositories.api.OrdnanceSurveyApiRepository
import com.peterlaurence.trekme.repositories.download.DownloadRepository
import com.peterlaurence.trekme.repositories.location.LocationSourceImpl
import com.peterlaurence.trekme.repositories.location.producers.GoogleLocationProducer
import com.peterlaurence.trekme.repositories.location.producers.NmeaOverBluetoothProducer
import com.peterlaurence.trekme.repositories.map.MapRepository
import com.peterlaurence.trekme.repositories.map.RouteRepository
import com.peterlaurence.trekme.repositories.mapcreate.LayerOverlayRepository
import com.peterlaurence.trekme.repositories.mapcreate.WmtsSourceRepository
import com.peterlaurence.trekme.repositories.recording.ElevationRepository
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    @Singleton // Provide always the same instance
    @ApplicationScope
    @Provides
    fun providesCoroutineScope(): CoroutineScope {
        // Run this code when providing an instance of CoroutineScope
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    /**
     * A single instance of [Billing] is used across the app. This object isn't expensive to create.
     */
    @IGN
    @Singleton
    @Provides
    fun bindIgnBilling(application: Application): Billing {
        return buildIgnBilling(application)
    }

    /**
     * A single instance of [Billing] is used across the app. This object isn't expensive to create.
     */
    @GpsPro
    @Singleton
    @Provides
    fun bindGpsProBilling(application: Application): Billing {
        return buildGpsProBilling(application)
    }

    @Singleton
    @Provides
    fun bindRouteRepository(): RouteRepository = RouteRepository(
        Dispatchers.IO, Dispatchers.Main
    )

    @Singleton
    @Provides
    fun bindTrackImporter(repo: RouteRepository): TrackImporter = TrackImporter(repo)

    @Singleton
    @Provides
    fun bindMapRepository(): MapRepository = MapRepository()

    @Singleton
    @Provides
    fun bindIgnApiRepository(): IgnApiRepository = IgnApiRepository()

    @Singleton
    @Provides
    fun bindOrdnanceSurveyApiRepository(): OrdnanceSurveyApiRepository =
        OrdnanceSurveyApiRepository()

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
    fun bindElevationRepository(ignApiRepository: IgnApiRepository): ElevationRepository {
        return ElevationRepository(Dispatchers.Default, Dispatchers.IO, ignApiRepository)
    }

    @Singleton
    @Provides
    fun bindAppEventBus(): AppEventBus = AppEventBus()

    @Singleton
    @Provides
    fun bindMapLoader(): MapLoader =
        MapLoader(Dispatchers.Main.immediate, Dispatchers.Default, Dispatchers.IO)

    @Singleton
    @Provides
    fun bindLocationSource(
        @ApplicationContext context: Context,
        settings: Settings,
        appEventBus: AppEventBus,
        gpsProEvents: GpsProEvents
    ): LocationSource {
        val modeFlow = settings.getLocationProducerInfo()
        val flowSelector = { mode: LocationProducerInfo ->
            when (mode) {
                InternalGps -> GoogleLocationProducer(context).locationFlow
                is LocationProducerBtInfo -> {
                    val connLostMsg = context.getString(R.string.connection_bt_lost_msg)
                    val bluetoothManager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val adapter = bluetoothManager.adapter

                    NmeaOverBluetoothProducer(
                        adapter,
                        connLostMsg,
                        mode,
                        appEventBus,
                        gpsProEvents
                    ).locationFlow
                }
            }
        }
        return LocationSourceImpl(modeFlow, flowSelector)
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IGN

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class GpsPro

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class ApplicationScope
