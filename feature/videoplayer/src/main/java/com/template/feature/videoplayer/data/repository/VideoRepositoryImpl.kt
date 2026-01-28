package com.template.feature.videoplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.VideoItem
import com.template.feature.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class VideoRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : VideoRepository {

    override fun getAllVideos(): Flow<ApiResult<List<VideoItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val videos = fetchFromMediaStore()
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
            val videos = fetchFromMediaStore().filter { it.folderName == folderName }
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
            val videos = fetchFromMediaStore()
            Timber.d("Refreshed ${videos.size} videos from MediaStore")
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh videos")
            throw e
        }
    }

    private fun fetchFromMediaStore(): List<VideoItem> {
        val videoList = mutableListOf<VideoItem>()

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_MODIFIED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val name = cursor.getString(nameColumn) ?: "Unknown"
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dateModified = cursor.getLong(dateModifiedColumn)
                        val path = cursor.getString(dataColumn) ?: ""

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        val folderName = if (path.isNotEmpty()) {
                            File(path).parentFile?.name ?: "Unknown"
                        } else {
                            "Unknown"
                        }

                        videoList.add(
                            VideoItem(
                                id = id,
                                uri = contentUri,
                                name = name,
                                duration = duration,
                                size = size,
                                folderName = folderName,
                                dateModified = dateModified
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse video entry, skipping")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query MediaStore")
            throw e
        }

        return videoList
    }
}
