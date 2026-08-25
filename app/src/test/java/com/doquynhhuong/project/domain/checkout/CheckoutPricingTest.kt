package com.doquynhhuong.project.domain.checkout

import com.doquynhhuong.project.models.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [CheckoutPricing]'s subtotal/total math. */
class CheckoutPricingTest {

    private fun item(price: String) = MenuItem(
        name = "Item",
        price = price,
        description = "",
        emoji = "🍔",
        thumbnailUrl = "",
        vendorName = "Test Vendor"
    )

    @Test
    fun `subtotal of an empty cart is zero`() {
        assertEquals(0.0, CheckoutPricing.subtotal(emptyList()), 0.0001)
    }

    @Test
    fun `subtotal sums dollar-formatted prices correctly`() {
        val items = listOf(item("$5.00"), item("$3.50"), item("$1.25"))
        assertEquals(9.75, CheckoutPricing.subtotal(items), 0.0001)
    }

    @Test
    fun `subtotal ignores an unparsable price instead of crashing`() {
        val items = listOf(item("$4.00"), item("N/A"))
        assertEquals(4.00, CheckoutPricing.subtotal(items), 0.0001)
    }

    @Test
    fun `total adds the fixed delivery fee to the subtotal`() {
        val items = listOf(item("$10.00"))
        assertEquals(10.00 + CheckoutPricing.DELIVERY_FEE, CheckoutPricing.total(items), 0.0001)
    }

    @Test
    fun `total of an empty cart is exactly the delivery fee`() {
        assertEquals(CheckoutPricing.DELIVERY_FEE, CheckoutPricing.total(emptyList()), 0.0001)
    }
}
