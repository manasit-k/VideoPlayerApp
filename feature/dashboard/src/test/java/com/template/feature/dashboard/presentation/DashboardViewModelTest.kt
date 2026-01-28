package com.template.feature.dashboard.presentation

import app.cash.turbine.test
import com.template.core.navigation.AppNavigator
import com.template.core.navigation.Route
import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.domain.model.DashboardItem
import com.template.feature.dashboard.domain.usecase.GetDashboardItemsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DashboardViewModelTest {

    private lateinit var getDashboardItemsUseCase: GetDashboardItemsUseCase
    private lateinit var navigator: AppNavigator
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        getDashboardItemsUseCase = mockk(relaxed = true)
        navigator = mockk(relaxed = true)
        viewModel = DashboardViewModel(getDashboardItemsUseCase, navigator)
    }

    @Test
    fun `initial state should have default values`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.items.isEmpty())
            assertNull(state.error)
        }
    }

    @Test
    fun `loadDashboardItems should emit loading then success`() = runTest {
        val items = listOf(
            DashboardItem("1", "Title1", "Description1", "https://example.com/image1.jpg", 1000L),
            DashboardItem("2", "Title2", "Description2", "https://example.com/image2.jpg", 2000L)
        )

        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success(items)
        )

        viewModel.uiState.test {
            viewModel.loadDashboardItems()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.error)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertEquals(2, successState.items.size)
            assertEquals("Title1", successState.items[0].title)
            assertEquals("Title2", successState.items[1].title)
            assertNull(successState.error)
        }
    }

    @Test
    fun `loadDashboardItems should handle error`() = runTest {
        val exception = Exception("Network error")

        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            viewModel.loadDashboardItems()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertEquals("Network error", errorState.error)
        }
    }

    @Test
    fun `loadDashboardItems should handle error with null message`() = runTest {
        val exception = Exception()

        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Error(exception)
        )

        viewModel.uiState.test {
            viewModel.loadDashboardItems()

            skipItems(1) // Skip loading state

            val errorState = awaitItem()
            assertEquals("Unknown error", errorState.error)
        }
    }

    @Test
    fun `onItemClick should navigate to dashboard detail`() = runTest {
        val itemId = "123"

        viewModel.onItemClick(itemId)

        verify { navigator.navigateTo(Route.DashboardDetail(itemId)) }
    }

    @Test
    fun `onRetry should reload dashboard items`() = runTest {
        val items = listOf(
            DashboardItem("1", "Title1", "Description1", null, 1000L)
        )

        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Success(items)
        )

        viewModel.onRetry()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.items.size)
        }

        verify(exactly = 2) { getDashboardItemsUseCase() } // Once in init, once in onRetry
    }

    @Test
    fun `init should automatically load dashboard items`() = runTest {
        val items = listOf(
            DashboardItem("1", "Title1", "Description1", null, 1000L)
        )

        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Success(items)
        )

        // Create new viewModel to trigger init
        val newViewModel = DashboardViewModel(getDashboardItemsUseCase, navigator)

        newViewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.items.size)
        }

        verify { getDashboardItemsUseCase() }
    }

    @Test
    fun `loadDashboardItems should handle empty list`() = runTest {
        coEvery { getDashboardItemsUseCase() } returns flowOf(
            ApiResult.Loading,
            ApiResult.Success(emptyList())
        )

        viewModel.uiState.test {
            viewModel.loadDashboardItems()

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertFalse(successState.isLoading)
            assertTrue(successState.items.isEmpty())
            assertNull(successState.error)
        }
    }

    @Test
    fun `multiple loadDashboardItems calls should update state correctly`() = runTest {
        val items1 = listOf(
            DashboardItem("1", "Title1", "Description1", null, 1000L)
        )
        val items2 = listOf(
            DashboardItem("1", "Title1", "Description1", null, 1000L),
            DashboardItem("2", "Title2", "Description2", null, 2000L)
        )

        coEvery { getDashboardItemsUseCase() } returnsMany listOf(
            flowOf(ApiResult.Success(items1)),
            flowOf(ApiResult.Success(items2))
        )

        viewModel.loadDashboardItems()
        viewModel.uiState.test {
            val state1 = awaitItem()
            assertEquals(1, state1.items.size)
        }

        viewModel.loadDashboardItems()
        viewModel.uiState.test {
            val state2 = awaitItem()
            assertEquals(2, state2.items.size)
        }
    }
}
