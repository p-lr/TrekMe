package com.peterlaurence.trekme.ui.maplist.events

sealed class ZipEvent
data class ZipProgressEvent(val p: Int, val mapName: String, val mapId: Int): ZipEvent()
data class ZipFinishedEvent(val mapId: Int): ZipEvent()
object ZipError : ZipEvent()
object ZipCloseEvent: ZipEvent()    // sent after a ZipFinishedEvent to mark as fully completed