package com.template.feature.videoplayer.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PlayerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val currentVideoTitle: String = "",
    val isPlaying: Boolean = false,
    val autoPlayEnabled: Boolean = true
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    private val application: Application,
    private val repository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _uiState.update { it.copy(isShuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _uiState.update { it.copy(repeatMode = repeatMode) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _uiState.update { it.copy(playbackSpeed = playbackParameters.speed) }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
            _uiState.update { it.copy(currentVideoTitle = title) }
            Timber.d("Now playing: $title (reason: $reason)")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    // Handle auto-play next
                    if (_uiState.value.autoPlayEnabled) {
                        handleAutoPlayNext()
                    }
                }
                Player.STATE_READY -> {
                    Timber.d("Player ready")
                }
                Player.STATE_BUFFERING -> {
                    Timber.d("Player buffering")
                }
                Player.STATE_IDLE -> {
                    Timber.d("Player idle")
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Timber.e(error, "Player error: ${error.message}")
            _uiState.update { it.copy(error = error.message ?: "Playback error") }
        }
    }

    private fun handleAutoPlayNext() {
        _player.value?.let { exoPlayer ->
            val hasNext = exoPlayer.hasNextMediaItem()
            if (hasNext) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.play()
                Timber.d("Auto-playing next video")
            } else {
                Timber.d("No more videos to auto-play")
            }
        }
    }

    fun initializePlayer(startVideoId: Long, folderName: String) {
        if (_player.value != null) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            repository.getVideosInFolder(folderName).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is ApiResult.Success -> {
                        val videos = result.data
                        if (videos.isEmpty()) {
                            _uiState.update { it.copy(isLoading = false, error = "No videos found") }
                            return@collect
                        }

                        val startIndex = videos.indexOfFirst { it.id == startVideoId }.coerceAtLeast(0)

                        _player.value = ExoPlayer.Builder(application).build().apply {
                            addListener(playerListener)
                            val mediaItems = videos.map { video ->
                                MediaItem.Builder()
                                    .setUri(video.uri)
                                    .setMediaMetadata(
                                        androidx.media3.common.MediaMetadata.Builder()
                                            .setTitle(video.name)
                                            .build()
                                    )
                                    .build()
                            }
                            setMediaItems(mediaItems)
                            seekTo(startIndex, 0)
                            prepare()
                            playWhenReady = true
                        }

                        _uiState.update { it.copy(isLoading = false) }
                        Timber.d("Player initialized with ${videos.size} videos, starting at index $startIndex")
                    }
                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Failed to load videos"
                            )
                        }
                        Timber.e(result.exception, "Failed to initialize player")
                    }
                }
            }
        }
    }

    fun initializePlayerWithUri(uriString: String) {
        if (_player.value != null) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        _player.value = ExoPlayer.Builder(application).build().apply {
            addListener(playerListener)
            setMediaItem(MediaItem.fromUri(uriString))
            prepare()
            playWhenReady = true
        }

        _uiState.update { it.copy(isLoading = false) }
        Timber.d("Player initialized with URI: $uriString")
    }

    fun releasePlayer() {
        _player.value?.removeListener(playerListener)
        _player.value?.release()
        _player.value = null
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
    }

    fun toggleShuffle() {
        _player.value?.let {
            val newShuffleMode = !it.shuffleModeEnabled
            it.shuffleModeEnabled = newShuffleMode

            // If Shuffle is turned ON and Repeat is currently ONE, switch to ALL
            // because Shuffle + Repeat One doesn't make sense (it would just repeat the same video)
            if (newShuffleMode && it.repeatMode == Player.REPEAT_MODE_ONE) {
                it.repeatMode = Player.REPEAT_MODE_ALL
            }

            Timber.d("Shuffle mode: $newShuffleMode")
        }
    }

    fun toggleRepeatMode() {
        _player.value?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                else -> Player.REPEAT_MODE_OFF
            }
            Timber.d("Repeat mode: ${it.repeatMode}")
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _player.value?.setPlaybackSpeed(speed)
        Timber.d("Playback speed: $speed")
    }

    fun playNext() {
        _player.value?.seekToNext()
    }

    fun playPrevious() {
        _player.value?.seekToPrevious()
    }

    fun seek(timeMs: Long) {
        _player.value?.let {
            val current = it.currentPosition
            val newPosition = (current + timeMs).coerceIn(0, it.duration)
            it.seekTo(newPosition)
        }
    }

    fun seekTo(positionMs: Long) {
        _player.value?.seekTo(positionMs)
    }

    fun togglePlayPause() {
        _player.value?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun toggleAutoPlay() {
        _uiState.update { it.copy(autoPlayEnabled = !it.autoPlayEnabled) }
        Timber.d("Auto-play: ${_uiState.value.autoPlayEnabled}")
    }

    fun setAutoPlay(enabled: Boolean) {
        _uiState.update { it.copy(autoPlayEnabled = enabled) }
        Timber.d("Auto-play set to: $enabled")
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
