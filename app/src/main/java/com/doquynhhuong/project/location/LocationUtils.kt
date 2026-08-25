package com.doquynhhuong.project.location

import android.location.Location

/**
 * Computes the distance in meters between two lat/lng points using
 * Android's built-in Location.distanceBetween (Haversine-based).
 */
fun distanceMeters(
    fromLat: Double, fromLng: Double,
    toLat: Double, toLng: Double
): Float {
    val results = FloatArray(1)
    Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
    return results[0]
}

/** Formats a distance in meters as a short human-readable string, e.g. "350 m" or "1.2 km". */
fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
        "${meters.toInt()} m"
    } else {
        "%.1f km".format(meters / 1000f)
    }
}
