package com.peterlaurence.trekme.model.providers

import com.peterlaurence.trekme.core.providers.generic.GenericBitmapProviderAuth
import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn

/**
 * Check an IGN api, along with the associated credentials.
 * If we can download one tile, we consider that this is a pass.
 */
fun checkIgnProvider(ignApiKey: String, ignUser: String, ignPwd: String): Boolean {
    val urlTileBuilder = UrlTileBuilderIgn(ignApiKey, IgnLayers.ScanExpressStandard.realName)
    val genericProvider = GenericBitmapProviderAuth(urlTileBuilder, ignUser, ignPwd)
    return genericProvider.getBitmap(1, 1, 1) != null
}