package com.template.feature.dashboard.domain.model

/**
 * Domain model for Dashboard Item
 * This is independent of any data source or presentation layer
 */
data class DashboardItem(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,
    val timestamp: Long
)
