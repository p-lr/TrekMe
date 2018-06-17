package com.peterlaurence.trekadvisor.core.providers.generic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekadvisor.core.mapsource.IGNCredentials
import java.io.BufferedInputStream
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.PasswordAuthentication
import java.net.URL


class GenericBitmapProviderIgn(private val credentials: IGNCredentials, options: BitmapFactory.Options? = null) : GenericBitmapProvider {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(credentials.user, credentials.pwd?.toCharArray())
            }
        })
    }

    override fun getBitmap(level: Int, row: Int, col: Int): Bitmap? {
        return try {
            val src = "https://wxs.ign.fr/${credentials.api}/geoportail/wmts?SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&STYLE=normal&LAYER=GEOGRAPHICALGRIDSYSTEMS.MAPS.SCAN-EXPRESS.STANDARD&EXCEPTIONS=text/xml&FORMAT=image/jpeg&TILEMATRIXSET=PM&TILEMATRIX=$level&TILEROW=$row&TILECOL=$col&"

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

    override fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}