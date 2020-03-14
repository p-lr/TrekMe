package com.peterlaurence.trekme.ui.maplist.events

sealed class ZipEvent
data class ZipProgressEvent(val p: Int, val mapName: String, val mapId: Int): ZipEvent()
data class ZipFinishedEvent(val mapId: Int): ZipEvent()
object ZipError : ZipEvent()