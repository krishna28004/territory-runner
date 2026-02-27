package com.example.territoryrunner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    currentStreak: Int,
    totalDistance: Double,
    territoryCapturedPercent: Float,
    onStartRun: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Map Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Map View Placeholder\n(Google Maps / Mapbox)",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.DarkGray
                )
            }
            
            // Bottom Stats Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem("Streak", "$currentStreak Days")
                        StatItem("Distance", "${totalDistance}km")
                        StatItem("Territory", "${territoryCapturedPercent}%")
                    }
                }
            }
            
            // Thumb-friendly padding for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }

        FloatingActionButton(
            onClick = onStartRun,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .size(72.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Start Run", modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
