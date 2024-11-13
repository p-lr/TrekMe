package com.peterlaurence.trekme.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.cacheGet
import java.io.IOException
import java.util.zip.GZIPInputStream
import kotlin.coroutines.resumeWithException

suspend inline fun <reified T> OkHttpClient.performRequest(request: Request, json: Json): T? =
    withContext(Dispatchers.IO) {
        runCatching {
            val cachedResponse = cache?.let { cacheGet(it, request) }
            val str = if (cachedResponse != null && cachedResponse.isSuccessful) {
                val body = cachedResponse.body!! // on purpose (!!)
                /* The cache may contain gzipped content */
                if (cachedResponse.isGzip()) {
                    GZIPInputStream(body.byteStream()).bufferedReader().use { it.readText() }
                } else {
                    body.string()
                }
            } else {
                val call = newCall(request)
                val r = call.await()
                r?.string()!!
            }
            json.decodeFromString<T>(str)
        }.onFailure {
            Log.e("Http error", null, it)
        }.getOrNull()
    }


suspend fun OkHttpClient.performRequest(request: Request): String? = withContext(Dispatchers.IO) {
    runCatching {
        val call = newCall(request)
        val r = call.await()
        r?.string()
    }.getOrNull()
}

suspend fun Call.await() = suspendCancellableCoroutine<ResponseBody?> { continuation ->
    continuation.invokeOnCancellation {
        cancel()
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response.body) {}
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
}

fun Response.isGzip(): Boolean {
    return header("Content-Encoding") == "gzip"
}
