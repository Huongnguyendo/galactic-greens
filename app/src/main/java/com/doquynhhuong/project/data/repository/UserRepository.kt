package com.doquynhhuong.project.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Authentication backed by Firebase Authentication, with public profile data
 * stored in Firestore.
 * Collection: users/{email}
 *
 * Document fields:
 *   email       – lowercase email (also the document ID)
 *   displayName – user's chosen display name
 *   createdAt   – epoch millis of first registration
 *   lastLoginAt – epoch millis of most recent login
 *
 * CRUD operations:
 * Passwords are handled only by Firebase Authentication and are never written
 * to Firestore or local preferences.
 *   - UPDATE : updateLogin   – bumps lastLoginAt on each successful login
 */
class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("users")

    data class UserRecord(
        val email: String,
        val displayName: String,
        val createdAt: Long,
        val lastLoginAt: Long
    )

    // ── READ ──────────────────────────────────────────────────────────────────

    /**
     * Returns the [UserRecord] for [email], or null if no account exists.
     * Throws on network/Firestore errors so the caller can surface them to the user.
     */
    suspend fun findByEmail(email: String): UserRecord? {
        val key = email.trim().lowercase()
        val snap = collection.document(key).get().await()
        if (!snap.exists()) return null
        return try {
            UserRecord(
                email       = snap.getString("email")       ?: key,
                displayName = snap.getString("displayName") ?: "",
                createdAt   = snap.getLong("createdAt")     ?: 0L,
                lastLoginAt = snap.getLong("lastLoginAt")   ?: 0L
            )
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to parse user doc $key", e)
            null
        }
    }

    /** Authenticates with Firebase Auth, then returns the matching Firestore profile. */
    suspend fun login(email: String, password: String): UserRecord {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val key = result.user?.email?.trim()?.lowercase()
            ?: error("Firebase Authentication returned no email")
        val profile = findByEmail(key)
        val now = System.currentTimeMillis()
        val user = profile ?: UserRecord(
            email = key,
            displayName = result.user?.displayName?.takeIf { it.isNotBlank() }
                ?: key.substringBefore('@'),
            createdAt = now,
            lastLoginAt = now
        )
        if (profile == null) {
            collection.document(key).set(
                mapOf(
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "createdAt" to user.createdAt,
                    "lastLoginAt" to user.lastLoginAt
                )
            ).await()
        } else {
            updateLastLogin(key)
        }
        return user
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /**
     * Registers a new user. Returns null on success, or an error message string
     * if the email is already taken or a Firestore write fails.
     */
    suspend fun register(
        email: String,
        displayName: String,
        password: String
    ): String? {
        val key = email.trim().lowercase()
        return try {
            val credential = auth.createUserWithEmailAndPassword(key, password).await()
            credential.user?.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
            )?.await()
            val now = System.currentTimeMillis()
            val data = hashMapOf(
                "email"       to key,
                "displayName" to displayName.trim(),
                "createdAt"   to now,
                "lastLoginAt" to now
            )
            collection.document(key).set(data).await()
            null   // success
        } catch (e: Exception) {
            Log.e("UserRepository", "register failed for $key", e)
            "Registration failed: ${e.message}"
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /** Bumps lastLoginAt; called after every successful login. Fire-and-forget. */
    suspend fun updateLastLogin(email: String) {
        val key = email.trim().lowercase()
        try {
            collection.document(key)
                .update("lastLoginAt", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.w("UserRepository", "updateLastLogin failed for $key (non-fatal)", e)
        }
    }
}
