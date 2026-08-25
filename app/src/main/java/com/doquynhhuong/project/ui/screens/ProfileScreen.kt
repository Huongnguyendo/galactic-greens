package com.doquynhhuong.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.doquynhhuong.project.domain.rewards.BadgeTier
import com.doquynhhuong.project.ui.theme.BRAmber
import com.doquynhhuong.project.ui.theme.BRBackground
import com.doquynhhuong.project.ui.theme.BRGreen
import com.doquynhhuong.project.ui.theme.BROnBackground
import com.doquynhhuong.project.ui.theme.BROnPrimary
import com.doquynhhuong.project.ui.theme.BRPrimary
import com.doquynhhuong.project.ui.theme.BRSubtext
import com.doquynhhuong.project.ui.theme.BRSurface
import com.doquynhhuong.project.viewmodels.OrderViewModel

/** Icon + colour used to represent each badge tier — no image asset needed. */
private fun badgeIcon(tier: BadgeTier): ImageVector = when (tier) {
    BadgeTier.EXPLORER -> Icons.Filled.Explore
    BadgeTier.CURATOR -> Icons.Filled.WorkspacePremium
    BadgeTier.ARCHIVIST -> Icons.Filled.MilitaryTech
}

private fun badgeColor(tier: BadgeTier): Color = when (tier) {
    BadgeTier.EXPLORER -> BRGreen
    BadgeTier.CURATOR -> BRAmber
    BadgeTier.ARCHIVIST -> BRPrimary
}

private fun badgeDescription(tier: BadgeTier): String = when (tier) {
    BadgeTier.EXPLORER -> "Just getting started — keep uploading delivery photos to earn points."
    BadgeTier.CURATOR -> "You're a regular — nice collection of delivery photos so far."
    BadgeTier.ARCHIVIST -> "Top tier! You've documented tons of deliveries."
}

/** Points needed to reach the next tier, or null if already at the top tier. */
private fun nextTierThreshold(tier: BadgeTier): Int? = when (tier) {
    BadgeTier.EXPLORER -> BadgeTier.CURATOR.minPoints
    BadgeTier.CURATOR -> BadgeTier.ARCHIVIST.minPoints
    BadgeTier.ARCHIVIST -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: OrderViewModel) {
    val displayName by viewModel.sessionDisplayName.collectAsState()
    val email by viewModel.sessionEmail.collectAsState()
    val rewards by viewModel.rewardsProfile.collectAsState()
    val history by viewModel.orderHistory.collectAsState()

    val tier = rewards.badge
    val nextThreshold = remember(tier) { nextTierThreshold(tier) }
    val progress = remember(rewards.totalPoints, tier, nextThreshold) {
        if (nextThreshold == null) 1f
        else {
            val span = (nextThreshold - tier.minPoints).coerceAtLeast(1)
            ((rewards.totalPoints - tier.minPoints).toFloat() / span).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        containerColor = BRBackground,
        topBar = {
            TopAppBar(
                title = { Text("Profile & Rewards", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BRPrimary,
                    titleContentColor = BROnPrimary
                )
            )
        },
        bottomBar = {
            BottomNavBar(
                currentRoute = "profile",
                onHome    = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                onBrowse  = { navController.navigate("browse") },
                onCart    = { navController.navigate("cart") },
                onHistory = { navController.navigate("history") },
                onProfile = { }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    if (!displayName.isNullOrBlank()) displayName!! else email.orEmpty().ifBlank { "Guest" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BROnBackground
                    )
                )
            }

            // ── Badge card ──────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor(tier).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    badgeIcon(tier),
                                    contentDescription = "${tier.label} badge",
                                    tint = badgeColor(tier),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${tier.label} badge",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeColor(tier)
                                    )
                                )
                                Text(
                                    "${rewards.totalPoints} points total",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BRSubtext
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text(
                            badgeDescription(tier),
                            style = MaterialTheme.typography.bodySmall,
                            color = BRSubtext
                        )

                        Spacer(Modifier.height(14.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = badgeColor(tier),
                            trackColor = badgeColor(tier).copy(alpha = 0.15f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (nextThreshold != null)
                                "${(nextThreshold - rewards.totalPoints).coerceAtLeast(0)} pts to ${badgeAfter(tier)?.label}"
                            else
                                "You've reached the highest tier!",
                            style = MaterialTheme.typography.labelSmall,
                            color = BRSubtext
                        )
                    }
                }
            }

            // ── Tier reference card ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BRSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Badge tiers",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = BROnBackground
                        )
                        Spacer(Modifier.height(10.dp))
                        BadgeTierRow(BadgeTier.EXPLORER, "0 – 100 pts", tier == BadgeTier.EXPLORER)
                        Spacer(Modifier.height(8.dp))
                        BadgeTierRow(BadgeTier.CURATOR, "101 – 250 pts", tier == BadgeTier.CURATOR)
                        Spacer(Modifier.height(8.dp))
                        BadgeTierRow(BadgeTier.ARCHIVIST, "251+ pts", tier == BadgeTier.ARCHIVIST)
                    }
                }
            }

            // ── Points breakdown per order ─────────────────────────────
            if (rewards.pointsByOrder.values.any { it > 0 }) {
                item {
                    Text(
                        "Points earned per order",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BROnBackground
                    )
                }
                items(history.filter { order ->
                    val key = order.firestoreId.ifBlank { order.timestamp.toString() }
                    (rewards.pointsByOrder[key] ?: 0) > 0
                }) { order ->
                    val key = order.firestoreId.ifBlank { order.timestamp.toString() }
                    val pts = rewards.pointsByOrder[key] ?: 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BRSurface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                order.vendorName.ifBlank { "Order" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = BROnBackground
                            )
                            Text(
                                "+$pts pts",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BRPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun badgeAfter(tier: BadgeTier): BadgeTier? = when (tier) {
    BadgeTier.EXPLORER -> BadgeTier.CURATOR
    BadgeTier.CURATOR -> BadgeTier.ARCHIVIST
    BadgeTier.ARCHIVIST -> null
}

@Composable
private fun BadgeTierRow(tier: BadgeTier, rangeLabel: String, isCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(badgeColor(tier).copy(alpha = if (isCurrent) 0.9f else 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                badgeIcon(tier),
                contentDescription = tier.label,
                tint = if (isCurrent) Color.White else badgeColor(tier),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                tier.label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) badgeColor(tier) else BROnBackground
                )
            )
            Text(
                rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = BRSubtext
            )
        }
    }
}
