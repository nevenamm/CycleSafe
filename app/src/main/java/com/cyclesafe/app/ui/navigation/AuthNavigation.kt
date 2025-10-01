package com.cyclesafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyclesafe.app.ui.screens.login.LoginScreen
import com.cyclesafe.app.ui.screens.registration.RegistrationScreen

@Composable
fun AuthNavigation(onLoginSuccess: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) { LoginScreen(navController, onLoginSuccess) }
        composable(Screen.Registration.route) { RegistrationScreen(navController, onLoginSuccess) }
    }
}