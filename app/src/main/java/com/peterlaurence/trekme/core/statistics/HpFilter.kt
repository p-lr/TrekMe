package com.peterlaurence.trekme.core.statistics

/**
 * Implementation of Hodrick–Prescott filter.
 */
fun hpfilter(data: DoubleArray, lambda: Double = 1600.0): DoubleArray {
    val n = data.size

    if (n < 3) {
        return data
    }

    val a = DoubleArray(n)
    val b = DoubleArray(n)
    val c = DoubleArray(n)

    a[0] = 1 + lambda
    b[0] = -2 * lambda
    c[0] = lambda

    var K = 1
    while (K < n - 2) {
        a[K] = 6 * lambda + 1
        b[K] = -4 * lambda
        c[K] = lambda
        K++
    }
    a[1] = 5 * lambda + 1
    a[n - 1] = 1 + lambda
    a[n - 2] = 5 * lambda + 1
    b[0] = -2 * lambda
    b[n - 2] = -2 * lambda
    b[n - 1] = 0.0
    c[n - 2] = 0.0
    c[n - 1] = 0.0

    return pentas(a, b, c, data, n)
}

/**
 * Solves the linear equation system BxX=Y with B being a pentadiagonal matrix.
 */
private fun pentas(a: DoubleArray, b: DoubleArray, c: DoubleArray, data: DoubleArray, N: Int): DoubleArray {
    var K: Int = 0
    var H1 = 0.0
    var H2 = 0.0
    var H3 = 0.0
    var H4 = 0.0
    var H5 = 0.0
    var HH1: Double
    var HH2 = 0.0
    var HH3 = 0.0
    var HH5 = 0.0
    var Z: Double
    var HB: Double
    var HC: Double

    while (K < N) {
        Z = a[K] - H4 * H1 - HH5 * HH2
        HB = b[K]
        HH1 = H1
        H1 = (HB - H4 * H2) / Z
        b[K] = H1
        HC = c[K]
        HH2 = H2
        H2 = HC / Z
        c[K] = H2
        a[K] = (data[K] - HH3 * HH5 - H3 * H4) / Z
        HH3 = H3
        H3 = a[K]
        H4 = HB - H5 * HH1
        HH5 = H5
        H5 = HC
        K++
    }

    H2 = 0.0
    H1 = a[N - 1]
    data[N - 1] = H1

    K = N - 2
    while (K > -1) {

        data[K] = a[K] - b[K] * H1 - c[K] * H2
        H2 = H1
        H1 = data[K]
        K--
    }

    return data
}