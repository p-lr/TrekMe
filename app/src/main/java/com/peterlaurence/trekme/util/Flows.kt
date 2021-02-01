package com.peterlaurence.trekme.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

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