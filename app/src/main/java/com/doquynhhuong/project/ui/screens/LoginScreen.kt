package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doquynhhuong.project.data.UserPreferences
import com.doquynhhuong.project.data.repository.UserRepository
import com.doquynhhuong.project.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    // Login fields
    var loginEmail    by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPassVisible by remember { mutableStateOf(false) }
    var loginError    by remember { mutableStateOf("") }
    var loginLoading  by remember { mutableStateOf(false) }

    // Register fields
    var regName           by remember { mutableStateOf("") }
    var regEmail          by remember { mutableStateOf("") }
    var regPassword       by remember { mutableStateOf("") }
    var regConfirm        by remember { mutableStateOf("") }
    var regPassVisible    by remember { mutableStateOf(false) }
    var regConfirmVisible by remember { mutableStateOf(false) }
    var regError          by remember { mutableStateOf("") }
    var regLoading        by remember { mutableStateOf(false) }

    val context  = LocalContext.current.applicationContext
    val prefs    = remember { UserPreferences(context) }
    val userRepo = remember { UserRepository() }
    val scope    = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign in to order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor         = BRPrimary,
                    titleContentColor      = BROnPrimary,
                    navigationIconContentColor = BROnPrimary
                )
            )
        },
        containerColor = BRBackground
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Add items to your cart after signing in. Browse the app as a guest anytime.",
                style     = MaterialTheme.typography.bodySmall,
                color     = BRSubtext,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(vertical = 12.dp)
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0; loginError = "" },
                    text     = { Text("Log in") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1; regError = "" },
                    text     = { Text("Register") }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── LOGIN TAB ──────────────────────────────────────────────────────
            if (selectedTab == 0) {
                OutlinedTextField(
                    value         = loginEmail,
                    onValueChange = { loginEmail = it; loginError = "" },
                    label         = { Text("Email") },
                    leadingIcon   = { Icon(Icons.Default.Email, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = loginPassword,
                    onValueChange = { loginPassword = it; loginError = "" },
                    label         = { Text("Password") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null) },
                    trailingIcon  = {
                        IconButton(onClick = { loginPassVisible = !loginPassVisible }) {
                            Icon(
                                if (loginPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (loginPassVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (loginPassVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth()
                )

                if (loginError.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(loginError, color = MaterialTheme.colorScheme.error,
                         style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val localErr = validateLoginFields(loginEmail, loginPassword)
                        if (localErr != null) { loginError = localErr; return@Button }

                        loginLoading = true
                        loginError   = ""
                        scope.launch {
                            try {
                                val user = userRepo.login(loginEmail, loginPassword)
                                prefs.setSessionEmail(user.email)
                                prefs.setSessionDisplayName(user.displayName)
                                onLoggedIn()
                            } catch (e: Exception) {
                                loginError = "Sign in failed. Check your email and password."
                            } finally {
                                loginLoading = false
                            }
                        }
                    },
                    enabled  = !loginLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                ) {
                    if (loginLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color    = BROnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Log in", fontWeight = FontWeight.Bold)
                    }
                }

            // ── REGISTER TAB ───────────────────────────────────────────────────
            } else {
                OutlinedTextField(
                    value         = regName,
                    onValueChange = { regName = it; regError = "" },
                    label         = { Text("Display name") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = regEmail,
                    onValueChange = { regEmail = it; regError = "" },
                    label         = { Text("Email") },
                    leadingIcon   = { Icon(Icons.Default.Email, null) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = regPassword,
                    onValueChange = { regPassword = it; regError = "" },
                    label         = { Text("Password") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null) },
                    trailingIcon  = {
                        IconButton(onClick = { regPassVisible = !regPassVisible }) {
                            Icon(
                                if (regPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (regPassVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (regPassVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = regConfirm,
                    onValueChange = { regConfirm = it; regError = "" },
                    label         = { Text("Confirm password") },
                    leadingIcon   = { Icon(Icons.Default.Lock, null) },
                    trailingIcon  = {
                        IconButton(onClick = { regConfirmVisible = !regConfirmVisible }) {
                            Icon(
                                if (regConfirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (regConfirmVisible) "Hide confirm password" else "Show confirm password"
                            )
                        }
                    },
                    visualTransformation = if (regConfirmVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier   = Modifier.fillMaxWidth()
                )

                if (regError.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(regError, color = MaterialTheme.colorScheme.error,
                         style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val localErr = validateRegisterFields(regName, regEmail, regPassword, regConfirm)
                        if (localErr != null) { regError = localErr; return@Button }

                        regLoading = true
                        regError   = ""
                        scope.launch {
                            try {
                                // Firebase Auth stores the password; Firestore stores profile data only.
                                val err = userRepo.register(
                                    email       = regEmail.trim(),
                                    displayName = regName.trim(),
                                    password    = regPassword
                                )
                                if (err != null) {
                                    regError = err
                                } else {
                                    val key = regEmail.trim().lowercase()
                                    prefs.setSessionEmail(key)
                                    prefs.setSessionDisplayName(regName.trim())
                                    onLoggedIn()
                                }
                            } catch (e: Exception) {
                                regError = "Could not connect. Check your internet connection."
                            } finally {
                                regLoading = false
                            }
                        }
                    },
                    enabled  = !regLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                ) {
                    if (regLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color    = BROnPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create account & sign in", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun validateLoginFields(email: String, password: String): String? {
    if (email.isBlank() || password.isBlank()) return "Enter email and password."
    return null
}

private fun validateRegisterFields(
    name: String, email: String, password: String, confirm: String
): String? {
    if (name.isBlank())            return "Enter your name."
    if (!email.contains("@"))      return "Enter a valid email."
    if (password.length < 4)       return "Password must be at least 4 characters."
    if (password != confirm)       return "Passwords do not match."
    return null
}
