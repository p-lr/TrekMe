package com.peterlaurence.trekadvisor.service.event

import com.peterlaurence.trekadvisor.core.map.Map
import com.peterlaurence.trekadvisor.core.map.mapimporter.MapImporter

data class PostProcessImportedEvent(val map: Map, val status: MapImporter.MapParserStatus)