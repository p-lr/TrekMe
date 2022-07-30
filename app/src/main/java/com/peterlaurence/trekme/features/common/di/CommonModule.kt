package com.peterlaurence.trekme.features.common.di

import android.app.Application
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.data.datasource.FileBasedSourceImpl
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.datasource.FileBasedSource
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.ImportGeoRecordInteractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommonModule {
    @Singleton
    @Provides
    fun bindImportGeoRecordInteractor(
        repo: RouteRepository,
        markersDao: MarkersDao,
        geoRecordParser: GeoRecordParser
    ): ImportGeoRecordInteractor = ImportGeoRecordInteractor(repo, markersDao, geoRecordParser)

    @Singleton
    @Provides
    fun bindGeoRecordFileBasedSource(
        trekMeContext: TrekMeContext,
        app: Application,
        geoRecordParser: GeoRecordParser,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        appEventBus: AppEventBus,
        gpxRecordEvents: GpxRecordEvents
    ): FileBasedSource = FileBasedSourceImpl(
        trekMeContext,
        app,
        geoRecordParser,
        ioDispatcher,
        appEventBus,
        gpxRecordEvents
    )
}