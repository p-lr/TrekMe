package com.peterlaurence.trekme.features.excursionsearch.domain.repository

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.excursionsearch.domain.model.ExcursionApi
import com.peterlaurence.trekme.util.ResultL
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import javax.inject.Inject

@ActivityRetainedScoped
class ExcursionGeoRecordRepository @Inject constructor(
    private val api: ExcursionApi,
) {
    private val lastItemFlow = MutableStateFlow<ExcursionSearchItem?>(null)

    fun postItem(item: ExcursionSearchItem) {
        lastItemFlow.value = item
    }

    fun getGeoRecordFlow(): Flow<ResultL<GeoRecord>> = channelFlow {
        lastItemFlow.collect { item ->
            if (item != null) {
                send(ResultL.loading())
                val geoRecord = api.getGeoRecord(item.id, item.title)
                if (geoRecord != null) {
                    send(ResultL.success(geoRecord))
                } else {
                    send(ResultL.failure(Exception("Error while parsing georecord for ${item.id}")))
                }
            } else {
                send(ResultL.loading())
            }
        }
    }
}