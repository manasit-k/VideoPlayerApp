package com.template.feature.dashboard.di

import com.template.feature.dashboard.data.remote.DashboardApiService
import com.template.feature.dashboard.data.repository.DashboardRepositoryImpl
import com.template.feature.dashboard.domain.repository.DashboardRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Hilt module for Dashboard feature
 * Provides dependencies for data layer
 */
@Module
@InstallIn(SingletonComponent::class)
object DashboardModule {
    
    @Provides
    @Singleton
    fun provideDashboardApiService(retrofit: Retrofit): DashboardApiService {
        return retrofit.create(DashboardApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDashboardRepository(
        apiService: DashboardApiService
    ): DashboardRepository {
        return DashboardRepositoryImpl(apiService)
    }
}
