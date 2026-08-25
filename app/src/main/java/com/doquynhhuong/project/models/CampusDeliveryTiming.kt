package com.doquynhhuong.project.models

/**
 * Short simulated orbital food runs between nearby themed delivery zones.
 * Used for checkout time picker, saved order ETA, and tracking.
 */
object OrbitalDeliveryTiming {
    /** Minutes to get food from the space kitchen to the themed drop-off zone. */
    const val ORBITAL_DELIVERY_MINUTES = 15

    const val SERVICE_START_H = 9
    const val SERVICE_END_H = 17
    const val SLOT_STEP_MINUTES = 5
}
