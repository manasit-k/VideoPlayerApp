package com.template.app.di

import com.template.app.navigation.AppNavigatorImpl
import com.template.core.navigation.AppNavigator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for app-level dependencies
 * Binds AppNavigator interface to implementation
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppNavigator(impl: AppNavigatorImpl): AppNavigator {
        return impl
    }
}
