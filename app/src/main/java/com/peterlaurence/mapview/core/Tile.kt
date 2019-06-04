package com.peterlaurence.mapview.core

import android.graphics.Bitmap

data class Tile(val zoom: Int, val row: Int, val col: Int, val bitmap: Bitmap)