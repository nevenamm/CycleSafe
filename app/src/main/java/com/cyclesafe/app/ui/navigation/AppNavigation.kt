package com.cyclesafe.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val auth = FirebaseAuth.getInstance()
    val (isLoggedIn, setLoggedIn) = remember { mutableStateOf(auth.currentUser != null) }

    if (isLoggedIn) {
        MainNavigation(onLogout = { setLoggedIn(false) })
    } else {
        AuthNavigation(onLoginSuccess = { setLoggedIn(true) })
    }
}