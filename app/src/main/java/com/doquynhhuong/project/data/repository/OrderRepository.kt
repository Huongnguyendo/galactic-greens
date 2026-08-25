package com.doquynhhuong.project.data.repository

import com.doquynhhuong.project.models.MenuItem
import com.doquynhhuong.project.models.Order
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class OrderRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Orders now live as a SUBCOLLECTION of the owning user's document:
     *   users/{email}/orders/{orderId}
     *
     * This means each user's orders are physically isolated in Firestore —
     * there is no longer a flat top-level "orders" collection that mixes
     * everyone's data together. A user can only ever read/create/update documents
     * under their own users/{email} path (enforce this with Firestore
     * security rules too — see note at the bottom of this file).
     */
    private fun ordersCollection(ownerEmail: String) =
        db.collection("users")
            .document(ownerEmail.trim().lowercase())
            .collection("orders")

    // ── READ ─────────────────────────────────────────────────────────────────

    /**
     * Real-time stream of orders for [ownerEmail], newest first.
     *
     * Reads from users/{ownerEmail}/orders. Sorting is done client-side so we
     * don't need a composite index (same reasoning as before — this collection
     * is also naturally small per-user, so client-side sort is cheap).
     */
    fun ordersForUser(ownerEmail: String): Flow<List<Order>> = callbackFlow {
        if (ownerEmail.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val ref = ordersCollection(ownerEmail)

        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log but don't crash — emit empty so the UI can recover
                android.util.Log.e("OrderRepository", "Firestore error: ${error.message}", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val orders = snapshot.documents
                .mapNotNull { doc ->
                    try {
                        Order(
                            items                = emptyList(),
                            vendorName           = doc.getString("vendorName") ?: "",
                            timestamp            = doc.getLong("timestamp") ?: 0L,
                            totalPrice           = doc.getString("totalPrice") ?: "0.00",
                            isDelivery           = doc.getBoolean("isDelivery") ?: false,
                            dropOffLocation      = doc.getString("dropOffLocation") ?: "",
                            ownerEmail           = doc.getString("ownerEmail") ?: ownerEmail.trim().lowercase(),
                            estimatedPrepMinutes = (doc.getLong("estimatedPrepMinutes") ?: 0L).toInt(),
                            firestoreId          = doc.id,
                            itemsSummary         = doc.getString("itemsSummary") ?: "",
                            photoUrls            = (doc.get("photoUrls") as? List<*>)
                                ?.filterIsInstance<String>()
                                .orEmpty()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("OrderRepository", "Failed to parse order doc ${doc.id}", e)
                        null
                    }
                }
                // Sort newest-first client-side (avoids composite index requirement)
                .sortedByDescending { it.timestamp }

            trySend(orders)
        }
        awaitClose { listener.remove() }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /** Saves a new order document under users/{ownerEmail}/orders and returns its document ID. */
    suspend fun saveOrder(
        items: List<MenuItem>,
        vendorName: String,
        isDelivery: Boolean = false,
        dropOffLocation: String = "",
        ownerEmail: String = "",
        estimatedPrepMinutes: Int = 0
    ): String {
        require(ownerEmail.isNotBlank()) { "ownerEmail is required to save an order under users/{email}/orders" }

        val summary = items.groupBy { it.name }
            .entries
            .joinToString(", ") { (name, list) -> "$name ×${list.size}" }
        val total = items.sumOf { it.price.replace("$", "").toDoubleOrNull() ?: 0.0 }

        val data = hashMapOf(
            "vendorName"           to vendorName,
            "itemsSummary"         to summary,
            "totalPrice"           to "%.2f".format(total),
            "timestamp"            to System.currentTimeMillis(),
            "isDelivery"           to isDelivery,
            "dropOffLocation"      to dropOffLocation,
            "ownerEmail"           to ownerEmail.trim().lowercase(),
            "estimatedPrepMinutes" to estimatedPrepMinutes.coerceIn(0, 120)
        )
        val ref = ordersCollection(ownerEmail).add(data).await()
        return ref.id
    }

}

/*
 * Suggested Firestore security rule to enforce this server-side as well
 * (Console → Firestore Database → Rules):
 *
 * rules_version = '2';
 * service cloud.firestore {
 *   match /databases/{database}/documents {
 *     match /users/{userEmail}/orders/{orderId} {
 *       allow read, create, update: if request.auth != null
 *         && request.auth.token.email.lower() == userEmail;
 *       allow delete: if false;
 *     }
 *   }
 * }
 *
 * The app now uses Firebase email/password Authentication, so request.auth and
 * its email claim are available to enforce this ownership rule.
 */
