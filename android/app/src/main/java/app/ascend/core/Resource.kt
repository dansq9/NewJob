package app.ascend.core

/** Minimal result wrapper for async data loads surfaced to the UI. */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val rateLimited: Boolean = false,
    ) : Resource<Nothing>
}
