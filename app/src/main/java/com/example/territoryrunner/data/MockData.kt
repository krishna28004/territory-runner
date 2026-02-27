package com.example.territoryrunner.data

data class UserStats(
    val totalRuns: Int,
    val longestStreak: Int,
    val totalDistanceKm: Double
)

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val distanceKm: Double,
    val isCurrentUser: Boolean = false
)

object MockRepository {
    val currentUserStats = UserStats(
        totalRuns = 42,
        longestStreak = 15,
        totalDistanceKm = 124.5
    )

    val leaderboardData = listOf(
        LeaderboardEntry(1, "SpeedyGonzales", 342.1),
        LeaderboardEntry(2, "RunnerX", 280.4),
        LeaderboardEntry(3, "AlexRuns", 245.0),
        LeaderboardEntry(4, "You", 124.5, isCurrentUser = true),
        LeaderboardEntry(5, "SlowPoke", 84.2),
        LeaderboardEntry(6, "Newbie", 12.0)
    )
}
