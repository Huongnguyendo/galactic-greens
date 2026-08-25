package com.doquynhhuong.project.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biterush_prefs")

private val sessionDisplayNameKey = stringPreferencesKey("session_display_name")
private val sessionEmailKey = stringPreferencesKey("session_email")

/**
 * Manages user preferences persisted via Jetpack DataStore:
 *  - dietaryFilter : "All" | "Vegetarian" | "Seafood" | "Chicken" | …
 *  - sortAZ        : whether to sort meals alphabetically
 *  - lastCategory  : last browsed API category (auto-restore)
 * Authentication credentials are managed by Firebase Authentication and are
 * intentionally never persisted here.
 */
class UserPreferences(private val context: Context) {

    companion object {
        val DIETARY_FILTER = stringPreferencesKey("dietary_filter")
        val SORT_AZ        = booleanPreferencesKey("sort_az")
        val LAST_CATEGORY  = stringPreferencesKey("last_category")
    }

    val dietaryFilter: Flow<String> = context.dataStore.data
        .map { it[DIETARY_FILTER] ?: "All" }

    val sortAZ: Flow<Boolean> = context.dataStore.data
        .map { it[SORT_AZ] ?: false }

    val lastCategory: Flow<String> = context.dataStore.data
        .map { it[LAST_CATEGORY] ?: "" }

    val sessionDisplayName: Flow<String?> = context.dataStore.data
        .map { it[sessionDisplayNameKey] }

    /** Lowercased email for the signed-in account; used to scope Room orders per user. */
    val sessionEmail: Flow<String?> = context.dataStore.data
        .map { it[sessionEmailKey] }

    suspend fun setSessionDisplayName(displayName: String?) {
        context.dataStore.edit { prefs ->
            if (displayName.isNullOrBlank()) prefs.remove(sessionDisplayNameKey)
            else prefs[sessionDisplayNameKey] = displayName.trim()
        }
    }

    suspend fun setSessionEmail(email: String?) {
        context.dataStore.edit { prefs ->
            if (email.isNullOrBlank()) prefs.remove(sessionEmailKey)
            else prefs[sessionEmailKey] = email.trim().lowercase()
        }
    }

    suspend fun setDietaryFilter(filter: String) =
        context.dataStore.edit { it[DIETARY_FILTER] = filter }

    suspend fun setSortAZ(enabled: Boolean) =
        context.dataStore.edit { it[SORT_AZ] = enabled }

    suspend fun setLastCategory(category: String) =
        context.dataStore.edit { it[LAST_CATEGORY] = category }

}
