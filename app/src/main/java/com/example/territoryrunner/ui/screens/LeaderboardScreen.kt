package com.example.territoryrunner.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.territoryrunner.data.LeaderboardEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(leaderboard: List<LeaderboardEntry>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(leaderboard) { entry ->
                LeaderboardRow(entry)
                Divider()
            }
        }
    }
}

@Composable
fun LeaderboardRow(entry: LeaderboardEntry) {
    val bgColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val textColor = if (entry.isCurrentUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#${entry.rank}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.width(40.dp)
        )
        
        Text(
            text = entry.username,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "${entry.distanceKm} km",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
