package com.cyclesafe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cyclesafe.app.ui.navigation.AppNavigation
import com.cyclesafe.app.ui.theme.CycleSafeTheme
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()

        MapsInitializer.initialize(this)
        enableEdgeToEdge()
        setContent {
            CycleSafeTheme {
                AppNavigation()
            }
        }
    }
}