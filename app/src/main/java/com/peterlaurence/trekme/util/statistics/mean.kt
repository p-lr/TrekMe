package com.peterlaurence.trekme.util.statistics

fun List<Double>.mean(): Double {
    var sum = 0.0
    forEach { sum += it }
    return sum / size
}