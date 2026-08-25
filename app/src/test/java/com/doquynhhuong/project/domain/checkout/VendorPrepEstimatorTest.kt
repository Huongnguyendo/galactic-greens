package com.doquynhhuong.project.domain.checkout

import com.doquynhhuong.project.models.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [VendorPrepEstimator]. */
class VendorPrepEstimatorTest {

    private fun item(vendorName: String) = MenuItem(
        name = "Item", price = "$5.00", vendorName = vendorName
    )

    @Test
    fun `estimate is always clamped between 5 and 20 minutes`() {
        // Try a spread of vendor name lengths to exercise the clamp on both ends.
        val shortName = listOf(item("A"))
        val longName = listOf(item("A".repeat(200)))

        val shortEstimate = VendorPrepEstimator.estimateKitchenPrepMinutes(shortName, "A")
        val longEstimate = VendorPrepEstimator.estimateKitchenPrepMinutes(longName, "A")

        assertTrue(shortEstimate in 5..20)
        assertTrue(longEstimate in 5..20)
    }

    @Test
    fun `falls back to the current vendor when no item has a vendor label`() {
        val items = listOf(item(""), item("  "))
        val estimate = VendorPrepEstimator.estimateKitchenPrepMinutes(items, "Campus Grill")
        val expected = (("Campus Grill".length % 16) + 5).coerceIn(5, 20)
        assertEquals(expected, estimate)
    }

    @Test
    fun `takes the slowest vendor when multiple vendors are in the cart`() {
        val items = listOf(item("Cafe"), item("The Big Campus Diner"))
        val cafeMinutes = (("Cafe".length % 16) + 5).coerceIn(5, 20)
        val dinerMinutes = (("The Big Campus Diner".length % 16) + 5).coerceIn(5, 20)

        val estimate = VendorPrepEstimator.estimateKitchenPrepMinutes(items, "Unused")

        assertEquals(maxOf(cafeMinutes, dinerMinutes), estimate)
    }
}
