package com.doquynhhuong.project.data.repository

import com.doquynhhuong.project.models.MenuItem
import com.doquynhhuong.project.models.Vendor
import com.doquynhhuong.project.models.categoryEmoji
import com.doquynhhuong.project.models.generatePrice
import com.doquynhhuong.project.models.vendorNameForCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/**
 * Syncs vendor (restaurant) and menu-item data to Firestore so it persists
 * across sessions and is available offline.
 *
 * Firestore structure:
 *   vendors/{restaurantId}                      — one doc per generated restaurant
 *   vendors/{restaurantId}/menuItems/{mealName} — its group of up to ten meals
 *
 * Strategy: write-through cache.
 *   1. fetchVendors() tries Firestore first; falls back to the API on miss/error.
 *   2. loadMenuForVendor() does the same for menu items.
 *   3. After every successful API fetch the results are written to Firestore
 *      so subsequent loads are instant and offline-safe.
 */
class VendorRepository {

    private val db = FirebaseFirestore.getInstance()
    private val vendorsCollection = db.collection("vendors")

    // ── Vendors ───────────────────────────────────────────────────────────────

    /** Returns cached vendors from Firestore, or null if the cache is empty. */
    suspend fun getCachedVendors(): List<Vendor>? {
        return try {
            val snapshot = vendorsCollection.get().await()
            if (snapshot.isEmpty) return null
            snapshot.documents.mapNotNull { doc ->
                try {
                    val category = doc.getString("category") ?: ""
                    // Fall back to the deterministic generator for older cached
                    // docs written before lat/lng existed.
                    val (fallbackLat, fallbackLng) = com.doquynhhuong.project.models.vendorLatLngForCategory(category)
                    Vendor(
                        name         = doc.getString("name") ?: return@mapNotNull null,
                        displayName  = doc.getString("displayName") ?: "",
                        status       = doc.getString("status") ?: "",
                        statusColor  = androidx.compose.ui.graphics.Color(
                            (doc.getLong("statusColorArgb") ?: 0xFF4CAF50L).toInt()
                        ),
                        category     = category,
                        emoji        = doc.getString("emoji") ?: "🍽️",
                        rating       = (doc.getDouble("rating") ?: 4.5).toFloat(),
                        thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                        latitude     = doc.getDouble("latitude") ?: fallbackLat,
                        longitude    = doc.getDouble("longitude") ?: fallbackLng
                    )
                } catch (_: Exception) { null }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    /**
     * Writes a list of vendors to Firestore (upsert by stable restaurant ID).
     * Runs all writes in parallel for speed.
     */
    suspend fun saveVendors(vendors: List<Vendor>) = coroutineScope {
        vendors.map { vendor ->
            async {
                try {
                    val data = hashMapOf(
                        "name"           to vendor.name,
                        "displayName"    to vendor.displayName,
                        "status"         to vendor.status,
                        "statusColorArgb" to vendor.statusColor.value.toLong(),
                        "category"       to vendor.category,
                        "emoji"          to vendor.emoji,
                        "rating"         to vendor.rating.toDouble(),
                        "thumbnailUrl"   to vendor.thumbnailUrl,
                        "latitude"       to vendor.latitude,
                        "longitude"      to vendor.longitude
                    )
                    vendorsCollection.document(vendor.name).set(data).await()
                } catch (_: Exception) { /* best-effort */ }
            }
        }.awaitAll()
    }

    // ── Menu items ────────────────────────────────────────────────────────────

    /** Returns cached menu items for [vendorCategory] from Firestore, or null on miss. */
    suspend fun getCachedMenuItems(vendorCategory: String): List<MenuItem>? {
        return try {
            val snapshot = vendorsCollection
                .document(vendorCategory)
                .collection("menuItems")
                .get().await()
            if (snapshot.isEmpty) return null
            snapshot.documents.mapNotNull { doc ->
                try {
                    MenuItem(
                        name         = doc.getString("name") ?: return@mapNotNull null,
                        price        = doc.getString("price") ?: generatePrice(doc.id),
                        description  = doc.getString("description") ?: "",
                        emoji        = doc.getString("emoji") ?: categoryEmoji(vendorCategory),
                        thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                        vendorName   = doc.getString("vendorName")
                            ?: vendorNameForCategory(vendorCategory)
                    )
                } catch (_: Exception) { null }
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    /**
     * Writes menu items for [vendorCategory] to Firestore (upsert by name).
     * Runs all writes in parallel.
     */
    suspend fun saveMenuItems(vendorCategory: String, items: List<MenuItem>) = coroutineScope {
        val sub = vendorsCollection.document(vendorCategory).collection("menuItems")
        items.map { item ->
            async {
                try {
                    val data = hashMapOf(
                        "name"         to item.name,
                        "price"        to item.price,
                        "description"  to item.description,
                        "emoji"        to item.emoji,
                        "thumbnailUrl" to item.thumbnailUrl,
                        "vendorName"   to item.vendorName
                    )
                    // Use a sanitised name as document ID
                    val docId = item.name.replace("/", "_").take(100)
                    sub.document(docId).set(data).await()
                } catch (_: Exception) { /* best-effort */ }
            }
        }.awaitAll()
    }
}
