package com.peterlaurence.trekme.core.excursion.domain.di

import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.excursion.data.dao.ExcursionDaoFileBased
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExcursionModule {
    @Singleton
    @Provides
    fun provideExcursionDao(
        trekMeContext: TrekMeContext
    ): ExcursionDao = ExcursionDaoFileBased(trekMeContext)
}