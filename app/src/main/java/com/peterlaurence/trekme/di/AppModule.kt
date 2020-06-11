package com.peterlaurence.trekme.di

import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.TrekMeContextAndroid
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
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
}