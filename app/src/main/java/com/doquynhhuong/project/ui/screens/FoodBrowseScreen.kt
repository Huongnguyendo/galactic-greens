package com.doquynhhuong.project.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.doquynhhuong.project.network.MealCategory
import com.doquynhhuong.project.viewmodels.FoodBrowseViewModel
import com.doquynhhuong.project.viewmodels.FoodBrowseViewModelFactory
import com.doquynhhuong.project.models.UiState
import com.doquynhhuong.project.network.MealSummary
import com.doquynhhuong.project.ui.theme.BRBackground
import com.doquynhhuong.project.ui.theme.BROnPrimary
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurface
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ─────────────────────────────────────────────
// Category grid screen  (route: "browse")
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodBrowseScreen(
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val browseFactory = remember(application) { FoodBrowseViewModelFactory(application) }
    val viewModel: FoodBrowseViewModel = viewModel(
        factory = browseFactory,
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val state by viewModel.categoriesState.collectAsState()

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column(Modifier.padding(end = 8.dp)) {
                        Text(
                            "Explore foods",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BROnPrimary
                        )
                        Text(
                            "Recipes & ideas · TheMealDB",
                            style = MaterialTheme.typography.labelMedium,
                            color = BROnPrimary.copy(alpha = 0.82f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BROnPrimary)
                    }
                },
                actions = {
                    if (state is UiState.Error) {
                        IconButton(onClick = { viewModel.loadCategories() }) {
                            Icon(Icons.Default.Refresh, "Retry", tint = BROnPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BRPrimary,
                    titleContentColor = BROnPrimary,
                    navigationIconContentColor = BROnPrimary,
                    actionIconContentColor = BROnPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is UiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BRPrimary, strokeWidth = 3.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Loading categories…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BRSubtext
                        )
                    }
                }
                is UiState.Error -> {
                    ErrorCard(message = s.message, onRetry = { viewModel.loadCategories() },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp))
                }
                is UiState.Success -> {
                    CategoryGrid(
                        categories = s.data,
                        onCategoryClick = { category ->
                            val encoded = URLEncoder.encode(category.name, StandardCharsets.UTF_8.toString())
                            navController.navigate("browse/$encoded")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<MealCategory>,
    onCategoryClick: (MealCategory) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Tap a tile to see meals",
                    style = MaterialTheme.typography.bodySmall,
                    color = BRSubtext
                )
            }
        }
        items(
            items = categories,
            key = { it.name }
        ) { category ->
            CategoryCard(category = category, onClick = { onCategoryClick(category) })
        }
    }
}

@Composable
private fun CategoryCard(category: MealCategory, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageRequest = remember(category.thumbnail) {
        ImageRequest.Builder(context)
            .data(category.thumbnail)
            .allowHardware(true)
            .crossfade(true)
            .size(Size(280, 280))
            .build()
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = BRSurface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.45f to Color.Transparent,
                                0.75f to Color.Black.copy(alpha = 0.35f),
                                1f to Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Meal list screen  (route: "browse/{category}")
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealListScreen(
    category: String,
    navController: NavController
) {
    val application = LocalContext.current.applicationContext as Application
    val browseFactory = remember(application) { FoodBrowseViewModelFactory(application) }
    val viewModel: FoodBrowseViewModel = viewModel(
        factory = browseFactory,
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    val state         by viewModel.mealsState.collectAsState()
    val sortAZ        by viewModel.sortAZ.collectAsState()
    val dietaryFilter by viewModel.dietaryFilter.collectAsState()
    val searchQuery   by viewModel.searchQuery.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }

    // Load meals when the screen first appears for this category
    LaunchedEffect(category) {
        viewModel.loadMealsByCategory(category)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Sort toggle
                    IconButton(onClick = { viewModel.setSortAZ(!sortAZ) }) {
                        Icon(Icons.Default.KeyboardArrowDown, "Sort",
                            tint = if (sortAZ) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.onPrimary)
                    }
                    // Filter sheet
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Menu, "Filter",
                            tint = if (dietaryFilter != "All") MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchMeals(it) },
                placeholder = { Text("Search meals…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp)
            )

            // Active filter chip
            if (dietaryFilter != "All") {
                Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.setDietaryFilter("All") },
                        label = { Text("Filter: $dietaryFilter  ✕") }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val s = state) {
                    is UiState.Loading -> {
                        Column(modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text("Loading meals…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is UiState.Error -> {
                        ErrorCard(message = s.message,
                            onRetry = { viewModel.loadMealsByCategory(category) },
                            modifier = Modifier.align(Alignment.Center).padding(24.dp))
                    }
                    is UiState.Success -> {
                        if (s.data.isEmpty()) {
                            Column(modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🍽️", fontSize = 48.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No meals found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            MealGrid(
                                meals = s.data,
                                onMealClick = { meal ->
                                    navController.navigate("foodDetail/${meal.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet for dietary filter preference
    if (showFilterSheet) {
        PreferenceBottomSheet(
            currentFilter = dietaryFilter,
            sortAZ = sortAZ,
            onFilterSelected = { viewModel.setDietaryFilter(it) },
            onSortToggle = { viewModel.setSortAZ(!sortAZ) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun MealGrid(meals: List<MealSummary>, onMealClick: (MealSummary) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = meals,
            key = { it.id }
        ) { meal ->
            MealCard(meal = meal, onClick = { onMealClick(meal) })
        }
    }
}

@Composable
private fun MealCard(meal: MealSummary, onClick: () -> Unit) {
    val context = LocalContext.current
    val mealImage = remember(meal.id, meal.thumbnail) {
        ImageRequest.Builder(context)
            .data(meal.thumbnail)
            .size(Size(320, 210))
            .allowHardware(true)
            .crossfade(true)
            .build()
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = mealImage,
                contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(130.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = meal.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Preferences bottom sheet
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferenceBottomSheet(
    currentFilter: String,
    sortAZ: Boolean,
    onFilterSelected: (String) -> Unit,
    onSortToggle: () -> Unit,
    onDismiss: () -> Unit
) {
    val filters = listOf(
        "All", "Breakfast", "Dessert", "Pasta", "Side", "Starter", "Vegan", "Vegetarian"
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Preferences", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // Dietary filter
            Text("Dietary Filter", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            filters.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { onFilterSelected(filter); onDismiss() },
                            label = { Text(filter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // pad last row if odd count
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Sort toggle
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Sort A→Z", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                    Text("Sort meals alphabetically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = sortAZ, onCheckedChange = { onSortToggle() })
            }
        }
    }
}

// ─────────────────────────────────────────────
// Shared error composable
// ─────────────────────────────────────────────

@Composable
fun ErrorCard(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠️", fontSize = 40.sp)
            Spacer(Modifier.height(8.dp))
            Text("Something went wrong",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(4.dp))
            Text(message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Retry")
            }
        }
    }
}
