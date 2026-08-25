package com.doquynhhuong.project.domain.rewards

/**
 * Achievement badge tiers based on a user's total accumulated points
 * (summed across all their orders' [PointsCalculator] results):
 *   Explorer  : 0-100 points
 *   Curator   : 101-250 points
 *   Archivist : 251-500+ points
 */
enum class BadgeTier(val label: String, val minPoints: Int) {
    EXPLORER("Explorer", 0),
    CURATOR("Curator", 101),
    ARCHIVIST("Archivist", 251);

    companion object {
        fun forPoints(totalPoints: Int): BadgeTier = when {
            totalPoints >= ARCHIVIST.minPoints -> ARCHIVIST
            totalPoints >= CURATOR.minPoints -> CURATOR
            else -> EXPLORER
        }

        fun fromName(name: String?): BadgeTier =
            entries.firstOrNull { it.name == name } ?: EXPLORER
    }
}
