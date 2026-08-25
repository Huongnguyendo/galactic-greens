package com.doquynhhuong.project.ui.screens

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI test for [BottomNavBar], the primary cross-screen navigation control
 * used from Home, Cart, Order History, and Profile. Verifies that tapping
 * each destination invokes the correct callback — the critical navigational
 * flow a user relies on to move around the app.
 */
class BottomNavBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingEachDestination_invokesItsOwnCallbackOnly() {
        var homeClicks = 0
        var browseClicks = 0
        var cartClicks = 0
        var historyClicks = 0
        var profileClicks = 0

        composeRule.setContent {
            BottomNavBar(
                currentRoute = "home",
                onHome = { homeClicks++ },
                onBrowse = { browseClicks++ },
                onCart = { cartClicks++ },
                onHistory = { historyClicks++ },
                onProfile = { profileClicks++ }
            )
        }

        composeRule.onNodeWithText("Browse").performClick()
        composeRule.onNodeWithText("Cart").performClick()
        composeRule.onNodeWithText("Orders").performClick()
        composeRule.onNodeWithText("Rewards").performClick()

        assertEquals("Home should not have been tapped", 0, homeClicks)
        assertEquals("Browse should register exactly one tap", 1, browseClicks)
        assertEquals("Cart should register exactly one tap", 1, cartClicks)
        assertEquals("History (Orders) should register exactly one tap", 1, historyClicks)
        assertEquals("Profile (Rewards) should register exactly one tap", 1, profileClicks)
    }

    @Test
    fun homeDestination_isSelectedWhenCurrentRouteIsHome() {
        composeRule.setContent {
            BottomNavBar(
                currentRoute = "home",
                onHome = {},
                onBrowse = {},
                onCart = {},
                onHistory = {},
                onProfile = {}
            )
        }

        // Selected state is exposed to accessibility services via the
        // "selected" semantics property on NavigationBarItem.
        composeRule.onNodeWithText("Home").assertIsSelected()
    }
}
