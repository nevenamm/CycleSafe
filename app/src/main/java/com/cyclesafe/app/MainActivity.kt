package com.cyclesafe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import com.cyclesafe.app.data.AppDatabase
import com.cyclesafe.app.ui.navigation.AppNavigation
import com.cyclesafe.app.ui.theme.CycleSafeTheme
import com.cloudinary.android.MediaManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        MediaManager.init(this, config)

        val db by lazy {
            Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "cycle_safe_db"
            ).build()
        }
        //val hotspotDao = db.hotspotDao()

        enableEdgeToEdge()
        setContent {
            CycleSafeTheme {
                AppNavigation()
            }
        }
    }
}