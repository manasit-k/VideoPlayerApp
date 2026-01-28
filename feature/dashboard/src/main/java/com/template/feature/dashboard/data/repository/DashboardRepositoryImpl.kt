package com.template.feature.dashboard.data.repository

import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.data.mapper.toDomain
import com.template.feature.dashboard.data.remote.DashboardApiService
import com.template.feature.dashboard.domain.model.DashboardItem
import com.template.feature.dashboard.domain.repository.DashboardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Implementation of DashboardRepository
 * This is in the data layer and implements the interface from domain layer
 */
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: DashboardApiService
) : DashboardRepository {
    
    override fun getDashboardItems(): Flow<ApiResult<List<DashboardItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = apiService.getDashboardItems()
            emit(ApiResult.Success(response.toDomain()))
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }
    
    override fun getDashboardItemById(id: String): Flow<ApiResult<DashboardItem>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = apiService.getDashboardItemById(id)
            emit(ApiResult.Success(response.toDomain()))
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }
}
