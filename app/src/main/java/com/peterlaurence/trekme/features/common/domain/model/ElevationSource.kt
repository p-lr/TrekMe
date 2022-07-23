package com.peterlaurence.trekme.features.common.domain.model

/**
 * The [sampling] indicates the rate at which elevation is subjects to change. For example, when
 * all points can potentially has different elevation values, the sampling is 1.
 */
data class ElevationSourceInfo(val elevationSource: ElevationSource, val sampling: Int)

enum class ElevationSource {
    GPS, IGN_RGE_ALTI, UNKNOWN
}