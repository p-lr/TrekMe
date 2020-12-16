package com.peterlaurence.trekme.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resumeWithException


suspend inline fun <reified T> performRequest(client: OkHttpClient, request: Request): T? = withContext(Dispatchers.IO) {
    val call = client.newCall(request)
    val r = call.await()
    val str = runCatching { r?.string() }.getOrNull()
    if (str != null) {
        Json { isLenient = true; ignoreUnknownKeys = true }.decodeFromString<T>(str)
    } else null
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
