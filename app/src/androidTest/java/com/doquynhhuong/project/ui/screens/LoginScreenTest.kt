package com.doquynhhuong.project.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for [LoginScreen] covering the critical sign-in / registration
 * entry flow. These only exercise *local* field validation (which runs
 * before any network/Firestore call), so they stay fast and hermetic.
 */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingLogInWithEmptyFields_showsValidationError() {
        composeRule.setContent {
            LoginScreen(onLoggedIn = {}, onBack = {})
        }

        // The "Log in" tab is selected by default. "Log in" also labels the
        // submit button below, so target the last match (the button) explicitly.
        composeRule.onAllNodesWithText("Log in").onLast().performClick()

        composeRule.onNodeWithText("Enter email and password.")
            .assertExists("Expected empty-field validation error to be shown")
    }

    @Test
    fun switchingToRegisterTab_showsRegistrationFieldsAndValidatesThem() {
        composeRule.setContent {
            LoginScreen(onLoggedIn = {}, onBack = {})
        }

        // Navigate to the Register tab — critical part of the sign-up flow.
        composeRule.onNodeWithText("Register").performClick()

        // Registration fields should now be visible.
        composeRule.onNodeWithText("Display name")
            .assertExists("Register tab should show the display name field")
        composeRule.onNodeWithText("Confirm password")
            .assertExists("Register tab should show the confirm password field")

        // Fill in a name only, then try to submit — should be rejected for a bad email.
        composeRule.onNodeWithText("Display name").performTextInput("Jamie")
        composeRule.onNodeWithText("Create account & sign in").performClick()

        composeRule.onNodeWithText("Enter a valid email.")
            .assertExists("Expected invalid-email validation error to be shown")
    }
}
