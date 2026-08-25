package com.doquynhhuong.project.location

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.awaitCancellation

/**
 * Holds the current device location (or null if unavailable/denied) and
 * whether the location permission has been granted.
 */
data class CurrentLocationState(
    val latitude: Double?,
    val longitude: Double?,
    val hasPermission: Boolean
)

/**
 * Composable helper that:
 *   1. Checks if ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION is granted
 *   2. If granted, subscribes to ongoing location updates from the
 *      Fused Location Provider (not just a one-shot fix) — this is what
 *      lets the map "auto-follow" the user as they move.
 *
 * Updates stop automatically when the composable leaves the composition
 * (e.g. navigating away from the screen), so there's no battery drain once
 * you're off this screen.
 *
 * Does NOT request the permission itself — pair this with a permission
 * launcher in the screen that uses it, so the user sees a clear system prompt.
 */
@Composable
fun rememberCurrentLocation(permissionGrantedTrigger: Boolean): CurrentLocationState {
    val context = LocalContext.current

    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val hasPermission = remember(permissionGrantedTrigger) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect

        val client = LocationServices.getFusedLocationProviderClient(context)

        // Seed with a quick one-shot fix so we show *something* immediately,
        // before the first periodic update arrives.
        try {
            client.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        latitude = loc.latitude
                        longitude = loc.longitude
                    }
                }
        } catch (_: SecurityException) {
            return@LaunchedEffect
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            /* intervalMillis = */ 4000L
        ).setMinUpdateIntervalMillis(2000L).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                latitude = loc.latitude
                longitude = loc.longitude
            }
        }

        try {
            client.requestLocationUpdates(request, callback, null)
            // Keep this coroutine alive (and therefore the subscription)
            // until the composable is disposed, then clean up.
            try {
                awaitCancellation()
            } finally {
                client.removeLocationUpdates(callback)
            }
        } catch (_: SecurityException) {
            // Permission revoked mid-flight — ignore
        }
    }

    return CurrentLocationState(latitude, longitude, hasPermission)
}
