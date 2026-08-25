package com.doquynhhuong.project.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.doquynhhuong.project.ui.theme.BRAmber
import com.doquynhhuong.project.ui.theme.BRBackground
import com.doquynhhuong.project.ui.theme.BRGreen
import com.doquynhhuong.project.ui.theme.BROnBackground
import com.doquynhhuong.project.ui.theme.BROnPrimary
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRPrimaryDark
import com.doquynhhuong.project.ui.theme.BRPrimaryLight
import com.doquynhhuong.project.ui.theme.BRSecondary
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurface
import com.doquynhhuong.project.ui.theme.BRSurfaceVariant
import com.doquynhhuong.project.viewmodels.OrderViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private data class TrackingStep(
    val label: String,
    val description: String,
    val insight: String? = null,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    navController: NavController,
    viewModel: OrderViewModel
) {
    val history by viewModel.orderHistory.collectAsState()
    val currentVendor by viewModel.currentVendor.collectAsState()
    val sessionEmail by viewModel.sessionEmail.collectAsState()
    /** Most recent order by time (defensive if list order ever changes). */
    val latestOrder = history.maxByOrNull { it.timestamp }

    val vendorLabel = remember(latestOrder?.vendorName, currentVendor) {
        when {
            latestOrder?.vendorName?.isNotBlank() == true -> latestOrder.vendorName
            currentVendor.isNotBlank() -> currentVendor
            else -> "Your vendor"
        }
    }

    val itemCount = latestOrder?.items?.size ?: 0

    val summaryLine = remember(latestOrder?.items, latestOrder?.timestamp) {
        latestOrder?.items
            ?.groupBy { it.name }
            ?.entries
            ?.joinToString(" · ") { (name, list) ->
                if (list.size > 1) "$name ×${list.size}" else name
            }
            ?.takeIf { it.isNotBlank() }
    }

    val totalDisplay = remember(latestOrder?.totalPrice) {
        latestOrder?.totalPrice?.takeIf { it.isNotBlank() }?.let { "$$it" } ?: "—"
    }

    val dateFormatPlaced = remember {
        SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
    }
    val placedAt = remember(latestOrder?.timestamp) {
        latestOrder?.timestamp?.let { ts -> dateFormatPlaced.format(Date(ts)) }
    }

    val orderBadge = remember(latestOrder?.timestamp, history.size) {
        if (latestOrder != null) {
            val idx = history.indexOfFirst { it.timestamp == latestOrder.timestamp }
            if (idx >= 0) "#${idx + 1}" else "#1"
        } else {
            "#—"
        }
    }

    val dropOff = latestOrder?.dropOffLocation.orEmpty()

    /** Stable key for the delivery-photo file: Firestore ID, else a timestamp fallback. */
    val orderKey = remember(latestOrder?.firestoreId, latestOrder?.timestamp) {
        latestOrder?.firestoreId?.takeIf { it.isNotBlank() }
            ?: latestOrder?.timestamp?.takeIf { it > 0 }?.toString()
            ?: ""
    }

    val steps = remember(dropOff) {
        listOf(
            TrackingStep(
                label = "Order confirmed",
                description = "We sent your order to the kitchen.",
                insight = "Status is driven by time since you placed the order and how many items you ordered.",
                icon = Icons.Filled.ReceiptLong
            ),
            TrackingStep(
                label = "In the kitchen",
                description = "Chefs are preparing your items fresh.",
                insight = "Larger orders takes a bit longer.",
                icon = Icons.Filled.Restaurant
            ),
            TrackingStep(
                label = "Finishing up",
                description = "Plating, packing, and a quick quality check.",
                insight = "You are in the last stretch before orbital delivery.",
                icon = Icons.Filled.AccessTime
            ),
            TrackingStep(
                label = "Out for delivery",
                description = if (dropOff.isNotBlank()) {
                    "Heading to: $dropOff"
                } else {
                    "Orbital delivery is on the way."
                },
                insight = if (dropOff.isNotBlank()) {
                    "Meet at your selected drop-off: $dropOff."
                } else {
                    "Have your phone ready when the order arrives."
                },
                icon = Icons.Filled.LocationOn
            ),
            TrackingStep(
                label = "Delivered",
                description = if (dropOff.isNotBlank()) {
                    "Your order has arrived at: $dropOff"
                } else {
                    "Your order has arrived."
                },
                insight = "Snap a photo of your food to lock in your reward points for this order.",
                icon = Icons.Filled.CheckCircle
            )
        )
    }

    /** Wall-clock anchor: real order time from Room, or “now” if none (demo). */
    val orderAnchorMs = remember(latestOrder?.timestamp) {
        latestOrder?.timestamp ?: System.currentTimeMillis()
    }

    /** Kitchen + orbital drop-off minutes saved at checkout (one number). */
    val savedPrepMin = latestOrder?.estimatedPrepMinutes?.takeIf { it > 0 }

    /**
     * Wall-clock seconds for the on-screen progress/timeline (compressed).
     * When [savedPrepMin] exists, length scales with vendor wait + item count.
     */
    val totalPrepSeconds = remember(itemCount, savedPrepMin) {
        if (savedPrepMin != null) {
            (savedPrepMin * 3 + itemCount * 2).coerceIn(20, 40)
        } else {
            (8 + itemCount * 2).coerceIn(10, 28)
        }
    }

    /** Boundaries (in elapsed seconds) between the 5 timeline steps. */
    val phaseEndSec = remember(totalPrepSeconds) {
        val t = totalPrepSeconds.toDouble()
        listOf(
            t * 0.10, // confirmed -> in the kitchen
            t * 0.45, // in the kitchen -> finishing up
            t * 0.75, // finishing up -> out for delivery
            t * 1.00  // out for delivery -> delivered (i.e. the actual ETA has passed)
        )
    }

    val timeFormatReady = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    /** Clock ETA: real minutes from vendor model when saved; else demo seconds. */
    val readyByTime = remember(orderAnchorMs, savedPrepMin, totalPrepSeconds) {
        val etaMs = if (savedPrepMin != null) {
            orderAnchorMs + savedPrepMin * 60_000L
        } else {
            orderAnchorMs + totalPrepSeconds * 1000L
        }
        timeFormatReady.format(Date(etaMs))
    }

    val prepEtaLabel = remember(totalPrepSeconds, savedPrepMin) {
        when {
            savedPrepMin != null ->
                "~${savedPrepMin} min total (cook + orbit) · ~${totalPrepSeconds}s on-screen steps"
            totalPrepSeconds < 60 -> "~${totalPrepSeconds}s demo timeline"
            else -> "~${totalPrepSeconds / 60} min ${totalPrepSeconds % 60}s demo timeline"
        }
    }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Live order tracking", fontWeight = FontWeight.Bold)
                        Text(
                            "From order time · $prepEtaLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = BROnPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BRPrimary,
                    titleContentColor = BROnPrimary
                )
            )
        }
    ) { innerPadding ->
        LiveTrackingOrderBody(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            orderBadge = orderBadge,
            vendorLabel = vendorLabel,
            summaryLine = summaryLine,
            totalDisplay = totalDisplay,
            itemCount = itemCount,
            placedAt = placedAt,
            readyByTime = readyByTime,
            orderAnchorMs = orderAnchorMs,
            totalPrepSeconds = totalPrepSeconds,
            phaseEndSec = phaseEndSec,
            savedPrepMin = savedPrepMin,
            dropOff = dropOff,
            steps = steps,
            orderKey = orderKey,
            remotePhotoUrls = latestOrder?.photoUrls.orEmpty(),
            onPhotoCountChanged = { count -> viewModel.recordDeliveryPhotoCount(orderKey, count) },
            onPhotoCaptured = { file ->
                // Only try uploading when we have a real Firestore order id (not the timestamp fallback)
                latestOrder?.firestoreId?.takeIf { it.isNotBlank() }?.let { fid ->
                    val ownerEmail = latestOrder.ownerEmail.ifBlank { sessionEmail.orEmpty() }
                    viewModel.uploadDeliveryPhoto(file, ownerEmail, fid)
                }
            },
            navController = navController
        )
    }
}

