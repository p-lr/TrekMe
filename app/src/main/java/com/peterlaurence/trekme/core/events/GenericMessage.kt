package com.peterlaurence.trekme.core.events

sealed class GenericMessage
data class StandardMessage(val msg: String, val showLong: Boolean = false): GenericMessage()
data class WarningMessage(val title: String, val msg: String): GenericMessage()