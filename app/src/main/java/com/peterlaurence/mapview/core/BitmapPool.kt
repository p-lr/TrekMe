package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import java.util.*

/**
 * A pool of [Bitmap] which has a limited size.
 */
class BitmapPool {
    private val pool = LinkedList<Bitmap>()
    private var size: Int = 0
    private val threshold = 300
    private var cnt = 0

    fun getBitmap(): Bitmap? {
        // println("size pool $size allocationsCnt $cnt")
        return if (pool.isNotEmpty()) {
            size--
            pool.removeFirst()
        } else {
            ++cnt
            null
        }
    }

    fun putBitmap(bitmap: Bitmap) {
        // println("size pool $size allocationsCnt $cnt")
        if (size < threshold) {
            size++
            pool.add(bitmap)
        }
    }
}