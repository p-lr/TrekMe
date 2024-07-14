package com.peterlaurence.trekme.events

sealed class GenericMessage
data class StandardMessage(val msg: String, val showLong: Boolean = false): GenericMessage()
data class WarningMessage(val title: String? = null, val msg: String): GenericMessage()
data class FatalMessage(val title: String, val msg: String): GenericMessage()