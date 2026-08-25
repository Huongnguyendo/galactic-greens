package com.doquynhhuong.project.ui.screens

/**
 * A fictional space-delivery destination anchored to real Earth coordinates
 * near Kennedy Space Center so Google Maps and distance calculations work.
 */
data class SpaceDestination(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Space-themed delivery zones for the Galactic Greens orbital-delivery simulation.
 */
internal val SPACE_DESTINATIONS = listOf(
    SpaceDestination("🔴 Mars Base Alpha",       28.5721, -80.6480),
    SpaceDestination("🟠 Jupiter Cloud Station", 28.5732, -80.6465),
    SpaceDestination("🌙 Lunar Crater Outpost",  28.5710, -80.6468),
    SpaceDestination("🪐 Saturn Ring Port",      28.5708, -80.6493),
    SpaceDestination("🧊 Europa Ice Dock",       28.5734, -80.6491)
)

/** Destination labels used by selection controls. */
internal val SPACE_DESTINATION_LABELS: List<String> =
    SPACE_DESTINATIONS.map { it.label }

/** Looks up a destination and its Earth-anchored map coordinates by label. */
internal fun spaceDestinationByLabel(label: String): SpaceDestination? =
    SPACE_DESTINATIONS.find { it.label == label }
