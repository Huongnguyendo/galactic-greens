package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.doquynhhuong.project.models.MenuItem
import com.doquynhhuong.project.models.MenuState
import com.doquynhhuong.project.ui.theme.*
import com.doquynhhuong.project.viewmodels.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorMenuScreen(
    vendorName: String,
    viewModel: OrderViewModel,
    navController: NavController,
    onNextClick: () -> Unit
) {
    val menuState     by viewModel.menuState.collectAsState()
    val cartItems     by viewModel.cartItems.collectAsState()
    val currentVendor by viewModel.currentVendor.collectAsState()
    val cartCount     = remember(cartItems) { cartItems.sumOf { it.quantity } }
    val cartTotal     = remember(cartItems) { viewModel.cartTotal() }

    LaunchedEffect(vendorName) { viewModel.loadMenuForVendor(vendorName) }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentVendor.ifBlank { vendorName },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    BadgedBox(
                        badge    = { if (cartCount > 0) Badge { Text("$cartCount") } },
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        IconButton(onClick = { navController.navigate("cart") }) {
                            Icon(Icons.Default.ShoppingCart, "Cart", tint = BROnPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = BRPrimary,
                    titleContentColor = BROnPrimary,
                    navigationIconContentColor = BROnPrimary
                )
            )
        },
        bottomBar = {
            if (cartCount > 0) {
                Surface(shadowElevation = 12.dp, color = BRSurface) {
                    Button(
                        onClick  = { viewModel.updateItemsFromCart(); onNextClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .height(52.dp),
                        shape  = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                    ) {
                        Icon(Icons.Default.ShoppingCart, null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "View Cart ($cartCount items)  ·  \$${"%.2f".format(cartTotal)}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = menuState) {

                is MenuState.Loading -> {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BRPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading menu…", color = BRSubtext)
                    }
                }

                is MenuState.Error -> {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .fillMaxWidth(),
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚠️", fontSize = 36.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Failed to load menu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.retryMenuLoad(vendorName) }) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                is MenuState.Success -> {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(
                            items = state.items,
                            key = { it.name }
                        ) { item ->
                            val qty = viewModel.getItemQuantity(item)
                            ImprovedMenuItemCard(
                                item     = item,
                                quantity = qty,
                                onAdd    = {
                                    if (!viewModel.addToCart(item)) {
                                        navController.navigate("login") {
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                onRemove = { viewModel.removeFromCart(item) }
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun ImprovedMenuItemCard(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val thumbRequest = remember(item.name, item.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(item.thumbnailUrl.takeIf { it.isNotEmpty() })
            .size(Size(140, 140))
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
    Card(
        shape     = RoundedCornerShape(16.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = BRSurface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            // Food image
            if (item.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model              = thumbRequest,
                    contentDescription = item.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                )
            } else {
                Box(
                    Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .background(BRSurfaceVariant)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.emoji, fontSize = 32.sp)
                }
            }

            // Details
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        item.name,
                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2
                    )
                    if (item.description.isNotEmpty()) {
                        Text(
                            item.description,
                            style    = MaterialTheme.typography.bodySmall.copy(color = BRSubtext),
                            maxLines = 1
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.price,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = BRPrimary
                        )
                    )
                    if (quantity == 0) {
                        FilledIconButton(
                            onClick  = onAdd,
                            modifier = Modifier.size(48.dp),
                            colors   = IconButtonDefaults.filledIconButtonColors(
                                containerColor = BRPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add ${item.name} to cart", modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilledIconButton(
                                onClick  = onRemove,
                                modifier = Modifier
                                    .size(48.dp)
                                    .semantics { contentDescription = "Decrease quantity of ${item.name}" },
                                colors   = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = BRSurfaceVariant
                                )
                            ) {
                                Text("−", fontWeight = FontWeight.Bold, color = BROnBackground)
                            }
                            Text(
                                "$quantity",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(20.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                    contentDescription = "Increase quantity of ${item.name}",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
