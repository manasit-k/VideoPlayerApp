package com.template.feature.dashboard.data.remote

import com.template.feature.dashboard.data.remote.dto.DashboardItemDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Dashboard API service
 */
interface DashboardApiService {
    
    @GET("dashboard/items")
    suspend fun getDashboardItems(): List<DashboardItemDto>
    
    @GET("dashboard/items/{id}")
    suspend fun getDashboardItemById(@Path("id") id: String): DashboardItemDto
}
