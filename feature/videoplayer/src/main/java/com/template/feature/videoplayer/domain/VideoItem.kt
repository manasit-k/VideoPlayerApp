package com.template.feature.videoplayer.domain

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long,
    val folderName: String,
    val dateModified: Long = 0L
)
