package com.peterlaurence.trekme.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resumeWithException


suspend inline fun <reified T> OkHttpClient.performRequest(request: Request): T? = withContext(Dispatchers.IO) {
    val call = newCall(request)
    val r = call.await()
    runCatching {
        val str = r?.string()!!
        Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<T>(str)
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
