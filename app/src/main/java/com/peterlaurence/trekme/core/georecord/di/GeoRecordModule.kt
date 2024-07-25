package com.peterlaurence.trekme.core.georecord.di

import android.content.Context
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.georecord.data.dao.GeoRecordDaoFileBased
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordDao
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.di.IoDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        geoRecordParser: GeoRecordParser,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @ApplicationContext
        applicationContext: Context
    ): GeoRecordDao = GeoRecordDaoFileBased(
        trekMeContext,
        geoRecordParser,
        ioDispatcher,
        applicationContext.cacheDir
    )
}
