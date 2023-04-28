package com.peterlaurence.trekme.core.wmts.domain.model

sealed class LayerProperties(open val layer: Layer, open val opacity: Float)

data class LayerPropertiesIgn(
    override val layer: IgnLayer, override val opacity: Float
) : LayerProperties(layer, opacity)