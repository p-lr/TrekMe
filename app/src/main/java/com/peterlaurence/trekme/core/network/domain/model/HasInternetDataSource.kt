package com.peterlaurence.trekme.core.network.domain.model

interface HasInternetDataSource {
    suspend fun checkInternet(): Boolean
}