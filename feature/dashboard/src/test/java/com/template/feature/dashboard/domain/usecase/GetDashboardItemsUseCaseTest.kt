package com.template.feature.dashboard.domain.usecase

import app.cash.turbine.test
import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.domain.model.DashboardItem
import com.template.feature.dashboard.domain.repository.DashboardRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetDashboardItemsUseCaseTest {

    private lateinit var repository: DashboardRepository
    private lateinit var useCase: GetDashboardItemsUseCase

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        useCase = GetDashboardItemsUseCase(repository)
    }

    @Test
    fun `invoke should return success result from repository`() = runTest {
        val items = listOf(
            DashboardItem("1", "Title1", "Description1", "https://example.com/image1.jpg", 1000L),
            DashboardItem("2", "Title2", "Description2", "https://example.com/image2.jpg", 2000L)
        )

        coEvery { repository.getDashboardItems() } returns flowOf(
            ApiResult.Success(items)
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result is ApiResult.Success)
            assertEquals(2, (result as ApiResult.Success).data.size)
            assertEquals("Title1", result.data[0].title)
            assertEquals("Title2", result.data[1].title)
        }
    }

    @Test
    fun `invoke should return loading result from repository`() = runTest {
        coEvery { repository.getDashboardItems() } returns flowOf(
            ApiResult.Loading
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result is ApiResult.Loading)
        }
    }

    @Test
    fun `invoke should return error result from repository`() = runTest {
        val exception = Exception("Network error")

        coEvery { repository.getDashboardItems() } returns flowOf(
            ApiResult.Error(exception)
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result is ApiResult.Error)
            assertEquals("Network error", (result as ApiResult.Error).exception.message)
        }
    }

    @Test
    fun `invoke should return empty list when repository returns empty`() = runTest {
        coEvery { repository.getDashboardItems() } returns flowOf(
            ApiResult.Success(emptyList())
        )

        useCase().test {
            val result = awaitItem()
            assertTrue(result is ApiResult.Success)
            assertTrue((result as ApiResult.Success).data.isEmpty())
        }
    }

    @Test
    fun `invoke should delegate to repository getDashboardItems`() = runTest {
        val items = listOf(
            DashboardItem("1", "Title1", "Description1", null, 1000L)
        )

        coEvery { repository.getDashboardItems() } returns flowOf(
            ApiResult.Success(items)
        )

        useCase().test {
            awaitItem()
        }

        coEvery { repository.getDashboardItems() }
    }
}
