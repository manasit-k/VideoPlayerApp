package com.template.feature.videoplayer.data.datasource

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.template.feature.videoplayer.domain.VideoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Fetches videos from Android MediaStore. Used by [VideoRepositoryImpl].
 */
class MediaStoreVideoDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : VideoDataSource {

    override fun fetchVideos(): List<VideoItem> {
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
