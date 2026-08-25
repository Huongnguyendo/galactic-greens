package com.doquynhhuong.project.domain.rewards

/**
 * Photo → points rules for a single order (the "artwork" in the spec):
 *   1-5 photos uploaded  → 10 points
 *   6-10 photos uploaded → 20 points
 *   Maximum per order    → 20 points (extra photos beyond 10 don't add more)
 */
object PointsCalculator {

    const val MAX_POINTS_PER_ORDER = 20

    fun pointsForPhotoCount(photoCount: Int): Int = when {
        photoCount <= 0 -> 0
        photoCount in 1..5 -> 10
        else -> MAX_POINTS_PER_ORDER // 6+ photos
    }
}
