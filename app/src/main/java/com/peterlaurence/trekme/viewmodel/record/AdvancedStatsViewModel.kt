package com.peterlaurence.trekme.viewmodel.record

import androidx.lifecycle.ViewModel

class AdvancedStatsViewModel : ViewModel() {
}


/**
 * A point representing the altitude at a given distance from the departure.
 *
 * @param dist distance in meters
 * @param altitude altitude in meters
 */
data class AltPoint(val dist: Double, val altitude: Double)