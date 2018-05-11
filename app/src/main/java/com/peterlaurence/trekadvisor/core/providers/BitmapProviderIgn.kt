package com.peterlaurence.trekadvisor.core.providers

import android.content.Context
import android.graphics.Bitmap
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Credentials
import okhttp3.OkHttpClient


class BitmapProviderIgn(private val credentials: IGNCredentials, context: Context) : BitmapProvider {
    var picasso: Picasso

    init {
        val okHttpClient = OkHttpClient.Builder()
                .authenticator { route, response ->
                    val credential = Credentials.basic(credentials.user, credentials.pwd)
                    response.request().newBuilder()
                            .header("Authorization", credential)
                            .build()
                }
                .build()
        picasso = Picasso.Builder(context).downloader(OkHttp3Downloader(okHttpClient)).build()
    }

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        return try {
            val zoomLvl = tile.data as Int

            val url = "https://wxs.ign.fr/${credentials.api}/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&LAYER=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&EXCEPTIONS=text/xml&FORMAT=image/jpeg&TILEMATRIXSET=PM&TILEMATRIX=${zoomLvl}&TILEROW=${tile.row}&TILECOL=${tile.column}&"

            picasso.load(url).config(android.graphics.Bitmap.Config.RGB_565).get()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}