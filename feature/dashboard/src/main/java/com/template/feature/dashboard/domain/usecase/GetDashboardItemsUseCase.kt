package com.template.feature.dashboard.domain.usecase

import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.domain.model.DashboardItem
import com.template.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting dashboard items
 * Encapsulates business logic and can be reused across different ViewModels
 */
class GetDashboardItemsUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    operator fun invoke(): Flow<ApiResult<List<DashboardItem>>> {
        return repository.getDashboardItems()
    }
}
