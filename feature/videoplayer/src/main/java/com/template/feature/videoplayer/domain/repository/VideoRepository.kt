package com.template.feature.videoplayer.domain.repository

import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.VideoItem
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getAllVideos(): Flow<ApiResult<List<VideoItem>>>
    fun getVideosInFolder(folderName: String): Flow<ApiResult<List<VideoItem>>>
    suspend fun refreshVideos()
}
