package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doquynhhuong.project.domain.delivery.OrbitalDeliveryScheduling
import com.doquynhhuong.project.models.OrbitalDeliveryTiming
import com.doquynhhuong.project.ui.theme.BROnBackground
import com.doquynhhuong.project.ui.theme.BROnPrimary
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Orbital delivery window + hour/minute lists in a [Dialog]. */
@Composable
internal fun DeliveryTimePickerDialog(
    initialTime: String,
    vendorPrepMinutes: Int,
    /** Bumps when the dialog is reopened so “now + prep” is recomputed. */
    dialogKey: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val minCal = remember(vendorPrepMinutes, dialogKey) {
        OrbitalDeliveryScheduling.minSelectableCalendar(vendorPrepMinutes)
    }

    val earliestTimeLabel = remember(minCal) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(minCal.timeInMillis))
    }

    val initialPair = remember(initialTime, minCal) {
        if (initialTime.isBlank()) {
            minCal.get(Calendar.HOUR_OF_DAY) to minCal.get(Calendar.MINUTE)
        } else {
            val parts = initialTime.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: minCal.get(Calendar.HOUR_OF_DAY)
            val m = parts.getOrNull(1)?.toIntOrNull() ?: minCal.get(Calendar.MINUTE)
            val picked = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val clamped = OrbitalDeliveryScheduling.clampSlot(picked, minCal)
            clamped.get(Calendar.HOUR_OF_DAY) to clamped.get(Calendar.MINUTE)
        }
    }

    var hourMinute by remember(initialTime, minCal, dialogKey) { mutableStateOf(initialPair) }

    val hours = remember(minCal) { OrbitalDeliveryScheduling.availableHours(minCal) }
    val minutes = remember(minCal, hourMinute.first) {
        OrbitalDeliveryScheduling.availableMinutes(minCal, hourMinute.first)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 400.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Select Delivery Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Earliest delivery",
                    style = MaterialTheme.typography.labelMedium,
                    color = BRSubtext
                )
                Text(
                    earliestTimeLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BROnBackground
                )
                Text(
                    "Launch window opens ${OrbitalDeliveryTiming.SERVICE_START_H}:00. " +
                        "Prep ~${vendorPrepMinutes.coerceIn(5, 20)} min + orbital delivery ${OrbitalDeliveryTiming.ORBITAL_DELIVERY_MINUTES} mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = BRSubtext
                )
                HorizontalDivider(color = BRSurfaceVariant)
                Text(
                    "%02d:%02d".format(hourMinute.first, hourMinute.second),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BRPrimary, RoundedCornerShape(12.dp))
                        .padding(vertical = 18.dp, horizontal = 12.dp),
                    textAlign = TextAlign.Center,
                    color = BROnPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    "Hour & minute",
                    style = MaterialTheme.typography.labelMedium,
                    color = BRSubtext
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        items(hours, key = { it }) { h ->
                            val selected = h == hourMinute.first
                            Text(
                                "%02d".format(h),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val valid = OrbitalDeliveryScheduling.availableMinutes(minCal, h)
                                        val newM = when {
                                            valid.isEmpty() -> hourMinute.second
                                            hourMinute.second in valid -> hourMinute.second
                                            else -> valid.first()
                                        }
                                        hourMinute = OrbitalDeliveryScheduling.snapHourMinuteToValid(h, newM, minCal)
                                    }
                                    .background(
                                        if (selected) BRPrimary.copy(alpha = 0.22f)
                                        else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) BRPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    LazyColumn(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        items(minutes, key = { it }) { m ->
                            val selected = m == hourMinute.second
                            Text(
                                "%02d".format(m),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hourMinute = OrbitalDeliveryScheduling.snapHourMinuteToValid(
                                            hourMinute.first,
                                            m,
                                            minCal
                                        )
                                    }
                                    .background(
                                        if (selected) BRPrimary.copy(alpha = 0.22f)
                                        else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) BRPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = BRSubtext)
                    }
                    Button(
                        onClick = { onConfirm(hourMinute.first, hourMinute.second) },
                        colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
