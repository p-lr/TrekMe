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
    val tileStreamProvider = TileStreamProviderHttpAuth(urlTileBuilder, ignUser, ignPwd)
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkUSGSProvider(): Boolean {
    val urlTileBuilder = UrlTileBuilderUSGS()
    val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkOSMProvider(): Boolean {
    val urlTileBuilder = UrlTileBuilderOSM()
    val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkIgnSpainProvider():Boolean {
    val urlTileBuilder = UrlTileBuilderIgnSpain()
    val tileStreamProvider = TileStreamProviderHttp(urlTileBuilder)
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(24, 31, 6) != null
}