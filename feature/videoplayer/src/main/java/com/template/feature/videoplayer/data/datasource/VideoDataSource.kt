package com.template.feature.videoplayer.data.datasource

import com.template.feature.videoplayer.domain.VideoItem

/**
 * Source of video items (e.g. MediaStore). Injected for testability.
 */
interface VideoDataSource {
    fun fetchVideos(): List<VideoItem>
}
