package com.cyclesafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.ui.screens.add_poi.AddPoiScreen
import com.cyclesafe.app.ui.screens.home.HomeScreen
import com.cyclesafe.app.ui.screens.poi_details.PoiDetailsScreen
import com.cyclesafe.app.ui.screens.poi_list.PoiListScreen
import com.cyclesafe.app.ui.screens.profile.ProfileScreen
import com.cyclesafe.app.ui.screens.ranking.RankingScreen

@Composable
fun MainNavigation(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val locationViewModel: LocationViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen(navController, locationViewModel) }
        composable(Screen.Profile.route) { ProfileScreen(navController, onLogout) }
        composable(Screen.AddPoi.route) { AddPoiScreen(navController, locationViewModel) }
        composable(Screen.Ranking.route) { RankingScreen(navController) }
        composable(Screen.PoiList.route) { PoiListScreen(navController) }
        composable(
            route = Screen.PoiDetails.route,
            arguments = listOf(navArgument("poiId") { type = NavType.StringType })
        ) { backStackEntry ->
            PoiDetailsScreen(
                poiId = backStackEntry.arguments?.getString("poiId") ?: "",
                navController = navController
            )
        }
    }
}