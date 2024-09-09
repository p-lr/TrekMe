package com.peterlaurence.trekme.features.common.di

import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.features.common.domain.interactors.ImportGeoRecordInteractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
}