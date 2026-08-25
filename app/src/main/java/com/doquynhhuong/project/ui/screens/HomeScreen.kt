package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.doquynhhuong.project.models.UiState
import com.doquynhhuong.project.models.Vendor
import com.doquynhhuong.project.ui.theme.*
import com.doquynhhuong.project.viewmodels.OrderViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: OrderViewModel,
    userName: String = "",
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    var searchQuery      by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val cartItems        by viewModel.cartItems.collectAsState()
    val vendorsState     by viewModel.vendorsState.collectAsState()
    val totalCartCount   = remember(cartItems) { cartItems.sumOf { it.quantity } }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF6D28D9), BRPrimary, Color(0xFFE14B6F))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (userName.isNotBlank()) "Welcome, $userName 👋" else "Browsing as Guest 👋",
                            style = MaterialTheme.typography.labelMedium,
                            color = BROnPrimary.copy(alpha = 0.80f)
                        )
                        Text(
                            "Galactic Greens",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = BROnPrimary
                            )
                        )
                        Text(
                            "Cosmic cravings, delivered fast ✦",
                            style = MaterialTheme.typography.labelSmall,
                            color = BROnPrimary.copy(alpha = 0.78f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (userName.isBlank()) {
                            TextButton(onClick = onSignInClick) {
                                Text("Sign in", color = BROnPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            TextButton(onClick = onSignOutClick) {
                                Text("Sign out", color = BROnPrimary.copy(alpha = 0.92f))
                            }
                        }
                        BadgedBox(
                            badge = { if (totalCartCount > 0) Badge { Text("$totalCartCount") } }
                        ) {
                            IconButton(onClick = { navController.navigate("cart") }) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = BROnPrimary
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "home",
                onHome   = { /* already here */ },
                onBrowse = { navController.navigate("browse") },
                onCart   = { navController.navigate("cart") },
                onHistory = { navController.navigate("history") },
                onProfile = { navController.navigate("profile") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("browse") },
                containerColor = BRPrimary,
                contentColor = BROnPrimary,
                content = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Browse vendors"
                    )
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {

            // ── Search bar ────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = {
                        Text(
                            "Search vendors or food...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = BRSubtext)
                        )
                    },
                    leadingIcon   = { Icon(Icons.Default.Search, null, tint = BRSubtext) },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape         = RoundedCornerShape(14.dp),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor   = Color.Transparent,
                        focusedBorderColor     = BRPrimary,
                        unfocusedContainerColor = BRSurface,
                        focusedContainerColor  = BRSurface
                    )
                )
            }

            // ── Category chips ────────────────────────────────────────────
            if (vendorsState is UiState.Success) {
                val categories = (vendorsState as UiState.Success<List<Vendor>>)
                    .data.map { it.category }.distinct()

                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "Categories",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BROnBackground
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BRFilterChip(
                                label    = "All",
                                selected = selectedCategory == null,
                                onClick  = { selectedCategory = null }
                            )
                            categories.forEach { cat ->
                                BRFilterChip(
                                    label    = cat,
                                    selected = selectedCategory == cat,
                                    onClick  = {
                                        selectedCategory = if (selectedCategory == cat) null else cat
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Hero card for top-rated vendor ────────────────────────────
            if (vendorsState is UiState.Success && searchQuery.isBlank() && selectedCategory == null) {
                val topVendor = (vendorsState as UiState.Success<List<Vendor>>)
                    .data.maxByOrNull { it.rating }
                if (topVendor != null) {
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                "🔥 Featured",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BROnBackground
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            HeroVendorCard(vendor = topVendor) {
                                viewModel.setCurrentVendor(topVendor.displayName)
                                val safe = URLEncoder.encode(topVendor.name, StandardCharsets.UTF_8.toString())
                                navController.navigate("menu/$safe")
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }

            // ── Nearby Vendors heading ────────────────────────────────────
            item {
                Text(
                    "Nearby Space Kitchens",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BROnBackground
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Vendor list ───────────────────────────────────────────────
            when (val state = vendorsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BRPrimary)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading vendors…", color = BRSubtext)
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
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
                                    "Could not load vendors",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    state.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.fetchVendors() }) {
                                    Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                is UiState.Success -> {
                    val filtered = state.data.filter { v ->
                        val matchSearch   = v.displayName.contains(searchQuery, ignoreCase = true) ||
                                v.category.contains(searchQuery, ignoreCase = true)
                        val matchCategory = selectedCategory == null || v.category == selectedCategory
                        matchSearch && matchCategory
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔍", fontSize = 40.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No vendors found", color = BRSubtext)
                                }
                            }
                        }
                    } else {
                        items(filtered) { vendor ->
                            VendorCard(
                                vendor  = vendor,
                                onClick = {
                                    viewModel.setCurrentVendor(vendor.displayName)
                                    val safe = URLEncoder.encode(
                                        vendor.name, StandardCharsets.UTF_8.toString()
                                    )
                                    navController.navigate("menu/$safe")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Hero card ─────────────────────────────────────────────────────────────────

@Composable
fun HeroVendorCard(vendor: Vendor, onClick: () -> Unit) {
    val context = LocalContext.current
    val heroImage = remember(vendor.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(vendor.thumbnailUrl.takeIf { it.isNotEmpty() })
            .size(Size(600, 340))
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (vendor.thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model              = heroImage,
                    contentDescription = vendor.displayName,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(BRPrimaryDark, BRPrimary)
                            )
                        )
                )
            }
            // Dark gradient overlay
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.68f)),
                            startY = 80f
                        )
                    )
            )
            // Content
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BRPrimary
                ) {
                    Text(
                        "⭐ Top Rated  ${vendor.rating}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    vendor.displayName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        vendor.emoji,
                        fontSize = 12.sp
                    )
                    Text(
                        "${vendor.category}  •  ${vendor.status}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }
        }
    }
}

// ── Vendor row card ───────────────────────────────────────────────────────────

@Composable
fun VendorCard(vendor: Vendor, onClick: () -> Unit) {
    val context = LocalContext.current
    val thumbRequest = remember(vendor.thumbnailUrl) {
        ImageRequest.Builder(context)
            .data(vendor.thumbnailUrl.takeIf { it.isNotEmpty() })
            .size(Size(140, 140))
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BRSurface),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(BRSurfaceVariant, BRPrimary.copy(alpha = 0.35f))
            )
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail or emoji fallback
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(vendor.statusColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                if (vendor.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model              = thumbRequest,
                        contentDescription = vendor.displayName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                } else {
                    Text(vendor.emoji, fontSize = 28.sp)
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    vendor.displayName,
                    style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = BRStar, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${vendor.rating}  ·  ${vendor.category}",
                        style = MaterialTheme.typography.bodySmall.copy(color = BRSubtext)
                    )
                }
                Spacer(Modifier.height(5.dp))
                // Status pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (vendor.status.startsWith("Open"))
                        BRGreen.copy(alpha = 0.12f) else BRAmber.copy(alpha = 0.12f)
                ) {
                    Text(
                        vendor.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (vendor.status.startsWith("Open")) BRGreen else BRAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open ${vendor.displayName}",
                tint   = BRPrimaryDark,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Shared filter chip ────────────────────────────────────────────────────────

@Composable
fun BRFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick  = onClick,
        label    = { Text(label, style = MaterialTheme.typography.labelMedium) },
        shape    = RoundedCornerShape(20.dp),
        colors   = FilterChipDefaults.filterChipColors(
            selectedContainerColor = BRPrimary,
            selectedLabelColor     = Color.White,
            containerColor         = BRSurface,
            labelColor             = BROnBackground
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled             = true,
            selected            = selected,
            borderColor         = BRSurfaceVariant,
            selectedBorderColor = Color.Transparent
        )
    )
}

