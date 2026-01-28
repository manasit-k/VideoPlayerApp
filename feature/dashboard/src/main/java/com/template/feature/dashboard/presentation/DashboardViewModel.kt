package com.template.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.template.core.navigation.AppNavigator
import com.template.core.navigation.Route
import com.template.core.network.model.ApiResult
import com.template.feature.dashboard.domain.usecase.GetDashboardItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dashboard Screen
 * Uses Hilt for dependency injection
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardItemsUseCase: GetDashboardItemsUseCase,
    private val navigator: AppNavigator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardItems()
    }
    
    fun loadDashboardItems() {
        viewModelScope.launch {
            getDashboardItemsUseCase().collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is ApiResult.Success -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                items = result.data,
                                error = null
                            ) 
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false, 
                                error = result.exception.message ?: "Unknown error"
                            ) 
                        }
                    }
                }
            }
        }
    }
    
    fun onItemClick(itemId: String) {
        navigator.navigateTo(Route.DashboardDetail(itemId))
    }
    
    fun onRetry() {
        loadDashboardItems()
    }
}
