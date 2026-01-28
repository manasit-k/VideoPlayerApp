package com.template.feature.videoplayer.presentation

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.view.LayoutInflater
import androidx.compose.ui.viewinterop.AndroidView
import com.template.feature.videoplayer.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    startVideoId: Long = -1,
    folderName: String = "",
    videoUri: String? = null,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()

    LaunchedEffect(startVideoId, folderName, videoUri) {
        if (videoUri != null) {
            viewModel.initializePlayerWithUri(videoUri)
        } else {
            viewModel.initializePlayer(startVideoId, folderName)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.player.value?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Player View - use key to force recreation when player becomes available
        // This fixes the black screen issue where surface doesn't attach properly
        player?.let { exoPlayer ->
            // Key forces AndroidView recreation when player instance changes
            // This ensures TextureView surface is properly attached for vertical videos
            key(exoPlayer) {
                AndroidView(
                    factory = { ctx ->
                        val inflater = LayoutInflater.from(ctx)
                        (inflater.inflate(R.layout.player_view, null) as PlayerView).apply {
                            this.player = exoPlayer
                            // Force layout to ensure surface is attached for vertical videos
                            post {
                                requestLayout()
                                invalidate()
                            }
                        }
                    },
                    update = { playerView ->
                        if (playerView.player != exoPlayer) {
                            playerView.player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                val sensitivity = 100L
                                val seekAmount = (dragAmount * sensitivity).toLong()
                                viewModel.seek(seekAmount)
                                change.consume()
                            }
                        }
                )
            }
        }
        
        
        // Top controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto-play switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { viewModel.toggleAutoPlay() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Auto-play",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (uiState.autoPlayEnabled) {
                    Text("ON", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else {
                    Text("OFF", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                }
            }

            // Playback Controls Row (Shuffle & Repeat)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Shuffle Button
                Box(
                    modifier = Modifier
                        .background(
                            if (uiState.isShuffleEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) 
                            else Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.toggleShuffle() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Repeat Button
                Box(
                    modifier = Modifier
                        .background(
                            if (uiState.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            else Color.Black.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.toggleRepeatMode() }
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (uiState.repeatMode == Player.REPEAT_MODE_OFF) Color.Gray else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

