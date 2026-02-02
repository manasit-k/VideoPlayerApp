package com.template.feature.videoplayer.presentation.gallery

import android.net.Uri
import app.cash.turbine.test
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.VideoItem
import com.template.feature.videoplayer.domain.repository.VideoRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
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
class VideoGalleryViewModelTest {

    private lateinit var repository: VideoRepository
    private lateinit var viewModel: VideoGalleryViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = VideoGalleryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.videos.isEmpty())
            assertTrue(state.folders.isEmpty())
            assertNull(state.error)
            assertEquals(ViewMode.LIST, state.viewMode)
            assertEquals(SortOption.DATE_NEWEST, state.sortOption)
            assertFalse(state.showSortMenu)
        }
    }

    @Test
    fun `loadVideos should emit loading then success`() = runTest(testDispatcher) {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 6000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.loadVideos()
            // May get Loading then Success, or Success directly depending on dispatcher
            var state = awaitItem()
            while (state.isLoading) {
                state = awaitItem()
            }
            assertFalse(state.isLoading)
            assertEquals(3, state.videos.size)
            assertEquals(2, state.folders.size)
            assertTrue(state.folders.containsKey("Folder1"))
            assertTrue(state.folders.containsKey("Folder2"))
        }
    }

    @Test
    fun `loadVideos should handle error`() = runTest(testDispatcher) {
        val exception = Exception("Network error")

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            viewModel.loadVideos()
            var state = awaitItem()
            while (state.isLoading) {
                state = awaitItem()
            }
            assertFalse(state.isLoading)
            assertEquals("Network error", state.error)
        }
    }

    @Test
    fun `toggleViewMode should switch between LIST and GRID`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertEquals(ViewMode.LIST, initialState.viewMode)

            viewModel.toggleViewMode()
            val gridState = awaitItem()
            assertEquals(ViewMode.GRID, gridState.viewMode)

            viewModel.toggleViewMode()
            val listState = awaitItem()
            assertEquals(ViewMode.LIST, listState.viewMode)
        }
    }

    @Test
    fun `setSortOption should update sort option and apply sorting`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "C Video", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "A Video", "Folder1", 2000L, 6000L, 2000000L),
            createVideoItem(3L, "B Video", "Folder2", 3000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1) // Skip initial state after load

            viewModel.setSortOption(SortOption.NAME_ASC)
            skipItems(1) // setSortOption emits twice (option update + applySortingAndGrouping)
            val sortedState = awaitItem()
            assertEquals(SortOption.NAME_ASC, sortedState.sortOption)
            assertFalse(sortedState.showSortMenu)
            assertEquals("A Video", sortedState.videos.first().name)
            assertEquals("C Video", sortedState.videos.last().name)
        }
    }

    @Test
    fun `setSortOption should close sort menu`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.showSortMenu)

            viewModel.toggleSortMenu()
            val menuOpenState = awaitItem()
            assertTrue(menuOpenState.showSortMenu)

            viewModel.setSortOption(SortOption.NAME_DESC)
            val menuClosedState = awaitItem()
            assertFalse(menuClosedState.showSortMenu)
        }
    }

    @Test
    fun `sorting by NAME_ASC should sort alphabetically ascending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Zebra", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Apple", "Folder1", 2000L, 6000L, 2000000L),
            createVideoItem(3L, "Banana", "Folder2", 3000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.NAME_ASC)
            skipItems(1) // Second emission has sorted videos
            val state = awaitItem()
            assertEquals("Apple", state.videos[0].name)
            assertEquals("Banana", state.videos[1].name)
            assertEquals("Zebra", state.videos[2].name)
        }
    }

    @Test
    fun `sorting by NAME_DESC should sort alphabetically descending`() = runTest(testDispatcher) {
        val videos = listOf(
            createVideoItem(1L, "Apple", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Banana", "Folder1", 2000L, 6000L, 2000000L),
            createVideoItem(3L, "Zebra", "Folder2", 3000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.loadVideos()
            var state = awaitItem()
            while (state.videos.isEmpty()) {
                state = awaitItem()
            }
            viewModel.setSortOption(SortOption.NAME_DESC)
            state = awaitItem()
            if (state.videos.isNotEmpty() && state.videos[0].name != "Zebra") {
                state = awaitItem()
            }
            assertEquals("Zebra", state.videos[0].name)
            assertEquals("Banana", state.videos[1].name)
            assertEquals("Apple", state.videos[2].name)
        }
    }

    @Test
    fun `sorting by DATE_NEWEST should sort by date descending`() = runTest(testDispatcher) {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 3000L, 6000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 2000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.loadVideos()
            var state = awaitItem()
            while (state.videos.isEmpty()) {
                state = awaitItem()
            }
            // Default sortOption is DATE_NEWEST, so loaded videos are already sorted by date descending
            assertEquals(3, state.videos.size)
            assertEquals(SortOption.DATE_NEWEST, state.sortOption)
            assertEquals(3000L, state.videos[0].dateModified)
            assertEquals(2000L, state.videos[1].dateModified)
            assertEquals(1000L, state.videos[2].dateModified)
        }
    }

    @Test
    fun `sorting by DATE_OLDEST should sort by date ascending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 3000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 1000L, 6000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 2000L, 7000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.DATE_OLDEST)
            skipItems(1)
            val state = awaitItem()
            assertEquals(1000L, state.videos[0].dateModified)
            assertEquals(2000L, state.videos[1].dateModified)
            assertEquals(3000L, state.videos[2].dateModified)
        }
    }

    @Test
    fun `sorting by SIZE_LARGEST should sort by size descending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 6000L, 3000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 7000L, 2000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.SIZE_LARGEST)
            skipItems(1)
            val state = awaitItem()
            assertEquals(3000000L, state.videos[0].size)
            assertEquals(2000000L, state.videos[1].size)
            assertEquals(1000000L, state.videos[2].size)
        }
    }

    @Test
    fun `sorting by SIZE_SMALLEST should sort by size ascending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 3000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 6000L, 1000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 7000L, 2000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.SIZE_SMALLEST)
            skipItems(1)
            val state = awaitItem()
            assertEquals(1000000L, state.videos[0].size)
            assertEquals(2000000L, state.videos[1].size)
            assertEquals(3000000L, state.videos[2].size)
        }
    }

    @Test
    fun `sorting by DURATION_LONGEST should sort by duration descending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 7000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 6000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.DURATION_LONGEST)
            skipItems(1)
            val state = awaitItem()
            assertEquals(7000L, state.videos[0].duration)
            assertEquals(6000L, state.videos[1].duration)
            assertEquals(5000L, state.videos[2].duration)
        }
    }

    @Test
    fun `sorting by DURATION_SHORTEST should sort by duration ascending`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 7000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 5000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 6000L, 3000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.loadVideos()

        viewModel.uiState.test {
            skipItems(1)

            viewModel.setSortOption(SortOption.DURATION_SHORTEST)
            skipItems(1)
            val state = awaitItem()
            assertEquals(5000L, state.videos[0].duration)
            assertEquals(6000L, state.videos[1].duration)
            assertEquals(7000L, state.videos[2].duration)
        }
    }

    @Test
    fun `toggleSortMenu should toggle showSortMenu`() = runTest {
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertFalse(initialState.showSortMenu)

            viewModel.toggleSortMenu()
            val openState = awaitItem()
            assertTrue(openState.showSortMenu)

            viewModel.toggleSortMenu()
            val closedState = awaitItem()
            assertFalse(closedState.showSortMenu)
        }
    }

    @Test
    fun `dismissSortMenu should close sort menu`() = runTest {
        viewModel.uiState.test {
            val initial = awaitItem()
            viewModel.toggleSortMenu()
            val menuOpenState = awaitItem()
            assertTrue(menuOpenState.showSortMenu)

            viewModel.dismissSortMenu()
            val closedState = awaitItem()
            assertFalse(closedState.showSortMenu)
        }
    }

    @Test
    fun `retry should reload videos`() = runTest {
        val videos = listOf(createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L))

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial
            viewModel.retry()
            val state = awaitItem()
            assertEquals(1, state.videos.size)
        }
    }

    @Test
    fun `clearError should clear error state`() = runTest(testDispatcher) {
        val exception = Exception("Test error")

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial
            viewModel.loadVideos()
            var state = awaitItem()
            while (state.isLoading) {
                state = awaitItem()
            }
            assertNotNull(state.error)

            viewModel.clearError()
            val clearedState = awaitItem()
            assertNull(clearedState.error)
        }
    }

    @Test
    fun `videos should be grouped by folder name`() = runTest {
        val videos = listOf(
            createVideoItem(1L, "Video1", "Folder1", 1000L, 5000L, 1000000L),
            createVideoItem(2L, "Video2", "Folder1", 2000L, 6000L, 2000000L),
            createVideoItem(3L, "Video3", "Folder2", 3000L, 7000L, 3000000L),
            createVideoItem(4L, "Video4", "Folder2", 4000L, 8000L, 4000000L),
            createVideoItem(5L, "Video5", "Folder2", 5000L, 9000L, 5000000L)
        )

        coEvery { repository.getAllVideos() } returns flowOf(
            ApiResult.Success(videos)
        )

        viewModel.uiState.test {
            skipItems(1) // Skip initial
            viewModel.loadVideos()
            val state = awaitItem()
            assertEquals(2, state.folders.size)
            assertEquals(2, state.folders["Folder1"]?.size)
            assertEquals(3, state.folders["Folder2"]?.size)
        }
    }

    private fun createVideoItem(
        id: Long,
        name: String,
        folderName: String,
        dateModified: Long,
        duration: Long,
        size: Long
    ): VideoItem {
        return VideoItem(
            id = id,
            uri = Uri.parse("content://media/external/video/media/$id"),
            name = name,
            duration = duration,
            size = size,
            folderName = folderName,
            dateModified = dateModified
        )
    }
}
