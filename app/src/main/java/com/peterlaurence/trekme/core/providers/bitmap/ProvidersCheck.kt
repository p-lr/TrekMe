package com.peterlaurence.trekme.core.providers.bitmap

import com.peterlaurence.trekme.core.map.TileStreamProvider

/**
 * Check an IGN api, along with the associated credentials.
 * If we can download one tile, we consider that this is a pass.
 */
fun checkIgnProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkUSGSProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkOSMProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(1, 1, 1) != null
}

fun checkIgnSpainProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(24, 31, 6) != null
}

fun checkSwissTopoProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(180, 266, 9) != null
}

fun checkOrdnanceSurveyProvider(tileStreamProvider: TileStreamProvider): Boolean {
    val bitmapProvider = BitmapProvider(tileStreamProvider)
    return bitmapProvider.getBitmap(40, 61, 7) != null
}