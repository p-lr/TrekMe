package com.peterlaurence.trekme.core.map.domain.models

import java.util.concurrent.atomic.AtomicInteger

/**
 * An in-memory id generator.
 * This class is thread-safe.
 */
object MapIdGenerator {
    private val generator = AtomicInteger(0)

    fun increment(): Int {
        return generator.incrementAndGet()
    }
}