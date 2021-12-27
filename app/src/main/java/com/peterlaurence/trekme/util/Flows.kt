package com.peterlaurence.trekme.util

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Simple size-based chunk of a flow.
 */
fun <T> Flow<T>.chunk(maxSize: Int): Flow<List<T>> = flow {
    require(maxSize > 0) { "maxSize should be greater than 0" }

    val buffer = ArrayDeque<T>()
    collect { value ->
        buffer.add(value)
        if (buffer.size >= maxSize) {
            emit(buffer.toList())
            buffer.clear()
        }
    }

    if (buffer.isNotEmpty()) {
        emit(buffer.toList())
        buffer.clear()
    }
}

fun <T> Flow<T>.throttle(wait: Long) = channelFlow {
    val channel = Channel<T>(capacity = Channel.CONFLATED)
    coroutineScope {
        launch {
            collect {
                channel.send(it)
            }
        }
        launch {
            for (e in channel) {
                send(e)
                delay(wait)
            }
        }
    }
}