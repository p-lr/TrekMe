package com.peterlaurence.trekme.core.georecord.di

import android.app.Application
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.data.datasource.FileBasedSourceImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.datasource.FileBasedSource
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeoRecordModule {
    @Singleton
    @Provides
    fun bindGeoRecordFileBasedSource(
        trekMeContext: TrekMeContext,
        app: Application,
        geoRecordParser: GeoRecordParser,
        appEventBus: AppEventBus,
        gpxRecordEvents: GpxRecordEvents,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): FileBasedSource = FileBasedSourceImpl(
        trekMeContext,
        app,
        geoRecordParser,
        appEventBus,
        gpxRecordEvents,
        ioDispatcher
    )
}
