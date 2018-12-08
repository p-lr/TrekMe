package com.peterlaurence.trekme.core.providers.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.peterlaurence.trekme.core.providers.urltilebuilder.UrlTileBuilder
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * Given the level, row and col numbers, a [GenericBitmapProvider] returns a [Bitmap] using the
 * provided [UrlTileBuilder] to build an [URL] and make an HTTP request.
 * Here is also defined the kind of provider used for each particular case (IGN, USGS, etc).
 */
open class GenericBitmapProvider protected constructor(open val urlTileBuilder: UrlTileBuilder, options: BitmapFactory.Options? = null) {
    companion object {
        fun getBitmapProviderIgn(urlTileBuilder: UrlTileBuilder, ignUser: String, ignPwd: String): GenericBitmapProvider {
            return GenericBitmapProviderAuth(urlTileBuilder, ignUser, ignPwd)
        }

        fun getBitmapProviderIgnSpain(urlTileBuilder: UrlTileBuilder): GenericBitmapProvider {
            return GenericBitmapProvider(urlTileBuilder)
        }

        fun getBitmapProviderOSM(urlTileBuilder: UrlTileBuilder): GenericBitmapProvider {
            return GenericBitmapProvider(urlTileBuilder)
        }

        fun getBitmapProviderUSGS(urlTileBuilder: UrlTileBuilder): GenericBitmapProvider {
            return GenericBitmapProvider(urlTileBuilder)
        }
    }

    var bitmapLoadingOptions = options ?: BitmapFactory.Options()

    init {
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565
    }

    open fun getBitmap(level: Int, row: Int, col: Int): Bitmap? {
        val url = URL(urlTileBuilder.build(level, row, col))
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true

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

    open fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}

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

