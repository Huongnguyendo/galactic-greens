package com.doquynhhuong.project.models

data class Vendor(
    val name: String,           // Stable Firestore/route ID, unique per restaurant
    val displayName: String,    // Human-friendly vendor name shown in the UI
    val status: String,
    val statusColor: androidx.compose.ui.graphics.Color,
    val category: String,       // matches TheMealDB category name
    val emoji: String = "🍽️",
    val rating: Float = 4.5f,
    val thumbnailUrl: String = "",
    /** Latitude/longitude on the space-delivery simulation map. */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/** Maps a TheMealDB category name to a playful space-food vendor name. */
fun vendorNameForCategory(category: String): String = when (category.lowercase()) {
    "beef"          -> "Meteor Burger Lab"
    "chicken"       -> "Rocket Rooster"
    "dessert"       -> "Milky Way Sweets"
    "lamb"          -> "Mars Mediterranean"
    "pasta"         -> "Nebula Noodles"
    "pork"          -> "Red Planet BBQ"
    "seafood"       -> "Europa Ocean Plate"
    "side"          -> "Orbit Bowls"
    "starter"       -> "Launch Pad Bites"
    "vegan"         -> "Green Galaxy"
    "vegetarian"    -> "Cosmic Garden"
    "breakfast"     -> "Sunrise Station"
    "goat"          -> "Comet Spice Route"
    "miscellaneous" -> "Space Station Kitchen"
    "mexican"       -> "Lunar Taco Base"
    "italian"       -> "Venus Italia"
    else             -> "$category Space Kitchen"
}

/** Gives each ten-item menu in a category its own distinct restaurant identity. */
fun vendorNameForCategoryPage(category: String, page: Int): String {
    val names = when (category.lowercase()) {
        "dessert" -> listOf(
            "Milky Way Sweets",
            "Moonlight Bakery",
            "Supernova Scoops",
            "Comet Candy Lab",
            "Saturn Sugar Ring"
        )
        "vegan" -> listOf(
            "Green Galaxy",
            "Plant Planet",
            "Cosmic Sprout",
            "Orbit Organics",
            "Nova Vegan Kitchen"
        )
        "vegetarian" -> listOf(
            "Cosmic Garden",
            "Aurora Harvest",
            "Lunar Leaf Cafe",
            "Stardust Veggie Grill",
            "Planet Green Kitchen"
        )
        "breakfast" -> listOf(
            "Sunrise Station", "Solar Brunch", "Dawn Orbit Cafe", "Morning Moon", "Apollo Breakfast Club"
        )
        "pasta" -> listOf(
            "Nebula Noodles", "Mars Pasta Lab", "Orbit Trattoria", "Venus Pasta House", "Cosmic Carbs"
        )
        "side" -> listOf(
            "Orbit Bowls", "Satellite Sides", "Lunar Small Plates", "Comet Extras", "Asteroid Bites"
        )
        "starter" -> listOf(
            "Launch Pad Bites", "First Contact Cafe", "Ignition Plates", "Nova Appetizers", "Mission Starters"
        )
        else -> listOf(
            vendorNameForCategory(category),
            "Starlight Kitchen",
            "Lunar Table",
            "Nova Cafe",
            "Aurora Eatery"
        )
    }
    return names[page.coerceIn(0, names.lastIndex)]
}

/**
 * Seed simulation coordinates per vendor category, so every vendor has a
 * fixed pin. These are spread around the same space-themed launch area
 * used by [com.doquynhhuong.project.ui.screens.SPACE_DESTINATIONS].
 *
 * The values are deterministic (derived from the category's hash) so each
 * vendor always lands in the same spot rather than jumping around between
 * app launches or API refreshes.
 */
fun vendorLatLngForCategory(category: String): Pair<Double, Double> {
    // Center of the space-delivery simulation near Kennedy Space Center.
    val baseLat = 28.5721
    val baseLng = -80.6480

    // Spread vendors in a small ~600m radius around the simulation center using a
    // deterministic pseudo-random offset based on the category name.
    val seed = category.lowercase().hashCode()
    val angle = (seed % 360).let { if (it < 0) it + 360 else it } * (Math.PI / 180.0)
    val radiusDegrees = 0.004 + (kotlin.math.abs(seed) % 100) / 100.0 * 0.003

    val lat = baseLat + radiusDegrees * kotlin.math.sin(angle)
    val lng = baseLng + radiusDegrees * kotlin.math.cos(angle)
    return lat to lng
}
