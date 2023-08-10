package com.peterlaurence.trekme.core.excursion.domain.di

import android.app.Application
import androidx.core.net.toUri
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.excursion.data.dao.ExcursionDaoFileBased
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.settings.Settings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExcursionModule {
    @Singleton
    @Provides
    fun provideExcursionDao(
        trekMeContext: TrekMeContext,
        settings: Settings,
        geoRecordParser: GeoRecordParser,
        app: Application
    ): ExcursionDao {
        return ExcursionDaoFileBased(
            rootFolders = trekMeContext.rootDirListFlow,
            appDirFlow = settings.getAppDir().filterNotNull(),
            geoRecordParser = { file ->
                geoRecordParser.parse(file.toUri(), app.contentResolver)
            }
        )
    }
}