// ── Bottom navigation bar ─────────────────────────────────────────────────────

@Composable
fun BottomNavBar(
    currentRoute: String,
    onHome: () -> Unit,
    onBrowse: () -> Unit,
    onCart: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit = {}
) {
    NavigationBar(
        containerColor = BRSurface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick  = onHome,
            icon     = { Icon(Icons.Default.Home, "Home") },
            label    = { Text("Home") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = BRPrimary,
                selectedTextColor   = BRPrimary,
                indicatorColor      = BRPrimaryLight,
                unselectedIconColor = BRSubtext,
                unselectedTextColor = BRSubtext
            )
        )
        NavigationBarItem(
            selected = currentRoute == "browse",
            onClick  = onBrowse,
            icon     = { Icon(Icons.Default.Search, "Browse") },
            label    = { Text("Browse") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = BRPrimary,
                selectedTextColor   = BRPrimary,
                indicatorColor      = BRPrimaryLight,
                unselectedIconColor = BRSubtext,
                unselectedTextColor = BRSubtext
            )
        )
        NavigationBarItem(
            selected = currentRoute == "cart",
            onClick  = onCart,
            icon     = { Icon(Icons.Default.ShoppingCart, "Cart") },
            label    = { Text("Cart") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = BRPrimary,
                selectedTextColor   = BRPrimary,
                indicatorColor      = BRPrimaryLight,
                unselectedIconColor = BRSubtext,
                unselectedTextColor = BRSubtext
            )
        )
        NavigationBarItem(
            selected = currentRoute == "history",
            onClick  = onHistory,
            icon     = { Icon(Icons.AutoMirrored.Filled.List, "Orders") },
            label    = { Text("Orders") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = BRPrimary,
                selectedTextColor   = BRPrimary,
                indicatorColor      = BRPrimaryLight,
                unselectedIconColor = BRSubtext,
                unselectedTextColor = BRSubtext
            )
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick  = onProfile,
            icon     = { Icon(Icons.Default.EmojiEvents, "Profile") },
            label    = { Text("Rewards") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = BRPrimary,
                selectedTextColor   = BRPrimary,
                indicatorColor      = BRPrimaryLight,
                unselectedIconColor = BRSubtext,
                unselectedTextColor = BRSubtext
            )
        )
    }
}
