package com.doquynhhuong.project.models

data class MenuItem(
    val name: String,
    val price: String,
    val description: String = "",
    val emoji: String = "🍽️",
    val thumbnailUrl: String = "",
    /** Space-kitchen display name for this line (from cart / menu). */
    val vendorName: String = ""
)

/** Deterministic price from meal name so the same meal always shows the same price. */
fun generatePrice(mealName: String): String {
    val base = (mealName.length % 8) + 5
    return "\$$base"
}

/** Emoji picked by meal category name. */
fun categoryEmoji(category: String): String = when (category.lowercase()) {
    "beef"          -> "🥩"
    "chicken"       -> "🍗"
    "dessert"       -> "🍰"
    "lamb"          -> "🍖"
    "pasta"         -> "🍝"
    "pork"          -> "🥓"
    "seafood"       -> "🦞"
    "side"          -> "🥗"
    "starter"       -> "🥣"
    "vegan"         -> "🥦"
    "vegetarian"    -> "🥕"
    "breakfast"     -> "🍳"
    "goat"          -> "🐐"
    "miscellaneous" -> "🍽️"
    "mexican"       -> "🌮"
    "italian"       -> "🍕"
    else            -> "🍴"
}
