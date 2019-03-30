package com.peterlaurence.trekme.ui.record.components.events

import com.peterlaurence.trekme.core.map.Map
import java.io.File

data class RequestImportRecording(val file: File, val map: Map)