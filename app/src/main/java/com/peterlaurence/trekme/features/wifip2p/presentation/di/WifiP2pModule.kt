package com.peterlaurence.trekme.features.wifip2p.presentation.di

import com.peterlaurence.trekme.features.wifip2p.presentation.events.WifiP2pEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object WifiP2pModule {

    @ActivityRetainedScoped
    @Provides
    fun bindEventBus(): WifiP2pEventBus = WifiP2pEventBus()
}