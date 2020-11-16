package com.peterlaurence.trekme.core.statistics

fun <T,V> List<T>.rolling(window: Int, function: (List<T>) -> V): List<V> {
    val result = arrayListOf<V>()

    /* If the window is greater than the data size, then we get only one value */
    if (window >= size) {
        result.add(function(this))
    } else {
        for (i in 0..(size - window)) {
            result.add(function(subList(i, window + i)))
        }
    }

    return result
}