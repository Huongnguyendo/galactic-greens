package com.doquynhhuong.project.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.doquynhhuong.project.ui.screens.CartScreen
import com.doquynhhuong.project.ui.screens.ConfirmOrderScreen
import com.doquynhhuong.project.ui.screens.FoodBrowseScreen
import com.doquynhhuong.project.ui.screens.FoodDetailScreen
import com.doquynhhuong.project.ui.screens.HomeScreen
import com.doquynhhuong.project.ui.screens.LoginScreen
import com.doquynhhuong.project.ui.screens.MealListScreen
import com.doquynhhuong.project.ui.screens.OrderHistoryScreen
import com.doquynhhuong.project.ui.screens.OrderTrackingScreen
import com.doquynhhuong.project.ui.screens.ProfileScreen
import com.doquynhhuong.project.ui.screens.VendorMenuScreen
import com.doquynhhuong.project.viewmodels.OrderViewModel
import com.doquynhhuong.project.viewmodels.OrderViewModelFactory
import java.net.URLDecoder

/**
 * Central navigation graph for Galactic Greens.
 *
 * Routes:
 *   home           → HomeScreen  (start destination; guest until sign-in)
 *   menu/{vendor}  → VendorMenuScreen
 *   cart           → CartScreen
 *   confirmation   → ConfirmOrderScreen
 *   tracking       → OrderTrackingScreen
 *   history        → OrderHistoryScreen
 *   browse         → FoodBrowseScreen
 *   browse/{cat}   → MealListScreen
 *   foodDetail/{id}→ FoodDetailScreen
 *   login          → LoginScreen (add-to-cart gate)
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    orderViewModel: OrderViewModel = viewModel(
        factory = OrderViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val sessionName by orderViewModel.sessionDisplayName.collectAsState()

    NavHost(
        navController    = navController,
        startDestination = "home",
        modifier         = modifier,
        enterTransition  = { EnterTransition.None },
        exitTransition   = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition  = { ExitTransition.None }
    ) {

        composable("home") {
            HomeScreen(
                navController   = navController,
                viewModel       = orderViewModel,
                userName        = sessionName.orEmpty(),
                onSignInClick   = {
                    navController.navigate("login") { launchSingleTop = true }
                },
                onSignOutClick  = { orderViewModel.logout() }
            )
        }

        composable("login") {
            LoginScreen(
                onLoggedIn = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("menu/{vendor}") { back ->
            val vendorName = URLDecoder.decode(back.arguments?.getString("vendor") ?: "", "UTF-8")
            VendorMenuScreen(
                vendorName    = vendorName,
                viewModel     = orderViewModel,
                navController = navController,
                onNextClick   = { navController.navigate("cart") }
            )
        }

        composable("cart") {
            CartScreen(viewModel = orderViewModel, navController = navController)
        }

        composable("confirmation") {
            ConfirmOrderScreen(viewModel = orderViewModel, navController = navController)
        }

        composable("tracking") {
            OrderTrackingScreen(navController = navController, viewModel = orderViewModel)
        }

        composable("history") {
            OrderHistoryScreen(navController = navController, viewModel = orderViewModel)
        }

        composable("profile") {
            ProfileScreen(navController = navController, viewModel = orderViewModel)
        }

        // ── Browse / API explore flow ────────────────────────────────────
        composable("browse") {
            FoodBrowseScreen(navController = navController)
        }

        composable("browse/{category}") { back ->
            val category = URLDecoder.decode(back.arguments?.getString("category") ?: "", "UTF-8")
            MealListScreen(
                category      = category,
                navController = navController
            )
        }

        composable("foodDetail/{mealId}") { back ->
            val mealId = back.arguments?.getString("mealId") ?: ""
            FoodDetailScreen(
                mealId        = mealId,
                navController = navController
            )
        }
    }
}
