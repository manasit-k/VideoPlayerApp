package com.template.feature.videoplayer.data.repository

import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.data.datasource.VideoDataSource
import com.template.feature.videoplayer.domain.VideoItem
import com.template.feature.videoplayer.domain.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    private val dataSource: VideoDataSource
) : VideoRepository {

    override fun getAllVideos(): Flow<ApiResult<List<VideoItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val videos = dataSource.fetchVideos()
            Timber.d("Loaded ${videos.size} videos from MediaStore")
            emit(ApiResult.Success(videos))
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied when accessing videos")
            emit(ApiResult.Error(e))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load videos")
            emit(ApiResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getVideosInFolder(folderName: String): Flow<ApiResult<List<VideoItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val videos = dataSource.fetchVideos().filter { it.folderName == folderName }
            Timber.d("Loaded ${videos.size} videos from MediaStore for folder: $folderName")
            emit(ApiResult.Success(videos))
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied when accessing videos in folder: $folderName")
            emit(ApiResult.Error(e))
        } catch (e: Exception) {
            Timber.e(e, "Failed to load videos in folder: $folderName")
            emit(ApiResult.Error(e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun refreshVideos() {
        try {
            val videos = dataSource.fetchVideos()
            Timber.d("Refreshed ${videos.size} videos from MediaStore")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh videos")
            throw e
        }
    }
}
