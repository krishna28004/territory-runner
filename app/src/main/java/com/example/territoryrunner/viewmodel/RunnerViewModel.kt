package com.example.territoryrunner.viewmodel

import androidx.lifecycle.ViewModel
import com.example.territoryrunner.data.LeaderboardEntry
import com.example.territoryrunner.data.MockRepository
import com.example.territoryrunner.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RunnerUiState(
    val isLoggedIn: Boolean = false,
    val currentStreak: Int = MockRepository.currentUserStats.longestStreak,
    val totalDistance: Double = MockRepository.currentUserStats.totalDistanceKm,
    val territoryCapturedPercent: Float = 12.5f,
    val leaderboard: List<LeaderboardEntry> = MockRepository.leaderboardData,
    val userStats: UserStats = MockRepository.currentUserStats
)

class RunnerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RunnerUiState())
    val uiState: StateFlow<RunnerUiState> = _uiState.asStateFlow()

    fun login() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
    }

    fun logout() {
        _uiState.value = _uiState.value.copy(isLoggedIn = false)
    }

    fun startRun() {
        // Handle start run action logic
    }
}
