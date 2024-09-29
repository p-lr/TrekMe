package com.peterlaurence.trekme.features.common.presentation.ui.mapcompose

import com.peterlaurence.trekme.core.map.domain.models.BoundingBox

sealed class Config
data class InitScaleAndScrollConfig(val scale: Float, val scrollX: Int, val scrollY: Int) : Config()
data class ScaleForZoomOnPositionConfig(val scale: Float) : Config()
data class ScaleLimitsConfig(val minScale: Float? = null, val maxScale: Float? = null) : Config()
data class LevelLimitsConfig(val levelMin: Int = 1, val levelMax: Int = 18) : Config()
data class BoundariesConfig(val boundingBoxList: List<BoundingBox>) : Config()

val ignConfig = listOf(
    ScaleLimitsConfig(maxScale = 2f),
    ScaleForZoomOnPositionConfig(scale = 0.25f),
    LevelLimitsConfig(levelMax = 18),
    BoundariesConfig(
        listOf(
            BoundingBox(41.21, 51.05, -4.92, 8.37),        // France
            BoundingBox(-21.39, -20.86, 55.20, 55.84),     // La Réunion
            BoundingBox(2.07, 5.82, -54.66, -51.53),       // Guyane
            BoundingBox(15.82, 16.54, -61.88, -60.95),     // Guadeloupe
            BoundingBox(18.0, 18.135, -63.162, -62.965),   // St Martin
            BoundingBox(17.856, 17.988, -62.957, -62.778), // St Barthélemy
            BoundingBox(14.35, 14.93, -61.31, -60.75),     // Martinique
            BoundingBox(-17.945, -17.46, -149.97, -149.1), // Tahiti
        )
    )
)

val osmConfig = listOf(
    ScaleLimitsConfig(maxScale = 4f),
    ScaleForZoomOnPositionConfig(scale = 1f),
    LevelLimitsConfig(levelMax = 16),
    BoundariesConfig(
        listOf(
            BoundingBox(-80.0, 83.0, -180.0, 180.0)        // World
        )
    )
)

val osmHdConfig = listOf(
    ScaleLimitsConfig(maxScale = 2f),
    ScaleForZoomOnPositionConfig(scale = 0.5f),
    LevelLimitsConfig(levelMax = 17),
    BoundariesConfig(
        listOf(
            BoundingBox(-80.0, 83.0, -180.0, 180.0)        // World
        )
    )
)

val usgsConfig = listOf(
    ScaleLimitsConfig(maxScale = 4f),
    ScaleForZoomOnPositionConfig(scale = 1f),
    LevelLimitsConfig(levelMax = 16),
    BoundariesConfig(
        listOf(
            BoundingBox(24.69, 49.44, -124.68, -66.5)
        )
    )
)

val swissTopoConfig = listOf(
    InitScaleAndScrollConfig(0.0023791015f, 41197, 27324),
    ScaleLimitsConfig(minScale = 0.0023791015f, maxScale = 2f),
    ScaleForZoomOnPositionConfig(scale = 0.5f),
    LevelLimitsConfig(levelMax = 17),
    BoundariesConfig(
        listOf(
            BoundingBox(45.78, 47.838, 5.98, 10.61)
        )
    )
)

val ignSpainConfig = listOf(
    InitScaleAndScrollConfig(8.1893284E-4f, 12958, 9399),
    ScaleLimitsConfig(minScale = 8.1893284E-4f, maxScale = 2f),
    ScaleForZoomOnPositionConfig(scale = 0.5f),
    LevelLimitsConfig(levelMax = 17),
    BoundariesConfig(
        listOf(
            BoundingBox(35.78, 43.81, -9.55, 3.32)
        )
    )
)

val ordnanceSurveyConfig = listOf(
    InitScaleAndScrollConfig(0.002746872f, 22207, 13755),
    ScaleLimitsConfig(minScale = 0.002746872f, maxScale = 4f),
    LevelLimitsConfig(7, 16),
    ScaleForZoomOnPositionConfig(scale = 1f),
    BoundariesConfig(
        listOf(
            BoundingBox(49.8, 61.08, -8.32, 2.04)
        )
    )
)

val ignBelgiumConfig = listOf(
    InitScaleAndScrollConfig(0.016787738f, 287963, 188014),
    ScaleLimitsConfig(minScale = 0.002f, maxScale = 4f),
    LevelLimitsConfig(7, 17),
    ScaleForZoomOnPositionConfig(scale = 1f),
    BoundariesConfig(
        listOf(
            BoundingBox(
                minLat = 49.4542277635,
                maxLat = 51.5056073315,
                minLon = 2.51141671277,
                maxLon = 6.57174320725
            )
        )
    )
)
