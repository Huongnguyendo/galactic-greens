package com.doquynhhuong.project.models

/** Generic sealed UI-state used by every network-driven screen. */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

/** Sealed state specifically for the vendor menu (keeps backward-compat name). */
sealed class MenuState {
    object Loading : MenuState()
    data class Success(val items: List<com.doquynhhuong.project.models.MenuItem>) : MenuState()
    data class Error(val message: String) : MenuState()
}
