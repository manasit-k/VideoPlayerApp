package com.template.feature.dashboard.presentation

import com.template.feature.dashboard.domain.model.DashboardItem

/**
 * UI State for Dashboard Screen
 * Represents all possible states of the UI
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val items: List<DashboardItem> = emptyList(),
    val error: String? = null
)
