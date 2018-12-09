package com.peterlaurence.trekme.core.providers.bitmap

import com.peterlaurence.trekme.core.providers.layers.IgnLayers
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgn
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderIgnSpain
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderOSM
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilderUSGS

/**
 * Check an IGN api, along with the associated credentials.
 * If we can download one tile, we consider that this is a pass.
 */
fun checkIgnProvider(ignApiKey: String, ignUser: String, ignPwd: String): Boolean {
    val urlTileBuilder = UrlTileBuilderIgn(ignApiKey, IgnLayers.ScanExpressStandard.realName)
    val genericProvider = GenericBitmapProvider.getBitmapProviderIgn(urlTileBuilder, ignUser, ignPwd)
    return genericProvider.getBitmap(1, 1, 1) != null
}

fun checkUSGSProvider(): Boolean {
    val genericProvider = GenericBitmapProvider.getBitmapProviderUSGS(UrlTileBuilderUSGS())
    return genericProvider.getBitmap(1, 1, 1) != null
}

fun checkOSMProvider(): Boolean {
    val genericProvider = GenericBitmapProvider.getBitmapProviderOSM(UrlTileBuilderOSM())
    return genericProvider.getBitmap(1, 1, 1) != null
}

fun checkIgnSpainProvider():Boolean {
    val genericProvider = GenericBitmapProvider.getBitmapProviderIgnSpain(UrlTileBuilderIgnSpain())
    return genericProvider.getBitmap(6, 24, 31) != null
}