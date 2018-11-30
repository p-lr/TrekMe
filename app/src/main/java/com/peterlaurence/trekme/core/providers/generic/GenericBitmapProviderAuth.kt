package com.peterlaurence.trekme.core.providers.generic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Same as [GenericBitmapProvider], but using basic authentication.
 */
class GenericBitmapProviderAuth(override val urlTileBuilder: UrlTileBuilder, private val user: String, private val pwd: String,
                                options: BitmapFactory.Options? = null) : GenericBitmapProvider(urlTileBuilder, options) {

    override fun getBitmap(level: Int, row: Int, col: Int): Bitmap? {

        val url = URL(urlTileBuilder.build(level, row, col))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true

        /* Set authentication */
        connection.setAuth()

        return try {
            connection.connect()
            val inputStream = BufferedInputStream(connection.inputStream)
            val myBitmap = BitmapFactory.decodeStream(inputStream, null, bitmapLoadingOptions)
            inputStream.close()
            myBitmap
        } catch (e: Exception) {
            connection.disconnect()
            e.printStackTrace()
            null
        }
    }

    private fun HttpURLConnection.setAuth() {
        val authString = "$user:$pwd"
        val authStringEnc = String(Base64.encode(authString.toByteArray(), Base64.NO_WRAP))
        setRequestProperty("Authorization", "Basic $authStringEnc")
    }
}