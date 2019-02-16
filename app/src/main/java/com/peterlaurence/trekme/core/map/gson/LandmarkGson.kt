package com.peterlaurence.trekme.core.map.gson

data class LandmarkGson(val landmarks: List<Landmark>)

data class Landmark(val name: String, val lat: Double, val lon: Double,
                    val proj_x: Double, val proj_y: Double, val comment: String)