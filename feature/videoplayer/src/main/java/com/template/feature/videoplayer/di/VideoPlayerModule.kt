package com.template.feature.videoplayer.di

import android.content.Context
import com.template.feature.videoplayer.data.repository.VideoRepositoryImpl
import com.template.feature.videoplayer.domain.repository.VideoRepository
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
    fun provideVideoRepository(
        @ApplicationContext context: Context
    ): VideoRepository {
        return VideoRepositoryImpl(context)
    }
}
