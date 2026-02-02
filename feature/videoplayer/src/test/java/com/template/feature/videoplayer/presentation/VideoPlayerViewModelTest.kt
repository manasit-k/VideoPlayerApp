package com.template.feature.videoplayer.presentation

import android.app.Application
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.template.feature.videoplayer.presentation.ExoPlayerFactory
import app.cash.turbine.test
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.VideoItem
import com.template.feature.videoplayer.domain.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoPlayerViewModelTest {

    private lateinit var application: Application
    private lateinit var repository: VideoRepository
    private lateinit var mockPlayer: ExoPlayer
    private lateinit var playerFactory: ExoPlayerFactory
    private lateinit var viewModel: VideoPlayerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        application = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        mockPlayer = mockk(relaxed = true)
        playerFactory = ExoPlayerFactory { mockPlayer }
        viewModel = VideoPlayerViewModel(application, repository, playerFactory)
    }

    @After
    fun tearDown() {
        viewModel.releasePlayer()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertTrue(state.isShuffleEnabled) // default in PlayerUiState
            assertEquals(Player.REPEAT_MODE_OFF, state.repeatMode)
            assertEquals(1.0f, state.playbackSpeed)
            assertEquals("", state.currentVideoTitle)
            assertFalse(state.isPlaying)
            assertTrue(state.autoPlayEnabled)
        }
    }

    @Test
    fun `initializePlayer should load videos and create player`() = runTest {
        val folderName = "TestFolder"
        val startVideoId = 1L
        val videos = listOf(
            createVideoItem(1L, "Video1"),
            createVideoItem(2L, "Video2"),
            createVideoItem(3L, "Video3")
        )

        coEvery { repository.getVideosInFolder(folderName) } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.initializePlayer(startVideoId, folderName)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertNull(successState.error)
        }

        assertNotNull(viewModel.player.value)
    }

    @Test
    fun `initializePlayer should handle empty video list`() = runTest {
        val folderName = "TestFolder"
        val startVideoId = 1L

        coEvery { repository.getVideosInFolder(folderName) } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success(emptyList())
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.initializePlayer(startVideoId, folderName)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals("No videos found", errorState.error)
        }

        assertNull(viewModel.player.value)
    }

    @Test
    fun `initializePlayer should handle error`() = runTest {
        val folderName = "TestFolder"
        val startVideoId = 1L
        val exception = Exception("Network error")

        coEvery { repository.getVideosInFolder(folderName) } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.initializePlayer(startVideoId, folderName)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals("Network error", errorState.error)
        }

        assertNull(viewModel.player.value)
    }

    @Test
    fun `initializePlayer should not recreate player if already exists`() = runTest {
        val folderName = "TestFolder"
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(folderName) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, folderName)
            awaitItem()
            awaitItem()
        }
        val firstPlayer = viewModel.player.value

        viewModel.initializePlayer(1L, folderName)
        val secondPlayer = viewModel.player.value

        assertEquals(firstPlayer, secondPlayer)
    }

    @Test
    fun `initializePlayerWithUri should create player with single URI`() = runTest {
        val uriString = "https://example.com/video.mp4"

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.initializePlayerWithUri(uriString)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
        }

        assertNotNull(viewModel.player.value)
    }

    @Test
    fun `initializePlayerWithUri should not recreate player if already exists`() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayerWithUri("https://example.com/video1.mp4")
            awaitItem()
            awaitItem()
        }
        val firstPlayer = viewModel.player.value

        viewModel.initializePlayerWithUri("https://example.com/video2.mp4")
        val secondPlayer = viewModel.player.value

        assertEquals(firstPlayer, secondPlayer)
    }

    @Test
    fun `releasePlayer should clear player`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        assertNotNull(viewModel.player.value)

        viewModel.releasePlayer()
        assertNull(viewModel.player.value)
    }

    @Test
    fun `toggleShuffle should toggle shuffle mode`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem() // loading
            awaitItem() // success
        }
        viewModel.toggleShuffle()

        verify(exactly = 1) { mockPlayer.shuffleModeEnabled = any() }
    }

    @Test
    fun `toggleShuffle should change repeat mode from ONE to ALL when enabling shuffle`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )
        every { mockPlayer.shuffleModeEnabled } returns false
        every { mockPlayer.repeatMode } returns Player.REPEAT_MODE_ONE

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        viewModel.toggleShuffle()

        verify { mockPlayer.shuffleModeEnabled = true }
        verify { mockPlayer.repeatMode = Player.REPEAT_MODE_ALL }
    }

    @Test
    fun `toggleRepeatMode should cycle through repeat modes`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }

        every { mockPlayer.repeatMode } returns Player.REPEAT_MODE_OFF
        viewModel.toggleRepeatMode()
        verify { mockPlayer.repeatMode = Player.REPEAT_MODE_ONE }

        every { mockPlayer.repeatMode } returns Player.REPEAT_MODE_ONE
        viewModel.toggleRepeatMode()
        verify(atLeast = 1) { mockPlayer.repeatMode = Player.REPEAT_MODE_ALL }

        every { mockPlayer.repeatMode } returns Player.REPEAT_MODE_ALL
        viewModel.toggleRepeatMode()
        verify(atLeast = 1) { mockPlayer.repeatMode = Player.REPEAT_MODE_OFF }
    }

    @Test
    fun `setPlaybackSpeed should update playback speed`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        viewModel.setPlaybackSpeed(1.5f)

        verify { mockPlayer.setPlaybackSpeed(1.5f) }
    }

    @Test
    fun `playNext should seek to next`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1"),
            createVideoItem(2L, "Video2")
        )

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        viewModel.playNext()

        verify { mockPlayer.seekToNext() }
    }

    @Test
    fun `playPrevious should seek to previous`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1"),
            createVideoItem(2L, "Video2")
        )

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(2L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        viewModel.playPrevious()

        verify { mockPlayer.seekToPrevious() }
    }

    @Test
    fun `seek should adjust position within bounds`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        every { mockPlayer.currentPosition } returns 1000L
        every { mockPlayer.duration } returns 10000L

        viewModel.seek(5000L)

        verify { mockPlayer.seekTo(6000L) }
    }

    @Test
    fun `seekTo should set position`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }
        viewModel.seekTo(5000L)

        verify { mockPlayer.seekTo(5000L) }
    }

    @Test
    fun `togglePlayPause should toggle play state`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1"))

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            awaitItem()
            awaitItem()
        }

        every { mockPlayer.isPlaying } returns false
        viewModel.togglePlayPause()
        verify { mockPlayer.play() }

        every { mockPlayer.isPlaying } returns true
        viewModel.togglePlayPause()
        verify { mockPlayer.pause() }
    }

    @Test
    fun `toggleAutoPlay should toggle autoPlayEnabled`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertTrue(initialState.autoPlayEnabled)

            viewModel.toggleAutoPlay()
            val updatedState = awaitItem()
            assertFalse(updatedState.autoPlayEnabled)

            viewModel.toggleAutoPlay()
            val finalState = awaitItem()
            assertTrue(finalState.autoPlayEnabled)
        }
    }

    @Test
    fun `setAutoPlay should set autoPlayEnabled`() = runTest {
        viewModel.uiState.test {
            skipItems(1) // Skip initial state

            viewModel.setAutoPlay(false)
            val state1 = awaitItem()
            assertFalse(state1.autoPlayEnabled)

            viewModel.setAutoPlay(true)
            val state2 = awaitItem()
            assertTrue(state2.autoPlayEnabled)
        }
    }

    @Test
    fun `clearError should clear error state`() = runTest {
        val exception = Exception("Test error")

        coEvery { repository.getVideosInFolder(any()) } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.initializePlayer(1L, "TestFolder")
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            val errorState = awaitItem()
            assertNotNull(errorState.error)

            viewModel.clearError()
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    private fun createVideoItem(id: Long, name: String): VideoItem {
        return VideoItem(
            id = id,
            uri = Uri.parse("content://media/external/video/media/$id"),
            name = name,
            duration = 10000L,
            size = 1024000L,
            folderName = "TestFolder",
            dateModified = System.currentTimeMillis()
        )
    }
}
