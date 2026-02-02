package com.template.feature.videoplayer.presentation

import androidx.media3.exoplayer.ExoPlayer

/**
 * Factory for creating ExoPlayer instances. Injected for testability.
 */
fun interface ExoPlayerFactory {
    fun create(): ExoPlayer
}
