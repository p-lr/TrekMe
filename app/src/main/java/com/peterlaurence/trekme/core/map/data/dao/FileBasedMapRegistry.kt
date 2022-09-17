package com.peterlaurence.trekme.core.map.data.dao

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileBasedMapRegistry @Inject constructor() {
    val fileForId = ConcurrentHashMap<Int, File>()
}