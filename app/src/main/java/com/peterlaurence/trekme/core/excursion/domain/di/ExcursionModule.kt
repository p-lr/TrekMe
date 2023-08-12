package com.peterlaurence.trekme.core.excursion.domain.di

import android.app.Application
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.excursion.data.dao.ExcursionDaoFileBased
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.readUri
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
        app: Application
    ): ExcursionDao {
        return ExcursionDaoFileBased(
            rootFolders = trekMeContext.rootDirListFlow,
            appDirFlow = settings.getAppDir().filterNotNull(),
            uriReader = { uri, reader ->
                readUri(uri, app.contentResolver, reader)
            },
            nameReaderUri = { uri ->
                FileUtils.getFileRealFileNameFromURI(app.contentResolver, uri)
            }
        )
    }
}