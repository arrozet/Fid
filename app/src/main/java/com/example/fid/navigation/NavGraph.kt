package com.example.fid.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Auth : Screen("auth")
    object GoalSetup : Screen("goal_setup")
    object Dashboard : Screen("dashboard")
    object PhotoRegistration : Screen("photo_registration")
    object VoiceRegistration : Screen("voice_registration")
    object ManualRegistration : Screen("manual_registration")
    object FoodDetail : Screen("food_detail/{foodId}") {
        fun createRoute(foodId: Long) = "food_detail/$foodId"
    }
    object Progress : Screen("progress")
    object DailyDetail : Screen("daily_detail/{date}") {
        fun createRoute(date: Long) = "daily_detail/$date"
    }
    object Settings : Screen("settings")
    object CustomFoods : Screen("custom_foods")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Onboarding.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            com.example.fid.ui.screens.onboarding.OnboardingScreen(navController)
        }
        
        composable(Screen.Auth.route) {
            com.example.fid.ui.screens.auth.AuthScreen(navController)
        }
        
        composable(Screen.GoalSetup.route) {
            com.example.fid.ui.screens.goals.GoalSetupScreen(navController)
        }
        
        composable(Screen.Dashboard.route) {
            com.example.fid.ui.screens.dashboard.DashboardScreen(navController)
        }
        
        composable(Screen.PhotoRegistration.route) {
            com.example.fid.ui.screens.registration.PhotoRegistrationScreen(navController)
        }
        
        composable(Screen.VoiceRegistration.route) {
            com.example.fid.ui.screens.registration.VoiceRegistrationScreen(navController)
        }
        
        composable(Screen.ManualRegistration.route) {
            com.example.fid.ui.screens.registration.ManualRegistrationScreen(navController)
        }
        
        composable(Screen.FoodDetail.route) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId")?.toLongOrNull() ?: 0L
            com.example.fid.ui.screens.registration.FoodDetailScreen(navController, foodId)
        }
        
        composable(Screen.Progress.route) {
            com.example.fid.ui.screens.progress.ProgressScreen(navController)
        }
        
        composable(Screen.DailyDetail.route) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")?.toLongOrNull() ?: System.currentTimeMillis()
            com.example.fid.ui.screens.progress.DailyDetailScreen(navController, date)
        }
        
        composable(Screen.Settings.route) {
            com.example.fid.ui.screens.settings.SettingsScreen(navController)
        }
        
        composable(Screen.CustomFoods.route) {
            com.example.fid.ui.screens.settings.CustomFoodsScreen(navController)
        }
    }
}

