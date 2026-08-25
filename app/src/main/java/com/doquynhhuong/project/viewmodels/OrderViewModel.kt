package com.doquynhhuong.project.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.doquynhhuong.project.domain.checkout.VendorPrepEstimator
import com.doquynhhuong.project.data.DeliveryPhotoUploader
import com.doquynhhuong.project.data.UserPreferences
import com.doquynhhuong.project.data.repository.CartRepository
import com.doquynhhuong.project.data.repository.OrderRepository
import com.doquynhhuong.project.data.repository.RewardsRepository
import com.doquynhhuong.project.data.repository.RewardsProfile
import com.doquynhhuong.project.data.repository.VendorRepository
import com.doquynhhuong.project.data.MealRepository
import com.doquynhhuong.project.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class OrderViewModel(application: Application) : AndroidViewModel(application) {

    private val cartRepo        = CartRepository()
    private val orderRepo       = OrderRepository()
    private val rewardsRepo     = RewardsRepository()
    private val vendorRepo      = VendorRepository()
    private val mealRepository  = MealRepository()
    private val userPreferences = UserPreferences(application)

    val sessionDisplayName: StateFlow<String?> = userPreferences.sessionDisplayName
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val sessionEmail: StateFlow<String?> = userPreferences.sessionEmail
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── Vendors ───────────────────────────────────────────────────────────────

    private val _vendorsState = MutableStateFlow<UiState<List<Vendor>>>(UiState.Loading)
    val vendorsState: StateFlow<UiState<List<Vendor>>> = _vendorsState.asStateFlow()

    init { fetchVendors() }

    fun fetchVendors() {
        viewModelScope.launch {
            _vendorsState.value = UiState.Loading
            val cached = vendorRepo.getCachedVendors()
            val seededRestaurants = cached.orEmpty().filter {
                MealRepository.isMeatFreeCategory(it.category) && "-station-v4-" in it.name
            }
            val cachedCategories = seededRestaurants.map { it.category.lowercase() }.toSet()
            val expectedCategories = MealRepository.supportedMeatFreeCategories()
            if (cachedCategories.containsAll(expectedCategories)) {
                _vendorsState.value = UiState.Success(seededRestaurants)
                return@launch
            }
            mealRepository.getCategories()
                .onSuccess { categories ->
                    val missingCategories = categories.filter {
                        it.name.lowercase() !in cachedCategories
                    }
                    val newlySeeded = seedRestaurants(missingCategories)
                    _vendorsState.value = UiState.Success(
                        (seededRestaurants + newlySeeded).distinctBy { it.name }
                    )
                }
                .onFailure {
                    _vendorsState.value = if (seededRestaurants.isNotEmpty()) {
                        UiState.Success(seededRestaurants)
                    } else {
                        UiState.Error(it.message ?: "Failed to load vendors")
                    }
                }
        }
    }

    private suspend fun seedRestaurants(
        categories: List<com.doquynhhuong.project.network.MealCategory>
    ): List<Vendor> {
        val restaurants = mutableListOf<Vendor>()
        categories.forEach { category ->
            mealRepository.getMealsByCategory(category.name).onSuccess { meals ->
                val menuGroups = if (category.name.equals("vegan", ignoreCase = true) && meals.isNotEmpty()) {
                    // TheMealDB currently has a very small Vegan category. Rotate those
                    // verified vegan meals so all five themed restaurants remain usable.
                    List(5) { page ->
                        val offset = page % meals.size
                        (meals.drop(offset) + meals.take(offset)).take(10)
                    }
                } else {
                    meals.chunked(10).take(5)
                }

                menuGroups.forEachIndexed { page, group ->
                    val restaurantId = "${category.name.lowercase()}-station-v4-${page + 1}"
                    val displayName = vendorNameForCategoryPage(category.name, page)
                    val (lat, lng) = vendorLatLngForCategory(restaurantId)
                    val vendor = Vendor(
                        name = restaurantId,
                        displayName = displayName,
                        status = statusForCategory(category.name),
                        statusColor = statusColorForCategory(category.name),
                        category = category.name,
                        emoji = categoryEmoji(category.name),
                        rating = ratingForCategory(restaurantId),
                        thumbnailUrl = group.firstOrNull()?.thumbnail ?: category.thumbnail,
                        latitude = lat,
                        longitude = lng
                    )
                    val menu = group.map { meal ->
                        MenuItem(
                            name = meal.name,
                            price = generatePrice(meal.name),
                            description = category.name,
                            emoji = categoryEmoji(category.name),
                            thumbnailUrl = meal.thumbnail,
                            vendorName = displayName
                        )
                    }
                    restaurants += vendor
                    vendorRepo.saveVendors(listOf(vendor))
                    vendorRepo.saveMenuItems(restaurantId, menu)
                }
            }
        }
        return restaurants
    }

    // ── Menu ──────────────────────────────────────────────────────────────────

    private val _menuState = MutableStateFlow<MenuState>(MenuState.Loading)
    val menuState: StateFlow<MenuState> = _menuState.asStateFlow()

    fun loadMenuForVendor(vendorName: String) {
        viewModelScope.launch {
            _menuState.value = MenuState.Loading
            val cached = vendorRepo.getCachedMenuItems(vendorName)
            if (cached != null) {
                _menuState.value = MenuState.Success(cached)
                return@launch
            }
            val vendor = when (val state = _vendorsState.value) {
                is UiState.Success -> state.data.firstOrNull { it.name == vendorName }
                else -> null
            }
            if (vendor == null) {
                _menuState.value = MenuState.Error("Restaurant menu is not available")
                return@launch
            }
            val page = vendorName.substringAfterLast('-').toIntOrNull()?.minus(1) ?: 0
            mealRepository.getMealsByCategory(vendor.category)
                .onSuccess { meals ->
                    val lineVendor = _currentVendor.value.ifBlank { vendor.displayName }
                    val pageMeals = meals.chunked(10).getOrNull(page).orEmpty()
                    val items = pageMeals.map { meal ->
                        MenuItem(
                            name         = meal.name,
                            price        = generatePrice(meal.name),
                            description  = vendor.category,
                            emoji        = categoryEmoji(vendor.category),
                            thumbnailUrl = meal.thumbnail,
                            vendorName   = lineVendor
                        )
                    }
                    _menuState.value = MenuState.Success(items)
                    vendorRepo.saveMenuItems(vendorName, items)
                }
                .onFailure { _menuState.value = MenuState.Error(it.message ?: "Failed to load menu") }
        }
    }

    fun retryMenuLoad(vendorName: String) = loadMenuForVendor(vendorName)

    // ── Cart ──────────────────────────────────────────────────────────────────

    val cartItems: StateFlow<List<CartItem>> = sessionEmail
        .map { it?.trim()?.lowercase().orEmpty() }
        .distinctUntilChanged()
        .flatMapLatest { email ->
            if (email.isBlank()) flowOf(emptyList())
            else cartRepo.cartItemsForUser(email)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentVendor  = MutableStateFlow("")
    val currentVendor: StateFlow<String> = _currentVendor.asStateFlow()

    private val _selectedItems  = MutableStateFlow<List<MenuItem>>(emptyList())
    val selectedItems: StateFlow<List<MenuItem>> = _selectedItems.asStateFlow()

    fun setCurrentVendor(vendor: String) { _currentVendor.value = vendor }

    fun addToCart(item: MenuItem): Boolean {
        val email = sessionEmail.value?.trim()?.lowercase()
        if (email.isNullOrBlank()) return false
        viewModelScope.launch { cartRepo.addOrIncrement(item, _currentVendor.value, email) }
        return true
    }

    fun removeFromCart(item: MenuItem) {
        val email = sessionEmail.value?.trim()?.lowercase() ?: return
        viewModelScope.launch { cartRepo.decrement(item, email) }
    }

    fun clearCart() {
        val email = sessionEmail.value?.trim()?.lowercase() ?: return
        viewModelScope.launch { cartRepo.clear(email) }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            val email = sessionEmail.value?.trim()?.lowercase()
            userPreferences.setSessionEmail(null)
            userPreferences.setSessionDisplayName(null)
            if (!email.isNullOrBlank()) cartRepo.clear(email)
        }
    }

    fun getItemQuantity(item: MenuItem): Int =
        cartItems.value.find { it.menuItem.name == item.name }?.quantity ?: 0

    fun cartTotal(): Double = cartItems.value.sumOf { ci ->
        (ci.menuItem.price.replace("$", "").toDoubleOrNull() ?: 0.0) * ci.quantity
    }

    fun cartItemCount(): Int = cartItems.value.sumOf { it.quantity }

    fun updateItemsFromCart() {
        _selectedItems.value = cartItems.value.flatMap { ci -> List(ci.quantity) { ci.menuItem } }
    }

    fun updateItems(items: List<MenuItem>) { _selectedItems.value = items }

    // ── Order history ─────────────────────────────────────────────────────────

    /**
     * True while we are loading the first snapshot.
     * Starts true, becomes false once the first Firestore result (or error) arrives,
     * or immediately when the user is logged out.
     */
    private val _isLoadingHistory = MutableStateFlow(true)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory.asStateFlow()

    val orderHistory: StateFlow<List<Order>> = sessionEmail
        .flatMapLatest { rawEmail ->
            if (rawEmail == null) {
                // DataStore hasn't loaded yet — keep spinner, emit nothing
                _isLoadingHistory.value = true
                return@flatMapLatest flowOf(emptyList())
            }
            val key = rawEmail.trim().lowercase()
            if (key.isBlank()) {
                // Logged out — clear spinner, nothing to show
                _isLoadingHistory.value = false
                flowOf(emptyList())
            } else {
                // Logged in — subscribe; spinner clears on first snapshot (including empty list)
                _isLoadingHistory.value = true
                orderRepo.ordersForUser(key)
                    .onEach {
                        // First emission (even an empty list) clears the loading flag
                        if (_isLoadingHistory.value) _isLoadingHistory.value = false
                    }
                    .catch { e ->
                        // If the flow itself throws, clear the spinner and log
                        Log.e("OrderViewModel", "orderHistory flow error", e)
                        _isLoadingHistory.value = false
                        emit(emptyList())
                    }
            }
        }
        // Use WhileSubscribed so the Firestore listener is torn down when no
        // screen is observing, preventing memory leaks.
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Rewards (points + badge) ─────────────────────────────────────────────

    /** Live points/badge for the signed-in user, read from users/{email}. */
    val rewardsProfile: StateFlow<RewardsProfile> = sessionEmail
        .flatMapLatest { rawEmail ->
            val key = rawEmail?.trim()?.lowercase().orEmpty()
            if (key.isBlank()) flowOf(RewardsProfile()) else rewardsRepo.observeProfile(key)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RewardsProfile())

    /**
     * Called after every successful delivery-photo capture. Recomputes this
     * order's points from its current photo count and syncs the new total +
     * badge to the user's profile document.
     */
    fun recordDeliveryPhotoCount(orderKey: String, photoCount: Int) {
        val email = sessionEmail.value?.trim()?.lowercase()
        if (email.isNullOrBlank() || orderKey.isBlank()) return
        viewModelScope.launch { rewardsRepo.updateOrderPoints(email, orderKey, photoCount) }
    }

    /**
     * Saves the order to Firestore, clears the cart, then calls [onComplete].
     *
     * FIX: We wait until the saved document appears in [orderHistory] (up to
     * 5 s) before navigating. This prevents the history screen from showing a
     * stale empty list on fast navigation.
     */
    fun saveOrderToHistory(
        dropOffLocation: String = "",
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val owner = sessionEmail.value?.trim()?.lowercase().orEmpty()
            if (owner.isBlank()) {
                onComplete()
                return@launch
            }

            val vendorLabel = _selectedItems.value
                .map { it.vendorName.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
                .ifBlank { _currentVendor.value }

            val prepMin = estimatedPrepMinutesForCheckout(_selectedItems.value, vendorLabel)

            // Save and capture the new document's ID
            val newDocId = try {
                orderRepo.saveOrder(
                    items                = _selectedItems.value,
                    vendorName           = vendorLabel,
                    isDelivery           = true,
                    dropOffLocation      = dropOffLocation,
                    ownerEmail           = owner,
                    estimatedPrepMinutes = prepMin
                )
            } catch (e: Exception) {
                Log.e("OrderViewModel", "saveOrder failed", e)
                _selectedItems.value = emptyList()
                cartRepo.clear(owner)
                onComplete()
                return@launch
            }

            _selectedItems.value = emptyList()
            cartRepo.clear(owner)

            // Wait up to 5 s for the real-time listener to reflect the new order,
            // then navigate regardless so the app never gets stuck.
            withTimeoutOrNull(5_000L) {
                orderHistory
                    .filter { orders -> orders.any { it.firestoreId == newDocId } }
                    .first()
            }

            onComplete()
        }
    }

    /** Upload a local delivery photo file to Firebase Storage and record the URL on the order document. */
    fun uploadDeliveryPhoto(localFile: File, ownerEmail: String, orderFirestoreId: String) {
        val owner = ownerEmail.trim().lowercase()
        if (owner.isBlank() || orderFirestoreId.isBlank()) return
        viewModelScope.launch {
            try {
                val url = DeliveryPhotoUploader.uploadPhoto(getApplication(), localFile, owner, orderFirestoreId)
                android.util.Log.i("OrderViewModel", "Uploaded delivery photo for $orderFirestoreId: $url")
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Photo uploaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("OrderViewModel", "Failed uploading delivery photo", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Photo upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun estimatedPrepMinutesForCheckout(items: List<MenuItem>, fallbackVendorLabel: String): Int {
        val kitchenMin = VendorPrepEstimator.estimateKitchenPrepMinutes(items, fallbackVendorLabel)
        return (kitchenMin + OrbitalDeliveryTiming.ORBITAL_DELIVERY_MINUTES).coerceIn(1, 120)
    }

    private fun statusForCategory(name: String): String {
        val waitMins = (name.length % 16) + 5
        val busy     = name.length % 3 == 0
        return if (busy) "Busy • $waitMins min wait" else "Open • $waitMins min wait"
    }

    private fun statusColorForCategory(name: String): Color = when {
        name.length % 3 == 0 -> Color(0xFFFF9800)
        name.length % 5 == 0 -> Color(0xFFF44336)
        else                  -> Color(0xFF4CAF50)
    }

    private fun ratingForCategory(name: String): Float = 4.0f + (name.length % 11) / 10f
}
