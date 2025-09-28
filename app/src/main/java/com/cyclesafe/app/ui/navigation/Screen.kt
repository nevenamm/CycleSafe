package com.cyclesafe.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Login : Screen("login")
    object Registration : Screen("registration")
    object Profile : Screen("profile")
    object AddPoi : Screen("add_poi")
    object Ranking : Screen("ranking")
    object PoiList : Screen("poi_list")
    object PoiDetails : Screen("poi_details/{poiId}") {
        fun createRoute(poiId: String) = "poi_details/$poiId"
    }
}