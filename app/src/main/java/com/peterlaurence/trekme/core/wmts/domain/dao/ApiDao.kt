package com.peterlaurence.trekme.core.wmts.domain.dao

interface ApiDao {
    suspend fun getIgnApi(): String?
    suspend fun getOrdnanceSurveyApi(): String?
}