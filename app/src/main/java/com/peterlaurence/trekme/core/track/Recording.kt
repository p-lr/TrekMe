package com.peterlaurence.trekme.core.track

import java.io.File

typealias Recording = File
fun Recording.id() = hashCode()