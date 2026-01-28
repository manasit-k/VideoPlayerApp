package com.template.feature.dashboard.data.mapper

import com.template.feature.dashboard.data.remote.dto.DashboardItemDto
import com.template.feature.dashboard.domain.model.DashboardItem

/**
 * Mapper to convert DTO to Domain Model
 */
fun DashboardItemDto.toDomain(): DashboardItem {
    return DashboardItem(
        id = this.id,
        title = this.title,
        description = this.description,
        imageUrl = this.imageUrl,
        timestamp = this.timestamp
    )
}

fun List<DashboardItemDto>.toDomain(): List<DashboardItem> {
    return this.map { it.toDomain() }
}
