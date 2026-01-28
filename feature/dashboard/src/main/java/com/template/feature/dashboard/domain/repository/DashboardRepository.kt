package com.template.feature.dashboard.domain.repository

import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.domain.model.DashboardItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defined in domain layer
 * Implementation will be in data layer
 * This follows Dependency Inversion Principle
 */
interface DashboardRepository {
    
    fun getDashboardItems(): Flow<ApiResult<List<DashboardItem>>>
    
    fun getDashboardItemById(id: String): Flow<ApiResult<DashboardItem>>
}
