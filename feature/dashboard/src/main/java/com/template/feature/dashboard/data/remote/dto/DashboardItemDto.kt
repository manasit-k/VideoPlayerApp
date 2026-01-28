package com.template.feature.dashboard.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data Transfer Object for Dashboard Item
 */
@JsonClass(generateAdapter = true)
data class DashboardItemDto(
    @Json(name = "id")
    val id: String,
    
    @Json(name = "title")
    val title: String,
    
    @Json(name = "description")
    val description: String,
    
    @Json(name = "imageUrl")
    val imageUrl: String?,
    
    @Json(name = "timestamp")
    val timestamp: Long
)
