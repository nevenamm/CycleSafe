package com.cyclesafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyclesafe.app.ui.screens.home.HomeScreen
import com.cyclesafe.app.ui.screens.login.LoginScreen
import com.cyclesafe.app.ui.screens.profile.ProfileScreen
import com.cyclesafe.app.ui.screens.registration.RegistrationScreen
import com.cyclesafe.app.ui.screens.add_poi.AddPoiScreen
import com.cyclesafe.app.ui.screens.ranking.RankingScreen
import com.cyclesafe.app.ui.screens.poi_details.PoiDetailsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument


import com.cyclesafe.app.ui.screens.poi_list.PoiListScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Registration.route) { RegistrationScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.Profile.route) { ProfileScreen(navController) }
        composable(Screen.AddPoi.route) { AddPoiScreen(navController) }
        composable(Screen.Ranking.route) { RankingScreen() }
        composable(Screen.PoiList.route) { PoiListScreen() }
        composable(
            route = Screen.PoiDetails.route,
            arguments = listOf(navArgument("poiId") { type = NavType.StringType })
        ) { backStackEntry ->
            PoiDetailsScreen(poiId = backStackEntry.arguments?.getString("poiId") ?: "")
        }
    }
}