package com.peterlaurence.trekme.core.billing.di

import android.app.Application
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
import com.peterlaurence.trekme.core.billing.data.api.factories.buildGpsProBilling
import com.peterlaurence.trekme.core.billing.data.api.factories.buildIgnBilling
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.repositories.ExtendedOfferRepository
import com.peterlaurence.trekme.core.billing.domain.repositories.GpsProPurchaseRepo
import com.peterlaurence.trekme.events.AppEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    /**
     * A single instance of [BillingApi] is used across the app. This object isn't expensive to create.
     */
    @GpsPro
    @Singleton
    @Provides
    fun bindGpsProBilling(application: Application, appEventBus: AppEventBus): BillingApi {
        return buildGpsProBilling(application, appEventBus)
    }

    /**
     * A single instance of [BillingApi] is used across the app. This object isn't expensive to create.
     */
    @IGN
    @Singleton
    @Provides
    fun bindIgnBilling(
        application: Application,
        appEventBus: AppEventBus,
    ): BillingApi {
        return buildIgnBilling(application, appEventBus)
    }

    @Singleton
    @Provides
    fun providePurchaseStateOwner(repository: GpsProPurchaseRepo): GpsProStateOwner {
        return repository
    }

    @Singleton
    @Provides
    fun provideExtendedOfferStateOwner(repository: ExtendedOfferRepository): ExtendedOfferStateOwner {
        return repository
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IGN

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class GpsPro