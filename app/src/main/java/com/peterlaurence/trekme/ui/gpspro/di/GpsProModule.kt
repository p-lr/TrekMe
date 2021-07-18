package com.peterlaurence.trekme.ui.gpspro.di

import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.di.GpsPro
import com.peterlaurence.trekme.repositories.gpspro.GpsProPurchaseRepo
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(ActivityRetainedComponent::class)
object GpsProModule {
    @ActivityRetainedScoped
    @Provides
    fun providesRepo(@GpsPro billing: Billing): GpsProPurchaseRepo {
        return GpsProPurchaseRepo(Dispatchers.Main, billing)
    }

    @ActivityRetainedScoped
    @Provides
    fun bindGpsProEvents(): GpsProEvents = GpsProEvents()
}