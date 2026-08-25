package com.doquynhhuong.project.domain.delivery

import com.doquynhhuong.project.models.OrbitalDeliveryTiming
import java.util.Calendar

/**
 * Pure scheduling logic for orbital-delivery slots (no Android / Compose).
 * Used by the checkout time picker and kept testable in isolation.
 */
object OrbitalDeliveryScheduling {

    fun ceilMinuteToStep(cal: Calendar, step: Int) {
        val m = cal.get(Calendar.MINUTE)
        val rem = m % step
        if (rem != 0) cal.add(Calendar.MINUTE, step - rem)
    }

    fun todayAt(hour: Int, minute: Int = 0): Calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /** End of service on the same calendar day as [reference]. */
    fun closeOnSameDayAs(reference: Calendar): Calendar =
        (reference.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, OrbitalDeliveryTiming.SERVICE_END_H)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /**
     * Earliest selectable instant: service window + kitchen prep + orbital leg, rounded to slot step.
     * After today’s close → next day open + prep.
     */
    fun minSelectableCalendar(vendorPrepMinutes: Int): Calendar {
        val vPrep = vendorPrepMinutes.coerceIn(5, 20)
        val open = todayAt(OrbitalDeliveryTiming.SERVICE_START_H)
        val closeToday = todayAt(OrbitalDeliveryTiming.SERVICE_END_H)
        val now = Calendar.getInstance()
        val effectiveStart: Calendar = when {
            now.before(open) -> open.clone() as Calendar
            now.after(closeToday) -> {
                val next = open.clone() as Calendar
                next.add(Calendar.DAY_OF_MONTH, 1)
                next
            }
            else -> now.clone() as Calendar
        }
        val eta = effectiveStart.clone() as Calendar
        eta.add(Calendar.MINUTE, vPrep)
        eta.add(Calendar.MINUTE, OrbitalDeliveryTiming.ORBITAL_DELIVERY_MINUTES)
        ceilMinuteToStep(eta, OrbitalDeliveryTiming.SLOT_STEP_MINUTES)
        val dayClose = closeOnSameDayAs(eta)
        return when {
            eta.after(dayClose) -> dayClose.clone() as Calendar
            else -> eta
        }
    }

    fun clampSlot(candidate: Calendar, earliest: Calendar): Calendar {
        val r = candidate.clone() as Calendar
        if (r.before(earliest)) return earliest.clone() as Calendar
        val dayClose = closeOnSameDayAs(r)
        return if (r.after(dayClose)) dayClose.clone() as Calendar else r
    }

    fun availableHours(minCal: Calendar): List<Int> {
        val h0 = minCal.get(Calendar.HOUR_OF_DAY)
        return (h0..OrbitalDeliveryTiming.SERVICE_END_H).toList()
    }

    fun availableMinutes(minCal: Calendar, selectedHour: Int): List<Int> {
        val minH = minCal.get(Calendar.HOUR_OF_DAY)
        val minM = minCal.get(Calendar.MINUTE)
        val step = OrbitalDeliveryTiming.SLOT_STEP_MINUTES
        return if (selectedHour > minH) {
            (0 until 60 step step).toList()
        } else {
            val start = (minM + step - 1) / step * step
            (start until 60 step step).toList()
        }
    }

    fun snapHourMinuteToValid(h: Int, m: Int, minCal: Calendar): Pair<Int, Int> {
        val valid = availableMinutes(minCal, h)
        if (valid.isEmpty()) return h to m
        val m2 = if (m in valid) m else valid.first()
        return h to m2
    }
}
