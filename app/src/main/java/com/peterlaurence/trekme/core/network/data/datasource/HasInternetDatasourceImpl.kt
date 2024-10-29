package com.peterlaurence.trekme.core.network.data.datasource

import com.peterlaurence.trekme.core.network.domain.model.HasInternetDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class HasInternetDatasourceImpl : HasInternetDataSource{
    override suspend fun checkInternet(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val ip = InetAddress.getByName("google.com")
            ip.hostAddress != ""
        }.isSuccess
    }
}