package com.template.feature.videoplayer.di

import android.app.Application
import com.template.feature.videoplayer.presentation.ExoPlayerFactory
import androidx.media3.exoplayer.ExoPlayer
import javax.inject.Inject

/**
 * Production implementation that creates real ExoPlayer instances.
 */
class DefaultExoPlayerFactory @Inject constructor(
    private val application: Application
) : ExoPlayerFactory {

    override fun create(): ExoPlayer =
        ExoPlayer.Builder(application).build()
}
