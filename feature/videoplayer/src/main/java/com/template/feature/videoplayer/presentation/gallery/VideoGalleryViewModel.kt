package com.template.feature.videoplayer.presentation.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.template.core.network.model.ApiResult
import com.template.feature.videoplayer.domain.VideoItem
import com.template.feature.videoplayer.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class ViewMode {
    LIST, GRID
}

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_NEWEST,
    DATE_OLDEST,
    SIZE_LARGEST,
    SIZE_SMALLEST,
    DURATION_LONGEST,
    DURATION_SHORTEST
}

data class VideoGalleryUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val folders: Map<String, List<VideoItem>> = emptyMap(),
    val error: String? = null,
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.DATE_NEWEST,
    val showSortMenu: Boolean = false
)

@HiltViewModel
class VideoGalleryViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoGalleryUiState())
    val uiState: StateFlow<VideoGalleryUiState> = _uiState.asStateFlow()

    private var rawVideos: List<VideoItem> = emptyList()

    fun loadVideos() {
        viewModelScope.launch {
            repository.getAllVideos().collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is ApiResult.Success -> {
                        rawVideos = result.data
                        applySortingAndGrouping()
                        Timber.d("Loaded ${rawVideos.size} videos")
                    }
                    is ApiResult.Error -> {
                        val errorMessage = result.exception.message ?: "Unknown error"
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = errorMessage
                            )
                        }
                        Timber.e(result.exception, "Failed to load videos")
                    }
                }
            }
        }
    }

    fun toggleViewMode() {
        _uiState.update {
            val newMode = if (it.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
            Timber.d("View mode changed to: $newMode")
            it.copy(viewMode = newMode)
        }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option, showSortMenu = false) }
        applySortingAndGrouping()
        Timber.d("Sort option changed to: $option")
    }

    fun toggleSortMenu() {
        _uiState.update { it.copy(showSortMenu = !it.showSortMenu) }
    }

    fun dismissSortMenu() {
        _uiState.update { it.copy(showSortMenu = false) }
    }

    private fun applySortingAndGrouping() {
        val sortedVideos = when (_uiState.value.sortOption) {
            SortOption.NAME_ASC -> rawVideos.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> rawVideos.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> rawVideos.sortedByDescending { it.dateModified }
            SortOption.DATE_OLDEST -> rawVideos.sortedBy { it.dateModified }
            SortOption.SIZE_LARGEST -> rawVideos.sortedByDescending { it.size }
            SortOption.SIZE_SMALLEST -> rawVideos.sortedBy { it.size }
            SortOption.DURATION_LONGEST -> rawVideos.sortedByDescending { it.duration }
            SortOption.DURATION_SHORTEST -> rawVideos.sortedBy { it.duration }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                videos = sortedVideos,
                folders = sortedVideos.groupBy { video -> video.folderName },
                error = null
            )
        }
    }

    fun retry() {
        loadVideos()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
