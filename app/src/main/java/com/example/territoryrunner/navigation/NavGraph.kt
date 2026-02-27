package com.example.territoryrunner.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.territoryrunner.ui.screens.*
import com.example.territoryrunner.viewmodel.RunnerViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: RunnerViewModel = viewModel()
) {

    NavHost(
        navController = navController,
        startDestination = Screen.Map.route   // 🔥 TEMPORARY: Start directly at Map
    ) {

        composable(Screen.Map.route) {
            MapScreen()
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    viewModel.login()
                    navController.navigate(Screen.MainApp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }

        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    viewModel.login()
                    navController.navigate(Screen.MainApp.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MainApp.route) {
            MainAppScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun MainAppScreen(viewModel: RunnerViewModel) {

    val bottomNavController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    val items = listOf(
        Triple(Screen.Home.route, "Home", Icons.Default.Home),
        Triple(Screen.Leaderboard.route, "Leaderboard", Icons.Default.List),
        Triple(Screen.Profile.route, "Profile", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            bottomNavController.navigate(route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = bottomNavController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(Screen.Home.route) {
                HomeScreen(
                    currentStreak = uiState.currentStreak,
                    totalDistance = uiState.totalDistance,
                    territoryCapturedPercent = uiState.territoryCapturedPercent,
                    onStartRun = { viewModel.startRun() }
                )
            }

            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(leaderboard = uiState.leaderboard)
            }

            composable(Screen.Profile.route) {
                ProfileScreen(stats = uiState.userStats)
            }
        }
    }
}