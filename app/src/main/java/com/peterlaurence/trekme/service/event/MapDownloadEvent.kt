package com.peterlaurence.trekme.service.event;

data class MapDownloadEvent(val status: Status, var progress: Double = 100.0)

enum class Status {
    FINISHED, PENDING, STORAGE_ERROR
}
