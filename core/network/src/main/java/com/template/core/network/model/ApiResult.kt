package com.template.core.network.model

/**
 * Generic API response wrapper
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

/**
 * Extension function to map ApiResult
 */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Error -> ApiResult.Error(exception)
        is ApiResult.Loading -> ApiResult.Loading
    }
}
