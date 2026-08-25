package com.doquynhhuong.project.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.doquynhhuong.project.data.MealRepository
import com.doquynhhuong.project.data.UserPreferences
import com.doquynhhuong.project.models.UiState
import com.doquynhhuong.project.network.MealCategory
import com.doquynhhuong.project.network.MealDetail
import com.doquynhhuong.project.network.MealSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for FoodBrowseScreen, MealListScreen, and FoodDetailScreen.
 * Uses AndroidViewModel to access Application context for DataStore.
 */
class FoodBrowseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MealRepository()
    private val prefs      = UserPreferences(application)

    // ── Persisted preferences ────────────────────────────────────────────
    val dietaryFilter: StateFlow<String> = prefs.dietaryFilter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "All")

    val sortAZ: StateFlow<Boolean> = prefs.sortAZ
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastCategory: StateFlow<String> = prefs.lastCategory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // ── Category screen ───────────────────────────────────────────────────
    private val _categoriesState = MutableStateFlow<UiState<List<MealCategory>>>(UiState.Loading)
    val categoriesState: StateFlow<UiState<List<MealCategory>>> = _categoriesState.asStateFlow()

    // ── Meal list screen ──────────────────────────────────────────────────
    private val _mealsState = MutableStateFlow<UiState<List<MealSummary>>>(UiState.Loading)
    val mealsState: StateFlow<UiState<List<MealSummary>>> = _mealsState.asStateFlow()

    private var categoryMeals: List<MealSummary> = emptyList()
    private var rawMeals: List<MealSummary> = emptyList()

    // ── Detail screen ─────────────────────────────────────────────────────
    private val _detailState = MutableStateFlow<UiState<MealDetail?>>(UiState.Loading)
    val detailState: StateFlow<UiState<MealDetail?>> = _detailState.asStateFlow()

    // ── Search ────────────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    init { loadCategories() }

    // ── API calls ─────────────────────────────────────────────────────────

    fun loadCategories() {
        viewModelScope.launch {
            _categoriesState.value = UiState.Loading
            repository.getCategories()
                .onSuccess { _categoriesState.value = UiState.Success(it) }
                .onFailure { _categoriesState.value = UiState.Error(it.message ?: "Failed to load categories") }
        }
    }

    fun loadMealsByCategory(category: String) {
        viewModelScope.launch {
            _selectedCategory.value = category
            _mealsState.value = UiState.Loading
            _searchQuery.value = ""
            prefs.setLastCategory(category)
            repository.getMealsByCategory(category)
                .onSuccess { meals ->
                    categoryMeals = meals
                    rawMeals = meals
                    applyFiltersAndSort()
                }
                .onFailure { _mealsState.value = UiState.Error(it.message ?: "Failed to load meals") }
        }
    }

    fun loadMealDetail(mealId: String) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            repository.getMealDetail(mealId)
                .onSuccess { _detailState.value = UiState.Success(it) }
                .onFailure { _detailState.value = UiState.Error(it.message ?: "Failed to load detail") }
        }
    }

    fun searchMeals(query: String) {
        _searchQuery.value = query
        rawMeals = if (query.isBlank()) {
            categoryMeals
        } else {
            categoryMeals.filter { it.name.contains(query, ignoreCase = true) }
        }
        applyFiltersAndSort()
    }

    // ── Preference updates ────────────────────────────────────────────────

    fun setDietaryFilter(filter: String) {
        viewModelScope.launch { prefs.setDietaryFilter(filter) }
        if (filter == "All") {
            rawMeals = categoryMeals
            applyFiltersAndSort()
        } else if (MealRepository.isMeatFreeCategory(filter)) {
            loadMealsByCategory(filter)
        }
    }

    fun setSortAZ(enabled: Boolean) {
        viewModelScope.launch { prefs.setSortAZ(enabled); applyFiltersAndSort() }
    }

    // ── Client-side filter + sort ─────────────────────────────────────────

    private fun applyFiltersAndSort() {
        var result = rawMeals
        if (sortAZ.value) result = result.sortedBy { it.name }
        _mealsState.value = UiState.Success(result)
    }
}
