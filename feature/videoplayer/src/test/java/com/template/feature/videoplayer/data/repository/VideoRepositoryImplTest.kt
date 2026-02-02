package com.template.feature.videoplayer.data.repository

import android.net.Uri
import app.cash.turbine.test
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.data.datasource.VideoDataSource
import com.template.feature.videoplayer.domain.VideoItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoRepositoryImplTest {

    private lateinit var dataSource: VideoDataSource
    private lateinit var repository: VideoRepositoryImpl

    private val video1 = createVideoItem(1L, "Video1", "FolderA")
    private val video2 = createVideoItem(2L, "Video2", "FolderA")
    private val video3 = createVideoItem(3L, "Video3", "FolderB")

    @Before
    fun setup() {
        dataSource = mockk(relaxed = true)
        repository = VideoRepositoryImpl(dataSource)
    }

    @Test
    fun `getAllVideos emits Loading then Success when dataSource returns list`() = runTest {
        every { dataSource.fetchVideos() } returns listOf(video1, video2, video3)

        repository.getAllVideos().test {
            assertEquals(ApiResult.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is ApiResult.Success)
            assertEquals(3, (success as ApiResult.Success).data.size)
            awaitComplete()
        }
    }

    @Test
    fun `getAllVideos emits Loading then Success empty when dataSource returns empty`() = runTest {
        every { dataSource.fetchVideos() } returns emptyList()

        repository.getAllVideos().test {
            assertEquals(ApiResult.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is ApiResult.Success)
            assertTrue((success as ApiResult.Success).data.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getAllVideos emits Loading then Error on SecurityException`() = runTest {
        val exception = SecurityException("Permission denied")
        every { dataSource.fetchVideos() } throws exception

        repository.getAllVideos().test {
            assertEquals(ApiResult.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ApiResult.Error)
            assertEquals(exception, (error as ApiResult.Error).exception)
            awaitComplete()
        }
    }

    @Test
    fun `getAllVideos emits Loading then Error on generic Exception`() = runTest {
        val exception = Exception("Failed to load")
        every { dataSource.fetchVideos() } throws exception

        repository.getAllVideos().test {
            assertEquals(ApiResult.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ApiResult.Error)
            assertEquals(exception, (error as ApiResult.Error).exception)
            awaitComplete()
        }
    }

    @Test
    fun `getVideosInFolder emits Loading then Success filtered by folder`() = runTest {
        every { dataSource.fetchVideos() } returns listOf(video1, video2, video3)

        repository.getVideosInFolder("FolderA").test {
            assertEquals(ApiResult.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is ApiResult.Success)
            assertEquals(2, (success as ApiResult.Success).data.size)
            assertTrue(success.data.all { it.folderName == "FolderA" })
            awaitComplete()
        }
    }

    @Test
    fun `getVideosInFolder emits Success empty when no matching folder`() = runTest {
        every { dataSource.fetchVideos() } returns listOf(video1, video2)

        repository.getVideosInFolder("FolderC").test {
            assertEquals(ApiResult.Loading, awaitItem())
            val success = awaitItem()
            assertTrue(success is ApiResult.Success)
            assertTrue((success as ApiResult.Success).data.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getVideosInFolder emits Loading then Error on SecurityException`() = runTest {
        val exception = SecurityException("Permission denied")
        every { dataSource.fetchVideos() } throws exception

        repository.getVideosInFolder("FolderA").test {
            assertEquals(ApiResult.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ApiResult.Error)
            assertEquals(exception, (error as ApiResult.Error).exception)
            awaitComplete()
        }
    }

    @Test
    fun `getVideosInFolder emits Loading then Error on generic Exception`() = runTest {
        val exception = Exception("Failed to load folder")
        every { dataSource.fetchVideos() } throws exception

        repository.getVideosInFolder("FolderA").test {
            assertEquals(ApiResult.Loading, awaitItem())
            val error = awaitItem()
            assertTrue(error is ApiResult.Error)
            assertEquals(exception, (error as ApiResult.Error).exception)
            awaitComplete()
        }
    }

    @Test
    fun `refreshVideos completes when dataSource succeeds`() = runTest {
        every { dataSource.fetchVideos() } returns listOf(video1)

        repository.refreshVideos()
        // no throw
    }

    @Test
    fun `refreshVideos throws when dataSource throws`() = runTest {
        val exception = Exception("Refresh failed")
        every { dataSource.fetchVideos() } throws exception

        try {
            repository.refreshVideos()
            throw AssertionError("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals(exception, e)
        }
    }

    private fun createVideoItem(id: Long, name: String, folderName: String): VideoItem =
        VideoItem(
            id = id,
            uri = Uri.parse("content://media/external/video/media/$id"),
            name = name,
            duration = 10000L,
            size = 1024000L,
            folderName = folderName,
            dateModified = System.currentTimeMillis()
        )
}
