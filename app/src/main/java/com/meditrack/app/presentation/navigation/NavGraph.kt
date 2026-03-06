package com.meditrack.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.meditrack.app.presentation.screens.addmedicine.AddMedicineScreen
import com.meditrack.app.presentation.screens.auth.LoginScreen
import com.meditrack.app.presentation.screens.auth.RegisterScreen
import com.meditrack.app.presentation.screens.history.HistoryScreen
import com.meditrack.app.presentation.screens.home.HomeScreen
import com.meditrack.app.presentation.screens.report.ReportScreen
import androidx.compose.ui.Modifier
import com.meditrack.app.presentation.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home?highlightMedicineId={highlightMedicineId}") {
        fun createRoute(highlightMedicineId: Int = -1): String =
            "home?highlightMedicineId=$highlightMedicineId"
    }
    data object AddMedicine : Screen("add_medicine?medicineId={medicineId}") {
        fun createRoute(medicineId: Int = -1): String = "add_medicine?medicineId=$medicineId"
    }
    data object History : Screen("history")
    data object Report : Screen("report")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.createRoute()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.createRoute()) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.createRoute()) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("highlightMedicineId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) { backStackEntry ->
            @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
            HomeScreen(
                highlightMedicineId = backStackEntry.arguments?.getInt("highlightMedicineId")?.takeIf { it > 0 },
                onAddMedicine = {
                    navController.navigate(Screen.AddMedicine.createRoute())
                },
                onEditMedicine = { medicineId ->
                    navController.navigate(Screen.AddMedicine.createRoute(medicineId))
                }
            )
        }

        composable(
            route = Screen.AddMedicine.route,
            arguments = listOf(
                navArgument("medicineId") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            )
        ) {
            AddMedicineScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen()
        }

        composable(Screen.Report.route) {
            ReportScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onSignedOut = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
