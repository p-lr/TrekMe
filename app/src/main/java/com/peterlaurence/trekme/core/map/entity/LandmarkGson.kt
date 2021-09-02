package com.peterlaurence.trekme.core.map.entity

data class LandmarkGson(val landmarks: MutableList<Landmark>)

data class Landmark(var name: String, var lat: Double, var lon: Double,
                    var proj_x: Double?, var proj_y: Double?, var comment: String)