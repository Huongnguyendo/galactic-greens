package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.doquynhhuong.project.domain.checkout.CheckoutPricing
import com.doquynhhuong.project.domain.checkout.VendorPrepEstimator
import com.doquynhhuong.project.models.OrbitalDeliveryTiming
import com.doquynhhuong.project.ui.theme.BRBackground
import com.doquynhhuong.project.ui.theme.BROnPrimary
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurface
import com.doquynhhuong.project.ui.theme.BRSurfaceVariant
import com.doquynhhuong.project.viewmodels.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmOrderScreen(viewModel: OrderViewModel, navController: NavController) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val currentVendor by viewModel.currentVendor.collectAsState()
    val vendorsState by viewModel.vendorsState.collectAsState()
    val vendorsSummary = remember(selectedItems, currentVendor) {
        val distinct = selectedItems.map { it.vendorName.trim() }.filter { it.isNotEmpty() }.distinct()
        when {
            distinct.isNotEmpty() -> distinct.joinToString(", ")
            currentVendor.isNotBlank() -> currentVendor
            else -> ""
        }
    }
    val linesByVendor = remember(selectedItems) {
        selectedItems.groupBy { item ->
            item.vendorName.trim().ifBlank { "Other" }
        }
    }

    val vendorPrepMinutes = remember(selectedItems, currentVendor) {
        VendorPrepEstimator.estimateKitchenPrepMinutes(selectedItems, currentVendor)
    }

    var selectedDestination by remember { mutableStateOf(SPACE_DESTINATIONS.first()) }
    var selectedTime by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerKey by remember { mutableIntStateOf(0) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val subtotal = remember(selectedItems) { CheckoutPricing.subtotal(selectedItems) }
    val deliveryFee = CheckoutPricing.DELIVERY_FEE
    val total = remember(selectedItems) { CheckoutPricing.total(selectedItems) }

    if (showTimePicker) {
        key(timePickerKey) {
            DeliveryTimePickerDialog(
                initialTime = selectedTime,
                vendorPrepMinutes = vendorPrepMinutes,
                dialogKey = timePickerKey,
                onDismiss = { showTimePicker = false },
                onConfirm = { hour, minute ->
                    selectedTime = "%02d:%02d".format(hour, minute)
                    showTimePicker = false
                }
            )
        }
    }

    if (showSuccessDialog) {
        OrderPlacedSuccessDialog(
            vendorSummary = vendorsSummary,
            selectedTime = selectedTime,
            dropOffLocation = selectedDestination.label,
            onTrackOrder = {
                showSuccessDialog = false
                viewModel.saveOrderToHistory(
                    dropOffLocation = selectedDestination.label,
                    onComplete = {
                        navController.navigate("tracking") { popUpTo("home") { inclusive = false } }
                    }
                )
            },
            onViewHistory = {
                showSuccessDialog = false
                viewModel.saveOrderToHistory(
                    dropOffLocation = selectedDestination.label,
                    onComplete = {
                        navController.navigate("history") { popUpTo("home") { inclusive = false } }
                    }
                )
            }
        )
    }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = { Text("Confirm Order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BRPrimary,
                    titleContentColor = BROnPrimary,
                    navigationIconContentColor = BROnPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                if (vendorsSummary.isNotEmpty()) {
                    Text(
                        "Order from $vendorsSummary",
                        style = MaterialTheme.typography.labelLarge.copy(color = BRSubtext)
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Order Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        linesByVendor.forEach { (vendor, lines) ->
                            Text(
                                vendor,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BRSubtext
                                )
                            )
                            Spacer(Modifier.height(6.dp))
                            lines.forEach { item ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "• ${item.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        item.price,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = BRPrimary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Delivery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Orbital delivery · +\$${"%.2f".format(CheckoutPricing.DELIVERY_FEE)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BRSubtext
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Drop-off location",
                            style = MaterialTheme.typography.labelMedium,
                            color = BRSubtext
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = BRPrimary,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                "Planet / station",
                                style = MaterialTheme.typography.labelLarge,
                                color = BRSubtext
                            )
                        }
                        SPACE_DESTINATIONS.forEach { place ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDestination = place
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedDestination == place,
                                    onClick = { selectedDestination = place },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = BRPrimary
                                    )
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    place.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            item {
                DeliveryMapCard(
                    vendorsState = vendorsState,
                    selectedDestination = selectedDestination
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Delivery Time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                timePickerKey++
                                showTimePicker = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedTime.isEmpty()) BRSubtext else BRPrimary
                            )
                        ) {
                            Text(
                                if (selectedTime.isEmpty()) "Select Time" else "⏰  $selectedTime",
                                fontWeight = if (selectedTime.isEmpty()) FontWeight.Normal
                                else FontWeight.SemiBold
                            )
                        }
                        if (selectedTime.isEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                buildString {
                                    append("Tap to set a time. ")
                                    append(
                                        "Earliest time adds kitchen wait + " +
                                            "${OrbitalDeliveryTiming.ORBITAL_DELIVERY_MINUTES} min orbital delivery. "
                                    )
                                    append("Space kitchens run ${OrbitalDeliveryTiming.SERVICE_START_H}:00–${OrbitalDeliveryTiming.SERVICE_END_H}:00. ")
                                    append("Open the dialog to see your earliest slot.")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = BRSubtext
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "\$${"%.2f".format(subtotal)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "\$${"%.2f".format(deliveryFee)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        HorizontalDivider(
                            Modifier.padding(vertical = 8.dp),
                            color = BRSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "\$${"%.2f".format(total)}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BRPrimary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (selectedTime.isNotEmpty() && selectedItems.isNotEmpty())
                            showSuccessDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = selectedTime.isNotEmpty() && selectedItems.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                ) {
                    Text(
                        "Place Order",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                if (selectedTime.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Please select a delivery time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
