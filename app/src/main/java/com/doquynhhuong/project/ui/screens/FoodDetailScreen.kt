package com.doquynhhuong.project.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.doquynhhuong.project.network.MealDetail
import com.doquynhhuong.project.models.MenuItem
import com.doquynhhuong.project.models.vendorNameForCategory
import com.doquynhhuong.project.viewmodels.FoodBrowseViewModel
import com.doquynhhuong.project.viewmodels.FoodBrowseViewModelFactory
import com.doquynhhuong.project.viewmodels.OrderViewModel
import com.doquynhhuong.project.viewmodels.OrderViewModelFactory
import com.doquynhhuong.project.models.UiState

/**
 * Meal Detail screen — loads a single meal from the API,
 * shows its image, area, tags, ingredients, and instructions,
 * and lets the user add it to the Galactic Greens cart.
 *
 * Route: "foodDetail/{mealId}"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailScreen(
    mealId: String,
    navController: NavController
) {
    val activity = LocalContext.current as ComponentActivity
    val application = activity.applicationContext as Application
    val browseFactory = remember(application) { FoodBrowseViewModelFactory(application) }
    val orderFactory = remember(application) { OrderViewModelFactory(application) }
    val browseViewModel: FoodBrowseViewModel = viewModel(
        factory = browseFactory,
        viewModelStoreOwner = activity
    )
    val orderViewModel: OrderViewModel = viewModel(
        factory = orderFactory,
        viewModelStoreOwner = activity
    )
    val state by browseViewModel.detailState.collectAsState()
    var addedToCart by remember { mutableStateOf(false) }
    var snackbarVisible by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger load when the screen mounts
    LaunchedEffect(mealId) {
        browseViewModel.loadMealDetail(mealId)
        addedToCart = false
    }

    // Show snackbar confirmation after add
    LaunchedEffect(snackbarVisible) {
        if (snackbarVisible) {
            snackbarHostState.showSnackbar(
                message = "Added to cart!",
                actionLabel = "View Cart",
                duration = SnackbarDuration.Short
            ).let { result ->
                if (result == SnackbarResult.ActionPerformed) {
                    navController.navigate("cart")
                }
            }
            snackbarVisible = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (val s = state) {
                            is UiState.Success -> s.data?.name ?: "Meal Detail"
                            else -> "Meal Detail"
                        },
                        maxLines = 1,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            // Only show Add to Cart when we have data
            if (state is UiState.Success && (state as UiState.Success).data != null) {
                val meal = (state as UiState.Success<MealDetail?>).data!!
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Button(
                        onClick = {
                            // Convert meal to a MenuItem and add to the Galactic Greens cart
                            val cartItem = MenuItem(
                                name = meal.name,
                                price = "\$12", // fixed price for API meals
                                description = "${meal.area} • ${meal.category}",
                                emoji = "🍽️",
                                vendorName = vendorNameForCategory(meal.category)
                            )
                            if (!orderViewModel.addToCart(cartItem)) {
                                navController.navigate("login") { launchSingleTop = true }
                            } else {
                                addedToCart = true
                                snackbarVisible = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (addedToCart)
                            ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        else ButtonDefaults.buttonColors()
                    ) {
                        Icon(Icons.Default.ShoppingCart, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (addedToCart) "Added to Cart ✓" else "Add to Cart — \$12.00",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                is UiState.Loading -> {
                    Column(modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading meal…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is UiState.Error -> {
                    ErrorCard(
                        message = s.message,
                        onRetry = { browseViewModel.loadMealDetail(mealId) },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                }
                is UiState.Success -> {
                    val meal = s.data
                    if (meal == null) {
                        Text("Meal not found.",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        MealDetailContent(meal = meal)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDetailContent(meal: MealDetail) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Hero image
        item {
            AsyncImage(
                model = meal.thumbnail,
                contentDescription = meal.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(260.dp)
            )
        }

        // Title + meta row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text(meal.name, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(meal.category) },
                        leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = {},
                        label = { Text(meal.area) },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp)) }
                    )
                }
                // Tags
                if (!meal.tags.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        meal.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    tag.trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Ingredients section
        item {
            SectionHeader(title = "Ingredients")
        }

        val ingredients = meal.ingredientList()
        items(ingredients) { (ingredient, measure) ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(ingredient, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    measure,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Instructions section
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader(title = "Instructions")
            Text(
                text = meal.instructions.trim(),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
