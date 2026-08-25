package com.doquynhhuong.project.data.repository

import android.util.Log
import com.doquynhhuong.project.domain.rewards.BadgeTier
import com.doquynhhuong.project.domain.rewards.PointsCalculator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Snapshot of a user's reward progress, read from their users/{email} document. */
data class RewardsProfile(
    val totalPoints: Int = 0,
    val badge: BadgeTier = BadgeTier.EXPLORER,
    /** orderKey -> points earned for that order (0, 10, or 20). */
    val pointsByOrder: Map<String, Int> = emptyMap()
)

/**
 * Manages the points/badge fields on a user's existing profile document
 * (users/{email} in Firestore — the same profile document UserRepository writes
 * email/displayName to). Photos themselves are never stored in Firestore; only the
 * resulting points and badge name are synced here.
 *
 * Fields added to users/{email}:
 *   pointsByOrder : map<string, number> — points earned per order
 *   totalPoints   : number              — sum of pointsByOrder
 *   badgeTier     : string              — BadgeTier enum name (e.g. "CURATOR")
 */
class RewardsRepository {

    private val db = FirebaseFirestore.getInstance()
    private fun userDoc(email: String) = db.collection("users").document(email.trim().lowercase())

    /**
     * Recomputes points for one order from its current local photo count and
     * writes the updated total + badge back to the user's profile document.
     * Uses a transaction so concurrent updates (e.g. multiple orders) don't clobber each other.
     */
    suspend fun updateOrderPoints(email: String, orderKey: String, photoCount: Int) {
        if (email.isBlank() || orderKey.isBlank()) return
        val ref = userDoc(email)
        val newPointsForOrder = PointsCalculator.pointsForPhotoCount(photoCount)

        try {
            db.runTransaction { txn ->
                val snap = txn.get(ref)
                @Suppress("UNCHECKED_CAST")
                val existing = (snap.get("pointsByOrder") as? Map<String, Number>)
                    ?.mapValues { it.value.toInt() }
                    ?: emptyMap()

                val updated = existing.toMutableMap().apply { this[orderKey] = newPointsForOrder }
                val total = updated.values.sum()
                val badge = BadgeTier.forPoints(total).name

                txn.set(
                    ref,
                    mapOf(
                        "pointsByOrder" to updated,
                        "totalPoints" to total,
                        "badgeTier" to badge
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                null
            }.await()
        } catch (e: Exception) {
            Log.e("RewardsRepository", "updateOrderPoints failed for $email/$orderKey", e)
        }
    }

    /** Removes an order's points (e.g. when the order + its photos are deleted) and recalculates the total. */
    suspend fun removeOrderPoints(email: String, orderKey: String) {
        if (email.isBlank() || orderKey.isBlank()) return
        val ref = userDoc(email)
        try {
            db.runTransaction { txn ->
                val snap = txn.get(ref)
                @Suppress("UNCHECKED_CAST")
                val existing = (snap.get("pointsByOrder") as? Map<String, Number>)
                    ?.mapValues { it.value.toInt() }
                    ?: emptyMap()
                if (!existing.containsKey(orderKey)) return@runTransaction null

                val updated = existing.toMutableMap().apply { remove(orderKey) }
                val total = updated.values.sum()
                val badge = BadgeTier.forPoints(total).name

                txn.set(
                    ref,
                    mapOf(
                        "pointsByOrder" to updated,
                        "totalPoints" to total,
                        "badgeTier" to badge
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                null
            }.await()
        } catch (e: Exception) {
            Log.e("RewardsRepository", "removeOrderPoints failed for $email/$orderKey", e)
        }
    }

    /** Real-time stream of the user's reward progress. */
    fun observeProfile(email: String): Flow<RewardsProfile> = callbackFlow {
        if (email.isBlank()) {
            trySend(RewardsProfile())
            awaitClose { }
            return@callbackFlow
        }
        val listener = userDoc(email).addSnapshotListener { snap, error ->
            if (error != null) {
                Log.e("RewardsRepository", "observeProfile error", error)
                trySend(RewardsProfile())
                return@addSnapshotListener
            }
            if (snap == null || !snap.exists()) {
                trySend(RewardsProfile())
                return@addSnapshotListener
            }
            @Suppress("UNCHECKED_CAST")
            val pointsByOrder = (snap.get("pointsByOrder") as? Map<String, Number>)
                ?.mapValues { it.value.toInt() }
                ?: emptyMap()
            val total = (snap.getLong("totalPoints") ?: pointsByOrder.values.sum().toLong()).toInt()
            val badge = BadgeTier.fromName(snap.getString("badgeTier"))
            trySend(RewardsProfile(totalPoints = total, badge = badge, pointsByOrder = pointsByOrder))
        }
        awaitClose { listener.remove() }
    }
}
