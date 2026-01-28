package com.template.feature.dashboard.data.repository

import app.cash.turbine.test
import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.data.mapper.toDomain
import com.template.feature.dashboard.data.remote.DashboardApiService
import com.template.feature.dashboard.data.remote.dto.DashboardItemDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardRepositoryImplTest {

    private lateinit var apiService: DashboardApiService
    private lateinit var repository: DashboardRepositoryImpl

    @Before
    fun setup() {
        apiService = mockk(relaxed = true)
        repository = DashboardRepositoryImpl(apiService)
    }

    @Test
    fun `getDashboardItems should emit loading then success`() = runTest {
        val dtoList = listOf(
            DashboardItemDto("1", "Title1", "Description1", "https://example.com/image1.jpg", 1000L),
            DashboardItemDto("2", "Title2", "Description2", "https://example.com/image2.jpg", 2000L)
        )

        coEvery { apiService.getDashboardItems() } returns dtoList

        repository.getDashboardItems().test {
            val loadingResult = awaitItem()
            assertTrue(loadingResult is ApiResult.Loading)

            val successResult = awaitItem()
            assertTrue(successResult is ApiResult.Success)
            val items = (successResult as ApiResult.Success).data
            assertEquals(2, items.size)
            assertEquals("Title1", items[0].title)
            assertEquals("Title2", items[1].title)
        }
    }

    @Test
    fun `getDashboardItems should emit loading then error on exception`() = runTest {
        val exception = Exception("Network error")

        coEvery { apiService.getDashboardItems() } throws exception

        repository.getDashboardItems().test {
            val loadingResult = awaitItem()
            assertTrue(loadingResult is ApiResult.Loading)

            val errorResult = awaitItem()
            assertTrue(errorResult is ApiResult.Error)
            assertEquals("Network error", (errorResult as ApiResult.Error).exception.message)
        }
    }

    @Test
    fun `getDashboardItems should handle empty list`() = runTest {
        coEvery { apiService.getDashboardItems() } returns emptyList()

        repository.getDashboardItems().test {
            val loadingResult = awaitItem()
            assertTrue(loadingResult is ApiResult.Loading)

            val successResult = awaitItem()
            assertTrue(successResult is ApiResult.Success)
            assertTrue((successResult as ApiResult.Success).data.isEmpty())
        }
    }

    @Test
    fun `getDashboardItemById should emit loading then success`() = runTest {
        val dto = DashboardItemDto("1", "Title1", "Description1", "https://example.com/image1.jpg", 1000L)

        coEvery { apiService.getDashboardItemById("1") } returns dto

        repository.getDashboardItemById("1").test {
            val loadingResult = awaitItem()
            assertTrue(loadingResult is ApiResult.Loading)

            val successResult = awaitItem()
            assertTrue(successResult is ApiResult.Success)
            val item = (successResult as ApiResult.Success).data
            assertEquals("1", item.id)
            assertEquals("Title1", item.title)
            assertEquals("Description1", item.description)
        }
    }

    @Test
    fun `getDashboardItemById should emit loading then error on exception`() = runTest {
        val exception = Exception("Not found")

        coEvery { apiService.getDashboardItemById("1") } throws exception

        repository.getDashboardItemById("1").test {
            val loadingResult = awaitItem()
            assertTrue(loadingResult is ApiResult.Loading)

            val errorResult = awaitItem()
            assertTrue(errorResult is ApiResult.Error)
            assertEquals("Not found", (errorResult as ApiResult.Error).exception.message)
        }
    }

    @Test
    fun `getDashboardItems should map DTOs to domain models correctly`() = runTest {
        val dto = DashboardItemDto(
            id = "123",
            title = "Test Title",
            description = "Test Description",
            imageUrl = "https://example.com/image.jpg",
            timestamp = 1234567890L
        )

        coEvery { apiService.getDashboardItems() } returns listOf(dto)

        val result = repository.getDashboardItems().first()
        assertTrue(result is ApiResult.Success)
        val item = (result as ApiResult.Success).data.first()

        assertEquals("123", item.id)
        assertEquals("Test Title", item.title)
        assertEquals("Test Description", item.description)
        assertEquals("https://example.com/image.jpg", item.imageUrl)
        assertEquals(1234567890L, item.timestamp)
    }

    @Test
    fun `getDashboardItemById should map DTO to domain model correctly`() = runTest {
        val dto = DashboardItemDto(
            id = "456",
            title = "Single Item",
            description = "Single Description",
            imageUrl = null,
            timestamp = 9876543210L
        )

        coEvery { apiService.getDashboardItemById("456") } returns dto

        val result = repository.getDashboardItemById("456").first()
        assertTrue(result is ApiResult.Success)
        val item = (result as ApiResult.Success).data

        assertEquals("456", item.id)
        assertEquals("Single Item", item.title)
        assertEquals("Single Description", item.description)
        assertEquals(null, item.imageUrl)
        assertEquals(9876543210L, item.timestamp)
    }
}
