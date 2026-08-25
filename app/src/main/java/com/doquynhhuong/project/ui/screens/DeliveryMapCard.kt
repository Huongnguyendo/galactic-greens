package com.doquynhhuong.project.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doquynhhuong.project.location.distanceMeters
import com.doquynhhuong.project.location.formatDistance
import com.doquynhhuong.project.location.rememberCurrentLocation
import com.doquynhhuong.project.models.UiState
import com.doquynhhuong.project.models.Vendor
import com.doquynhhuong.project.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PatternItem
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

/**
 * Space-delivery simulation shown on the Confirm Order screen using Google Maps
 * (Maps Compose). Requires a Maps API key set in AndroidManifest.xml.
 *
 * Features:
 *   - Vendor pins + highlighted drop-off pin
 *   - Live "my location" dot that updates continuously (not one-shot),
 *     via Google Maps' built-in MapProperties(isMyLocationEnabled = true)
 *   - A dashed route line from the user's current location to the drop-off
 *   - Distance rings (radius circles) around the drop-off point, so you can
 *     see at a glance which vendors fall within ~150m / ~300m / ~500m
 *   - A floating "center on me" button that also toggles auto-follow mode —
 *     while following, the camera smoothly pans to track your live position
 *   - Animated camera pans (CameraUpdateFactory + animate) when switching
 *     space destinations or re-centering, instead of hard jumps
 */
