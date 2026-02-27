package com.example.territoryrunner.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object MainApp : Screen("main_app")
    object Home : Screen("home")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
    object Map : Screen("map")   // ADD THIS
}
