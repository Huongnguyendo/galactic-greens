package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.doquynhhuong.project.models.CartItem
import com.doquynhhuong.project.ui.theme.*
import com.doquynhhuong.project.viewmodels.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(viewModel: OrderViewModel, navController: NavController) {
    val cartItems  by viewModel.cartItems.collectAsState()
    val currentVendor by viewModel.currentVendor.collectAsState()
    val vendorsSummary = remember(cartItems, currentVendor) {
        val distinct = cartItems
            .map { it.menuItem.vendorName.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        when {
            distinct.isNotEmpty() -> distinct.joinToString(", ")
            currentVendor.isNotBlank() -> currentVendor
            else -> ""
        }
    }

    val total = remember(cartItems) {
        cartItems.sumOf { ci ->
            (ci.menuItem.price.replace("$", "").toDoubleOrNull() ?: 0.0) * ci.quantity
        }
    }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = { Text("Your Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCart() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor             = BRPrimary,
                    titleContentColor          = BROnPrimary,
                    navigationIconContentColor = BROnPrimary
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(shadowElevation = 12.dp, color = BRSurface) {
                    Column(
                        Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                "\$${"%.2f".format(total)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = BRPrimary
                                )
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateItemsFromCart()
                                navController.navigate("confirmation")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                        ) {
                            Text("Proceed to Checkout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (cartItems.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your cart is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = BRSubtext
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("home") },
                        shape   = RoundedCornerShape(12.dp)
                    ) { Text("Browse Vendors") }
                }
            }
        } else {
            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding      = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (vendorsSummary.isNotEmpty()) {
                    item {
                        Text(
                            "From $vendorsSummary",
                            style = MaterialTheme.typography.labelLarge.copy(color = BRSubtext)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(
                    cartItems,
                    key = { "${it.menuItem.name}|${it.menuItem.vendorName}" }
                ) { cartItem ->
                    ImprovedCartItemRow(
                        cartItem = cartItem,
                        onAdd    = {
                            if (!viewModel.addToCart(cartItem.menuItem)) {
                                navController.navigate("login") { launchSingleTop = true }
                            }
                        },
                        onRemove = { viewModel.removeFromCart(cartItem.menuItem) },
                        onDelete = {
                            repeat(cartItem.quantity) { viewModel.removeFromCart(cartItem.menuItem) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovedCartItemRow(
    cartItem: CartItem,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    val itemPrice = cartItem.menuItem.price.replace("$", "").toDoubleOrNull() ?: 0.0
    val lineTotal = itemPrice * cartItem.quantity

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors    = CardDefaults.cardColors(containerColor = BRSurface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cartItem.menuItem.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model              = cartItem.menuItem.thumbnailUrl,
                    contentDescription = cartItem.menuItem.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cartItem.menuItem.emoji, fontSize = 26.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    cartItem.menuItem.name,
                    style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )
                if (cartItem.menuItem.vendorName.isNotBlank()) {
                    Text(
                        cartItem.menuItem.vendorName,
                        style = MaterialTheme.typography.labelSmall.copy(color = BRPrimary)
                    )
                }
                Text(
                    "${cartItem.menuItem.price} each",
                    style = MaterialTheme.typography.bodySmall.copy(color = BRSubtext)
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick  = onRemove,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Decrease quantity of ${cartItem.menuItem.name}" },
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BRSurfaceVariant
                        )
                    ) { Text("−", color = BROnBackground) }
                    Text(
                        "${cartItem.quantity}",
                        modifier   = Modifier.padding(horizontal = 10.dp),
                        fontWeight = FontWeight.Bold
                    )
                    FilledIconButton(
                        onClick  = onAdd,
                        modifier = Modifier.size(48.dp),
                        colors   = IconButtonDefaults.filledIconButtonColors(
                            containerColor = BRPrimary
                        )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Increase quantity of ${cartItem.menuItem.name}",
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "\$${"%.2f".format(lineTotal)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color      = BRPrimary
                    )
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove ${cartItem.menuItem.name} from cart",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}