@Composable
fun DeliveryMapCard(
    vendorsState: UiState<List<Vendor>>,
    selectedDestination: SpaceDestination
) {
    var permissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    val locationState = rememberCurrentLocation(permissionGrantedTrigger = permissionGranted)
    val vendors = (vendorsState as? UiState.Success)?.data.orEmpty()

    // Auto-follow: when on, the camera re-centers on the user's live
    // position every time it updates. Tapping the button while already
    // following simply re-centers (handy if you've manually panned away).
    var autoFollow by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(selectedDestination.latitude, selectedDestination.longitude),
            15.5f
        )
    }
    val coroutineScope = rememberCoroutineScope()

    // Pulsing animation for the nearest-vendor highlight ring
    val infiniteTransition = rememberInfiniteTransition(label = "nearestVendorPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val nearestVendor = remember(vendors, selectedDestination) {
        vendors.minByOrNull {
            distanceMeters(selectedDestination.latitude, selectedDestination.longitude, it.latitude, it.longitude)
        }
    }

    // Smooth camera pan whenever the selected drop-off changes
    LaunchedEffect(selectedDestination) {
        autoFollow = false
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(selectedDestination.latitude, selectedDestination.longitude),
                15.5f
            )
        )
    }

    // Auto-follow: smoothly pan to the user's live position as it updates
    LaunchedEffect(autoFollow, locationState.latitude, locationState.longitude) {
        if (!autoFollow) return@LaunchedEffect
        val lat = locationState.latitude ?: return@LaunchedEffect
        val lng = locationState.longitude ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(LatLng(lat, lng)))
    }

    val dashedPattern: List<PatternItem> = remember { listOf(Dash(20f), Gap(14f)) }

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BRSurface),
        border = BorderStroke(1.dp, BRSurfaceVariant),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "✦  ORBITAL DELIVERY MAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = BRPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Nearby Space Kitchens",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(selectedDestination.label, style = MaterialTheme.typography.bodySmall, color = BRSubtext)
                }
                if (!locationState.hasPermission) {
                    TextButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }) {
                        Text("Use my location")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = locationState.hasPermission),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false // we use our own animated FAB instead
                    )
                ) {
                    // ── Distance rings around the drop-off point ───────────
                    val dropOffLatLng = LatLng(selectedDestination.latitude, selectedDestination.longitude)
                    Circle(
                        center = dropOffLatLng,
                        radius = 150.0,
                        fillColor = Color(0x338B5CF6),
                        strokeColor = Color(0x998B5CF6),
                        strokeWidth = 1.5f
                    )
                    Circle(
                        center = dropOffLatLng,
                        radius = 300.0,
                        fillColor = Color(0x22C72C5C),
                        strokeColor = Color(0x88C72C5C),
                        strokeWidth = 1.5f
                    )
                    Circle(
                        center = dropOffLatLng,
                        radius = 500.0,
                        fillColor = Color(0x148B5CF6),
                        strokeColor = Color(0x668B5CF6),
                        strokeWidth = 1.5f
                    )

                    // ── Drop-off marker (highlighted) ───────────────────────
                    Marker(
                        state = MarkerState(position = dropOffLatLng),
                        title = selectedDestination.label,
                        snippet = "Your drop-off point",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_VIOLET
                        )
                    )

                    // ── Vendor markers + pulsing highlight on the nearest one ─
                    vendors.forEach { vendor ->
                        val meters = distanceMeters(
                            selectedDestination.latitude, selectedDestination.longitude,
                            vendor.latitude, vendor.longitude
                        )
                        val vendorLatLng = LatLng(vendor.latitude, vendor.longitude)

                        if (vendor === nearestVendor) {
                            Circle(
                                center = vendorLatLng,
                                radius = 18.0 * pulseScale,
                                fillColor = Color(0x55FFC107),
                                strokeColor = Color(0xFFFFC107),
                                strokeWidth = 2f
                            )
                        }

                        Marker(
                            state = MarkerState(position = vendorLatLng),
                            title = vendor.displayName,
                            snippet = "${formatDistance(meters)} from drop-off",
                            icon = if (vendor === nearestVendor) {
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                            } else {
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            }
                        )
                    }

                    // ── Dashed route line from current location to drop-off ─
                    if (locationState.latitude != null && locationState.longitude != null) {
                        Polyline(
                            points = listOf(
                                LatLng(locationState.latitude, locationState.longitude),
                                dropOffLatLng
                            ),
                            color = Color(0xFFC72C5C),
                            width = 5f,
                            pattern = dashedPattern
                        )
                    }
                }

                // Floating "center on me" / auto-follow toggle button
                if (locationState.hasPermission) {
                    val isFollowing = autoFollow
                    FloatingActionButton(
                        onClick = {
                            autoFollow = true
                            val lat = locationState.latitude
                            val lng = locationState.longitude
                            if (lat != null && lng != null) {
                                coroutineScope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 17f)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(40.dp),
                        containerColor = if (isFollowing) BRPrimary else BRSurface,
                        contentColor = if (isFollowing) Color.White else BRPrimaryDark,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Center on my location")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Legend for the distance rings
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0x998B5CF6))
                Text("150m", style = MaterialTheme.typography.labelSmall, color = BRSubtext)
                LegendDot(color = Color(0x88C72C5C))
                Text("300m", style = MaterialTheme.typography.labelSmall, color = BRSubtext)
                LegendDot(color = Color(0x668B5CF6))
                Text("500m", style = MaterialTheme.typography.labelSmall, color = BRSubtext)
            }

            Spacer(Modifier.height(8.dp))

            // Distance from the user's current location to the drop-off point
            if (locationState.latitude != null && locationState.longitude != null) {
                val distanceToDropoff = distanceMeters(
                    locationState.latitude, locationState.longitude,
                    selectedDestination.latitude, selectedDestination.longitude
                )
                Text(
                    "You're ${formatDistance(distanceToDropoff)} from \"${selectedDestination.label}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = BRSubtext
                )
                Spacer(Modifier.height(8.dp))
            } else if (locationState.hasPermission) {
                Text(
                    "Getting your location…",
                    style = MaterialTheme.typography.bodySmall,
                    color = BRSubtext
                )
                Spacer(Modifier.height(8.dp))
            }

            // Space-kitchen distance list (sorted nearest-first)
            if (vendors.isNotEmpty()) {
                Text(
                    "Space-kitchen distance from drop-off",
                    style = MaterialTheme.typography.labelMedium,
                    color = BRSubtext
                )
                Spacer(Modifier.height(6.dp))
                val sortedVendors = remember(vendors, selectedDestination) {
                    vendors.map { vendor ->
                        vendor to distanceMeters(
                            selectedDestination.latitude, selectedDestination.longitude,
                            vendor.latitude, vendor.longitude
                        )
                    }.sortedBy { it.second }
                }
                sortedVendors.take(5).forEach { (vendor, meters) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${vendor.emoji} ${vendor.displayName}" +
                                if (vendor === nearestVendor) " ⭐" else "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (vendor === nearestVendor) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            formatDistance(meters),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = BRPrimary
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}
