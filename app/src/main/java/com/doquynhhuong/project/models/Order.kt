package com.doquynhhuong.project.models

data class Order(
    val items: List<MenuItem>,
    val vendorName: String,
    val timestamp: Long,
    val totalPrice: String = "",
    val isDelivery: Boolean = false,
    val dropOffLocation: String = "",
    val ownerEmail: String = "",
    val estimatedPrepMinutes: Int = 0,
    /** Firestore document ID. Empty string for orders not yet persisted. */
    val firestoreId: String = "",
    /** Raw summary string stored in Firestore, e.g. "Spaghetti ×2, Tiramisu ×1". */
    val itemsSummary: String = "",
    /** Firebase Storage download URLs persisted on the Firestore order document. */
    val photoUrls: List<String> = emptyList()
)