/**
 * Owns the wall-clock tick and all UI that depends on elapsed time.
 * Recomposes ~1×/s while visible; [OrderTrackingScreen] above stays stable.
 */
@Composable
private fun LiveTrackingOrderBody(
    modifier: Modifier = Modifier,
    orderBadge: String,
    vendorLabel: String,
    summaryLine: String?,
    totalDisplay: String,
    itemCount: Int,
    placedAt: String?,
    readyByTime: String,
    orderAnchorMs: Long,
    totalPrepSeconds: Int,
    phaseEndSec: List<Double>,
    savedPrepMin: Int?,
    dropOff: String,
    steps: List<TrackingStep>,
    orderKey: String,
    remotePhotoUrls: List<String>,
    onPhotoCountChanged: (Int) -> Unit = {},
    onPhotoCaptured: (java.io.File) -> Unit = {},
    navController: NavController
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(orderAnchorMs) {
        while (true) {
            delay(1_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val elapsedSec = ((nowMs - orderAnchorMs) / 1000.0).coerceAtLeast(0.0)

    val currentStep = when {
        elapsedSec < phaseEndSec[0] -> 0
        elapsedSec < phaseEndSec[1] -> 1
        elapsedSec < phaseEndSec[2] -> 2
        elapsedSec < phaseEndSec[3] -> 3
        else -> 4
    }

    val progressRaw =
        (elapsedSec / totalPrepSeconds.toDouble()).toFloat().coerceIn(0f, 1f)

    val secondsLeft = maxOf(0, ceil(totalPrepSeconds - elapsedSec).toInt())

    val dynamicInsight = remember(currentStep, itemCount, dropOff, savedPrepMin) {
        val base = steps[currentStep].insight.orEmpty()
        val vendorEta = if (savedPrepMin != null) {
            " About ${savedPrepMin} min from order to arrival (cook + orbit) at the ETA."
        } else ""
        val extra = when {
            itemCount >= 8 -> " Larger order — extra prep time is baked into your ETA."
            itemCount <= 2 && currentStep == 1 -> " Small order — you may move through stages a bit faster."
            else -> ""
        }
        base + vendorEta + extra
    }

    Column(modifier.verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BRPrimary, BRPrimaryDark, BRSecondary.copy(alpha = 0.92f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                "Order $orderBadge",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = BROnPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text("Live", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = BROnPrimary
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledContainerColor = Color.White.copy(alpha = 0.2f),
                                disabledLabelColor = BROnPrimary,
                                disabledLeadingIconContentColor = BROnPrimary
                            ),
                            border = null
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        vendorLabel,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = BROnPrimary,
                            letterSpacing = 0.2.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        when {
                            summaryLine != null -> "$summaryLine · $totalDisplay"
                            itemCount > 0 -> "$itemCount items · $totalDisplay"
                            else -> totalDisplay
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = BROnPrimary.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    placedAt?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium,
                            color = BROnPrimary.copy(alpha = 0.8f)
                        )
                    }
                    if (currentStep < steps.lastIndex) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Estimated arrival around $readyByTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = BROnPrimary.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = BRSurface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(BRPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            steps[currentStep].icon,
                            contentDescription = null,
                            tint = BRPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(Modifier.weight(1f)) {
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                fadeIn(tween(280)) togetherWith fadeOut(tween(180))
                            },
                            label = "statusTitle"
                        ) { step ->
                            Text(
                                steps[step].label,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = BROnBackground
                            )
                        }
                        AnimatedContent(
                            targetState = currentStep,
                            transitionSpec = {
                                fadeIn(tween(320)) togetherWith fadeOut(tween(200))
                            },
                            label = "statusDesc"
                        ) { step ->
                            Text(
                                steps[step].description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = BRSubtext
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BRSurfaceVariant
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.TipsAndUpdates,
                            contentDescription = null,
                            tint = BRPrimary.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            dynamicInsight,
                            style = MaterialTheme.typography.bodySmall,
                            color = BROnBackground.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "Progress",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = BRSubtext,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressRaw },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = BRPrimary,
                    trackColor = BRPrimaryLight
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Timeline",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = BROnBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BRSurface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                steps.forEachIndexed { index, step ->
                    TrackingStepRow(
                        step = step,
                        isCompleted = index < currentStep,
                        isActive = index == currentStep,
                        isLast = index == steps.lastIndex,
                        secondsLeft = secondsLeft
                    )
                    if (index < steps.lastIndex) {
                        Box(
                            Modifier
                                .padding(start = 21.dp)
                                .width(3.dp)
                                .height(14.dp)
                                .background(
                                    if (index < currentStep) {
                                        Brush.verticalGradient(
                                            listOf(BRGreen, BRGreen.copy(alpha = 0.45f))
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            listOf(BRSurfaceVariant, BRSurfaceVariant)
                                        )
                                    }
                                )
                        )
                    }
                }
            }
        }

        if (currentStep == steps.lastIndex && orderKey.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            DeliveryPhotoCard(
                orderKey = orderKey,
                remotePhotoUrls = remotePhotoUrls,
                onPhotoCountChanged = onPhotoCountChanged,
                onPhotoCaptured = onPhotoCaptured
            )
        }

        Spacer(Modifier.height(20.dp))

        if (currentStep == steps.lastIndex) {
            Button(
                onClick = {
                    navController.navigate("history") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
            ) {
                Text("View order history", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Home, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back to home")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TrackingStepRow(
    step: TrackingStep,
    isCompleted: Boolean,
    isActive: Boolean,
    isLast: Boolean,
    secondsLeft: Int
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isActive -> BRPrimary
                        isCompleted -> BRGreen
                        else -> BRSurfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted && !isActive -> Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                else -> Icon(
                    step.icon,
                    contentDescription = null,
                    tint = when {
                        isActive -> BROnPrimary
                        else -> BRSubtext.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                step.label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = when {
                        isActive -> BRPrimary
                        isCompleted -> BROnBackground
                        else -> BRSubtext
                    }
                )
            )
            if (isActive || isCompleted) {
                Text(
                    step.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isActive) BRSubtext else BRSubtext.copy(alpha = 0.85f)
                    )
                )
            }
        }

        if (isActive && !isLast && secondsLeft > 0) {
            Text(
                if (secondsLeft < 60) "~${secondsLeft}s" else "~${secondsLeft / 60} min",
                style = MaterialTheme.typography.labelSmall,
                color = BRAmber,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
