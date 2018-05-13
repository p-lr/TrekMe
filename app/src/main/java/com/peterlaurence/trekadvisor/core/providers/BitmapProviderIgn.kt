package com.peterlaurence.trekadvisor.core.providers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import com.qozix.tileview.graphics.BitmapProvider
import com.qozix.tileview.tiles.Tile
import java.io.BufferedInputStream
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URL


/**
 * Luckily, IGN's [WMTS service](https://geoservices.ign.fr/documentation/geoservices/wmts.html) has
 * a grid coordinates that is exactly the same as the one [TileView] uses. <br>
 * Consequently, to make a valid HTTP request, we just have to format the URL with raw zoom-level,
 * row and col numbers. <br>
 * Additional information have to be provided though, like IGN credentials.
 */
class BitmapProviderIgn(private val credentials: IGNCredentials, context: Context) : BitmapProvider {
    private var bitmapLoadingOptions = BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(credentials.user, credentials.pwd?.toCharArray())
            }
        })
    }

    override fun getBitmap(tile: Tile, p1: Context?): Bitmap? {
        return try {
            val zoomLvl = tile.data as Int

            val src = "https://wxs.ign.fr/${credentials.api}/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&LAYER=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&EXCEPTIONS=text/xml&FORMAT=image/jpeg&TILEMATRIXSET=PM&TILEMATRIX=${zoomLvl}&TILEROW=${tile.row}&TILECOL=${tile.column}&"

            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = BufferedInputStream(connection.inputStream)
            val myBitmap = BitmapFactory.decodeStream(inputStream, null, bitmapLoadingOptions)
            inputStream.close()
            myBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}