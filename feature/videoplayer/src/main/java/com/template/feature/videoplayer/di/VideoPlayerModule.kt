package com.template.feature.videoplayer.di

import android.app.Application
import android.content.Context
import com.template.feature.videoplayer.data.datasource.MediaStoreVideoDataSource
import com.template.feature.videoplayer.data.datasource.VideoDataSource
import com.template.feature.videoplayer.data.repository.VideoRepositoryImpl
import com.template.feature.videoplayer.domain.repository.VideoRepository
import com.template.feature.videoplayer.presentation.ExoPlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VideoPlayerModule {

    @Provides
    @Singleton
    fun provideVideoDataSource(
        @ApplicationContext context: Context
    ): VideoDataSource = MediaStoreVideoDataSource(context)

    @Provides
    @Singleton
    fun provideVideoRepository(
        dataSource: VideoDataSource
    ): VideoRepository = VideoRepositoryImpl(dataSource)

    @Provides
    @Singleton
    fun provideExoPlayerFactory(
        application: Application
    ): ExoPlayerFactory = DefaultExoPlayerFactory(application)
}
