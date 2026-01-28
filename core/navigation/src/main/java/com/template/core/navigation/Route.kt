package com.template.core.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization
 * Each route is a sealed class with @Serializable annotation
 */
sealed interface Route {
    
    @Serializable
    data object Dashboard : Route
    
    @Serializable
    data class DashboardDetail(val id: String) : Route
    
    // Add more routes as needed
    @Serializable
    data class VideoPlayer(val startVideoId: Long, val folderName: String) : Route

    @Serializable
    data class VideoPlayerFromUri(val uri: String) : Route

    @Serializable
    data object VideoGallery : Route
}
