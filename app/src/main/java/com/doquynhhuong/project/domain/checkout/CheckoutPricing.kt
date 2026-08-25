package com.doquynhhuong.project.domain.checkout

import com.doquynhhuong.project.models.MenuItem

/** Fixed orbital-delivery fee and subtotal helper — single place for checkout math. */
object CheckoutPricing {
    const val DELIVERY_FEE: Double = 2.50

    fun subtotal(selectedItems: List<MenuItem>): Double =
        selectedItems.sumOf { it.price.replace("$", "").toDoubleOrNull() ?: 0.0 }

    fun total(selectedItems: List<MenuItem>): Double = subtotal(selectedItems) + DELIVERY_FEE
}
