package com.doquynhhuong.project.data.repository

import com.doquynhhuong.project.models.CartItem
import com.doquynhhuong.project.models.MenuItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Cart backed by Firestore.
 * Structure: carts/{userEmail}/items/{itemName}
 *
 * CRUD operations:
 *  - CREATE / UPDATE : addOrIncrement  → sets or merges the item document
 *  - READ            : cartItemsForUser → real-time Flow via snapshot listener
 *  - UPDATE          : decrement       → decreases quantity or deletes the doc
 *  - DELETE          : removeAll / clear
 */
class CartRepository {

    private val db = FirebaseFirestore.getInstance()

    // ── READ ─────────────────────────────────────────────────────────────────

    /** Real-time stream of cart items for [ownerEmail]. */
    fun cartItemsForUser(ownerEmail: String): Flow<List<CartItem>> = callbackFlow {
        val ref = db.collection("carts")
            .document(ownerEmail.trim().lowercase())
            .collection("items")

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    CartItem(
                        menuItem = MenuItem(
                            name         = doc.getString("name") ?: return@mapNotNull null,
                            price        = doc.getString("price") ?: "",
                            description  = doc.getString("description") ?: "",
                            emoji        = doc.getString("emoji") ?: "🍽️",
                            thumbnailUrl = doc.getString("thumbnailUrl") ?: "",
                            vendorName   = doc.getString("vendorName") ?: ""
                        ),
                        quantity = (doc.getLong("quantity") ?: 1L).toInt()
                    )
                } catch (e: Exception) { null }
            }
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    // ── CREATE / UPDATE ───────────────────────────────────────────────────────

    /** Adds item to cart or increments its quantity if it already exists. */
    suspend fun addOrIncrement(item: MenuItem, vendorName: String, ownerEmail: String) {
        val docRef = cartDoc(ownerEmail, item.name)
        // Use set-with-merge so the base fields are written on first add,
        // then FieldValue.increment atomically bumps quantity — no get() round-trip needed.
        val data = hashMapOf<String, Any>(
            "name"         to item.name,
            "price"        to item.price,
            "description"  to item.description,
            "emoji"        to item.emoji,
            "thumbnailUrl" to item.thumbnailUrl,
            "vendorName"   to vendorName,
            "quantity"     to FieldValue.increment(1)
        )
        docRef.set(data, SetOptions.merge()).await()
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** Decrements quantity; removes the item document when it reaches zero. */
    suspend fun decrement(item: MenuItem, ownerEmail: String) {
        val docRef = cartDoc(ownerEmail, item.name)
        val snapshot = docRef.get().await()
        val current = (snapshot.getLong("quantity") ?: 1L).toInt()
        if (current > 1) {
            docRef.update("quantity", current - 1).await()
        } else {
            docRef.delete().await()
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /** Removes a single item from the cart entirely. */
    suspend fun removeAll(item: MenuItem, ownerEmail: String) {
        cartDoc(ownerEmail, item.name).delete().await()
    }

    /** Deletes every item in the cart for [ownerEmail]. */
    suspend fun clear(ownerEmail: String) = coroutineScope {
        val items = db.collection("carts")
            .document(ownerEmail.trim().lowercase())
            .collection("items")
            .get().await()
        items.documents.map { async { it.reference.delete().await() } }.awaitAll()
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun cartDoc(ownerEmail: String, itemName: String) =
        db.collection("carts")
            .document(ownerEmail.trim().lowercase())
            .collection("items")
            .document(itemName)
}
