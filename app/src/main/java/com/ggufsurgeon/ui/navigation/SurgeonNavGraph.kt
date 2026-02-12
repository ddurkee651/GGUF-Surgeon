package com.ggufsurgeon.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ggufsurgeon.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Viewer : Screen("viewer")
    object Edit : Screen("edit")
    object Merge : Screen("merge")
    object Optimize : Screen("optimize")
}

@Composable
fun SurgeonNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                vm = hiltViewModel(),
                onNavigate = { route ->
                    navController.navigate(route)
                }
            )
        }
        
        composable(Screen.Viewer.route) {
            ViewerScreen(
                vm = hiltViewModel(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Edit.route) {
            EditScreen(
                vm = hiltViewModel(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Merge.route) {
            MergeScreen(
                vm = hiltViewModel(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Optimize.route) {
            OptimizeScreen(
                vm = hiltViewModel(),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
