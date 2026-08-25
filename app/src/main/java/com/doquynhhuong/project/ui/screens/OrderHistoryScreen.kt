package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.doquynhhuong.project.models.Order
import com.doquynhhuong.project.ui.theme.*
import com.doquynhhuong.project.viewmodels.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(navController: NavController, viewModel: OrderViewModel) {
    val history by viewModel.orderHistory.collectAsState()
    val isLoading by viewModel.isLoadingHistory.collectAsState()
    val rewardsProfile by viewModel.rewardsProfile.collectAsState()
    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title  = { Text("Order History", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = BRPrimary,
                    titleContentColor = BROnPrimary,
                    actionIconContentColor = BROnPrimary
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "history",
                onHome    = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onBrowse  = { navController.navigate("browse") },
                onCart    = { navController.navigate("cart") },
                onHistory = { },
                onProfile = { navController.navigate("profile") }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BRPrimary)
            }
        } else if (history.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No orders yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = BRSubtext
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { navController.navigate("home") },
                        colors  = ButtonDefaults.buttonColors(containerColor = BRPrimary),
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Order Now") }
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "${history.size} order${if (history.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelLarge.copy(color = BRSubtext)
                    )
                    Spacer(Modifier.height(4.dp))
                }
                itemsIndexed(history) { index, order ->
                    val orderKey = order.firestoreId.ifBlank { order.timestamp.toString() }
                    OrderHistoryCard(
                        order       = order,
                        orderNumber = history.size - index,
                        pointsEarned = rewardsProfile.pointsByOrder[orderKey],
                        onUploadPhoto = { file ->
                            // Upload only when we have a Firestore id
                            if (order.firestoreId.isNotBlank()) {
                                viewModel.uploadDeliveryPhoto(file, order.ownerEmail, order.firestoreId)
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(
    order: Order,
    orderNumber: Int,
    pointsEarned: Int? = null,
    onUploadPhoto: (java.io.File) -> Unit = {}
) {
    val dateFormat   = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
    val displayTotal = order.totalPrice.toDoubleOrNull() ?: 0.0

    // Parse itemsSummary: "Spaghetti ×2, Tiramisu ×1"
    val parsedLines = remember(order.itemsSummary) {
        if (order.itemsSummary.isBlank()) emptyList()
        else order.itemsSummary.split(", ").mapNotNull { part ->
            val name = part.substringBeforeLast("×").trim()
            val qty  = part.substringAfterLast("×").trim().toIntOrNull() ?: 1
            if (name.isNotBlank()) name to qty else null
        }
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = BRSurface)
    ) {
        Column(Modifier.padding(16.dp)) {

            // Header row: order number + vendor + total
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BRPrimaryLight
                    ) {
                        Text(
                            "#$orderNumber",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelMedium.copy(
                                color      = BRPrimaryDark,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    if (order.vendorName.isNotEmpty()) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            order.vendorName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color      = BROnBackground,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                    }
                }
                Text(
                    "\$${" %.2f".format(displayTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color      = BRPrimary
                    )
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BRSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            // Items from itemsSummary
            if (parsedLines.isNotEmpty()) {
                parsedLines.forEach { (name, qty) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "🍽️  $name${if (qty > 1) " ×$qty" else ""}",
                            style    = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }
            }

            // Delivery badge
            if (order.isDelivery && order.dropOffLocation.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BRPrimaryLight
                ) {
                    Text(
                        "📍 ${order.dropOffLocation}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style    = MaterialTheme.typography.labelSmall.copy(color = BRPrimaryDark)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                dateFormat.format(Date(order.timestamp)),
                style = MaterialTheme.typography.labelSmall.copy(color = BRSubtext)
            )

            // Delivery photo UI: show existing photos and allow adding more
            val orderKey = remember(order.firestoreId, order.timestamp) {
                order.firestoreId.ifBlank { order.timestamp.toString() }
            }
            Spacer(Modifier.height(10.dp))
            DeliveryPhotoCard(
                orderKey = orderKey,
                modifier = Modifier.fillMaxWidth(),
                remotePhotoUrls = order.photoUrls,
                onPhotoCaptured = { file ->
                    onUploadPhoto(file)
                }
            )

            if (pointsEarned != null && pointsEarned > 0) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BRPrimaryLight
                ) {
                    Text(
                        "⭐ $pointsEarned pts earned",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BRPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
