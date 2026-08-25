package com.doquynhhuong.project.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.doquynhhuong.project.data.DeliveryPhotoStore
import com.doquynhhuong.project.domain.rewards.PointsCalculator
import com.doquynhhuong.project.ui.theme.BROnBackground
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRPrimaryLight
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurface

/**
 * Lets the customer snap one or more photos of the food once it's delivered.
 * Opens an embedded CameraX preview and keeps a local preview. The caller can
 * upload each captured file and pass its persisted Firebase URLs back through
 * [remotePhotoUrls].
 *
 * Reports the current photo count back via [onPhotoCountChanged] after every
 * successful capture, so the caller can award points (1-5 photos = 10 pts,
 * 6-10 photos = 20 pts, capped at 20 pts for this order).
 */
@Composable
fun DeliveryPhotoCard(
    orderKey: String,
    modifier: Modifier = Modifier,
    remotePhotoUrls: List<String> = emptyList(),
    onPhotoCountChanged: (Int) -> Unit = {},
    onPhotoCaptured: (java.io.File) -> Unit = {}
) {
    if (orderKey.isBlank()) return
    val context = LocalContext.current

    var photoCount by remember(orderKey) {
        mutableIntStateOf(DeliveryPhotoStore.photoCount(context, orderKey))
    }
    var refreshTick by remember(orderKey) { mutableIntStateOf(0) }
    var showCamera by remember(orderKey) { mutableStateOf(false) }

    LaunchedEffect(remotePhotoUrls.size) {
        photoCount = maxOf(photoCount, remotePhotoUrls.size)
    }

    fun handleCapturedPhoto(saved: java.io.File) {
        photoCount = maxOf(photoCount, remotePhotoUrls.size) + 1
        refreshTick++
        onPhotoCountChanged(photoCount)
        onPhotoCaptured(saved)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) showCamera = true
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (showCamera) {
        CameraXCaptureDialog(
            orderKey = orderKey,
            onDismiss = { showCamera = false },
            onPhotoCaptured = { file ->
                showCamera = false
                handleCapturedPhoto(file)
            }
        )
    }

    val persistedPhotoCount = maxOf(photoCount, remotePhotoUrls.size)
    val earnedPoints = PointsCalculator.pointsForPhotoCount(persistedPhotoCount)
    val photos = remember(orderKey, refreshTick) { DeliveryPhotoStore.listPhotos(context, orderKey) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = BRSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Delivery photos",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = BROnBackground
                )
                if (earnedPoints > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BRPrimaryLight
                    ) {
                        Text(
                            "+$earnedPoints pts",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = BRPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Snap photos of your food when it arrives. Photos are saved to your order. " +
                    "1-5 photos = 10 pts, 6-10 photos = 20 pts (max per order).",
                style = MaterialTheme.typography.bodySmall,
                color = BRSubtext
            )
            Spacer(Modifier.height(12.dp))

            if (photos.isNotEmpty() || remotePhotoUrls.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(photos) { file ->
                        AsyncImage(
                            model = DeliveryPhotoStore.uriFor(context, file),
                            contentDescription = "Photo of your delivered food",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    items(remotePhotoUrls, key = { it }) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Photo saved with your order",
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { launchCamera() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add another photo")
                }
            } else {
                Button(
                    onClick = { launchCamera() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BRPrimary)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Take delivery photo", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
