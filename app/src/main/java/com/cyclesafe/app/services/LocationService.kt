package com.cyclesafe.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cyclesafe.app.R
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.preferences.UserPreferencesRepository
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var dangerousPois = listOf<Poi>()
    private lateinit var prefsRepository: UserPreferencesRepository
    private val recentlyNotifiedPois = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        fetchDangerousPois() // fetch only dangerous POIs, updates when new POI is added
        prefsRepository = Injection.provideUserPreferencesRepository(applicationContext)
    }

    // called when the LocationViewModel calls startForegroundService()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        startLocationUpdates()
        return START_STICKY
    }

    // called when the LocationViewModel calls stopService()
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TimeUnit.SECONDS.toMillis(30))
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(15))
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
                        userRef.update("lastLocation", GeoPoint(location.latitude, location.longitude))
                    }

                    CoroutineScope(Dispatchers.Default).launch {
                        if (prefsRepository.isTrackingEnabled.first()) {
                            checkNearbyPois(LatLng(location.latitude, location.longitude))
                        }
                    }
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } catch (e: SecurityException) {
            // This should be handled by requesting permissions before starting the service
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("CycleSafe")
            .setContentText("Tracking your location")
            .setSmallIcon(R.mipmap.bicycle_icon)
            .build()
    }

    private fun fetchDangerousPois() {
        db.collection("pois").whereEqualTo("dangerous", true)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    dangerousPois = snapshot.toObjects(Poi::class.java)
                }
            }
    }

    private fun checkNearbyPois(userLocation: LatLng) {
        dangerousPois.forEach { poi ->
            val poiLocation = LatLng(poi.latitude, poi.longitude)
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                poiLocation.latitude,
                poiLocation.longitude,
                distance
            )
            if (distance[0] < 800) { // 800 meters radius
                val now = System.currentTimeMillis()
                val lastNotificationTime = recentlyNotifiedPois[poi.firestoreId]
                if (lastNotificationTime == null || now - lastNotificationTime > TimeUnit.MINUTES.toMillis(5)) {
                    sendNotification(this, poi)
                    recentlyNotifiedPois[poi.firestoreId] = now
                }
            }
        }
    }

    private fun sendNotification(context: Context, poi: Poi) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("dangerous_poi_channel", "Dangerous POIs", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "dangerous_poi_channel")
            .setContentTitle("Dangerous POI nearby")
            .setContentText("You are near a dangerous POI: ${poi.name}")
            .setSmallIcon(R.mipmap.bicycle_icon)
            .build()

        notificationManager.notify(poi.hashCode(), notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}