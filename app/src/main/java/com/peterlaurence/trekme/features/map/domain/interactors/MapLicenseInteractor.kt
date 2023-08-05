package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.repositories.TrekmeExtendedRepository
import com.peterlaurence.trekme.core.billing.domain.repositories.TrekmeExtendedWithIgnRepository
import com.peterlaurence.trekme.core.map.domain.dao.MapTagDao
import com.peterlaurence.trekme.core.map.domain.models.ErrorIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.ErrorWmtsLicense
import com.peterlaurence.trekme.core.map.domain.models.FreeLicense
import com.peterlaurence.trekme.core.map.domain.models.Ign
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapLicense
import com.peterlaurence.trekme.core.map.domain.models.TileTag
import com.peterlaurence.trekme.core.map.domain.models.ValidIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.ValidWmtsLicense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class MapLicenseInteractor @Inject constructor(
    private val trekmeExtendedWithIgnRepository: TrekmeExtendedWithIgnRepository,
    private val trekmeExtendedRepository: TrekmeExtendedRepository,
    private val mapTagDao: MapTagDao
) {
    fun getMapLicenseFlow(map: Map): Flow<MapLicense> = channelFlow {
        val tag = mapTagDao.getTag(map)

        /* Take into account former maps which weren't tagged */
        val virtualTag = if (tag == null) {
            when (val origin = map.origin) {
                is Ign -> {
                    if (origin.licensed) TileTag.IGN else null
                }

                else -> null
            }
        } else tag

        when (virtualTag) {
            TileTag.IGN -> {
                /* In this case TrekMe Extended with IGN option is required */
                trekmeExtendedWithIgnRepository.updatePurchaseState()
                val purchaseState = trekmeExtendedWithIgnRepository.purchaseFlow.first()
                send(
                    if (purchaseState == PurchaseState.PURCHASED) {
                        ValidIgnLicense
                    } else {
                        ErrorIgnLicense(map)
                    }
                )
            }

            TileTag.OsmHdStandard -> {
                /* In this case TrekMe Extended with or without IGN option is required */
                trekmeExtendedRepository.updatePurchaseState()
                trekmeExtendedWithIgnRepository.updatePurchaseState()
                val x = trekmeExtendedRepository.purchaseFlow.first()
                val y = trekmeExtendedWithIgnRepository.purchaseFlow.first()

                send(
                    if (x == PurchaseState.PURCHASED || y == PurchaseState.PURCHASED) {
                        ValidWmtsLicense
                    } else {
                        ErrorWmtsLicense(map)
                    }
                )
            }

            null -> send(FreeLicense)
        }
    }
}