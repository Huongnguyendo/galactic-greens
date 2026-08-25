package com.doquynhhuong.project.domain.checkout

import com.doquynhhuong.project.models.MenuItem

/**
 * Kitchen prep estimate for UI (same scale as home / tracking cards).
 */
object VendorPrepEstimator {

    fun estimateKitchenPrepMinutes(
        selectedItems: List<MenuItem>,
        currentVendor: String
    ): Int {
        val labels = selectedItems.map { it.vendorName.trim() }.filter { it.isNotEmpty() }.distinct()
        val candidates = if (labels.isNotEmpty()) labels else listOf(currentVendor.trim())
        val mins = candidates.map { label ->
            if (label.isEmpty()) 12 else (label.length % 16) + 5
        }
        return (mins.maxOrNull() ?: 12).coerceIn(5, 20)
    }
}
