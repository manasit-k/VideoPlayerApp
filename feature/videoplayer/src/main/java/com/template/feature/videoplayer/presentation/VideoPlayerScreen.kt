package com.template.feature.videoplayer.presentation

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import android.view.LayoutInflater
import android.widget.TextView
import androidx.compose.ui.viewinterop.AndroidView
import com.template.feature.videoplayer.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    startVideoId: Long = -1,
    folderName: String = "",
    videoUri: String? = null,
    onNavigateUp: () -> Unit = {},
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

    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            delay(200)
            viewModel.updatePlaybackState(p.currentPosition, p.duration.coerceAtLeast(0L))
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
            .background(Color.Black)
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
                            this.keepScreenOn = true
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
                        // ผูกปุ่ม Auto-play ใน custom controller
                        playerView.findViewById<TextView>(R.id.exo_auto_play)?.let { autoPlayView ->
                            autoPlayView.text = if (uiState.autoPlayEnabled) "Auto-play ON" else "Auto-play OFF"
                            autoPlayView.setOnClickListener { viewModel.toggleAutoPlay() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            var cumulativeDragMs = 0L
                            var startPositionMs = 0L
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    cumulativeDragMs = 0L
                                    startPositionMs = exoPlayer.currentPosition
                                    viewModel.setSeekDragState(isDragging = true, deltaMs = 0L)
                                },
                                onDragEnd = {
                                    viewModel.setSeekDragState(isDragging = false, deltaMs = 0L)
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    val sensitivity = 25L
                                    val seekAmount = (dragAmount * sensitivity).toLong()
                                    cumulativeDragMs += seekAmount
                                    val targetPosition = (startPositionMs + cumulativeDragMs)
                                        .coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
                                    viewModel.seekTo(targetPosition)
                                    viewModel.setSeekDragState(isDragging = true, deltaMs = cumulativeDragMs)
                                    change.consume()
                                }
                            )
                        }
                )
            }
        }

        // Top Bar Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = uiState.currentVideoTitle,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Loading State
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Error State
        uiState.error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }

        // Seek bar overlay ตอนลากนิ้ว (ไว้ด้านล่างเหนือแถบควบคุม ไม่ทับปุ่มกลาง)
        if (uiState.isSeekDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 72.dp)
            ) {
                // พื้นหลัง bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
                // ความคืบหน้าตามตำแหน่งปัจจุบัน
                val duration = uiState.durationMs.coerceAtLeast(1L)
                val progress = (uiState.currentPositionMs.toFloat() / duration).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                // ข้อความ +/- วินาทีที่ลาก
                val deltaSec = uiState.seekDragDeltaMs / 1000f
                val deltaText = when {
                    deltaSec > 0 -> "+%.1fs".format(deltaSec)
                    deltaSec < 0 -> "%.1fs".format(deltaSec)
                    else -> ""
                }
                if (deltaText.isNotEmpty()) {
                    Text(
                        text = deltaText,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

    }
}

