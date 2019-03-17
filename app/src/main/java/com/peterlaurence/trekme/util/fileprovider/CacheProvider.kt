package com.peterlaurence.trekme.util.fileprovider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.io.FileNotFoundException


class CacheProvider : ContentProvider() {

    private var uriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    override fun onCreate(): Boolean {
        // Add a URI to the matcher which will match against the form
        // 'content://it.my.app.LogFileProvider/*'
        // and return 1 in the case that the incoming Uri matches this pattern
//        uriMatcher.addURI(AUTHORITY, "*", 1)

        return true
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {

        val LOG_TAG = CLASS_NAME

        Log.v(LOG_TAG,
                "Called with uri: '" + uri + "'." + uri.lastPathSegment)

        // Check incoming Uri against the matcher
        when (uriMatcher.match(uri)) {

            // If it returns 1 - then it matches the Uri defined in onCreate
            1 -> {
                val fileLocation = File(context!!.cacheDir, uri.lastPathSegment).path
                println("RecordProvider : " + fileLocation)

                return ParcelFileDescriptor.open(File(
                        fileLocation), ParcelFileDescriptor.MODE_READ_ONLY)
            }

            // Otherwise unrecognised Uri
            else -> {
                Log.v(LOG_TAG, "Unsupported uri: '$uri'.")
                throw FileNotFoundException("Unsupported uri: " + uri.toString())
            }
        }
    }

    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        super.attachInfo(context, info)

        info?.authority?.let {
            uriMatcher.addURI(it, "*", 1)
        }
    }

    // //////////////////////////////////////////////////////////////
    // Not supported / used / required for this example
    // //////////////////////////////////////////////////////////////

    override fun update(uri: Uri, contentvalues: ContentValues?, s: String?,
                        `as`: Array<String>?): Int {
        return 0
    }

    override fun delete(uri: Uri, s: String, `as`: Array<String>?): Int {
        return 0
    }

    override fun insert(uri: Uri, contentvalues: ContentValues?): Uri? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun query(uri: Uri, projection: Array<String>?, s: String?, as1: Array<String>?,
                       s1: String?): Cursor? {
        return null
    }

    companion object {

        private val CLASS_NAME = "CacheProvider"

        // The authority is the symbolic name for the provider class
        val AUTHORITY = "com.peterlaurence.trekme.CacheProvider"
    }
}