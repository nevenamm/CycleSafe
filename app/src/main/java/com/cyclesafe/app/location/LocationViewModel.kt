package com.cyclesafe.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.R
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.services.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
class LocationViewModel : ViewModel() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _dangerousPois = MutableStateFlow<List<Poi>>(emptyList())

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _requestBackgroundLocationPermission = MutableStateFlow(false)
    val requestBackgroundLocationPermission = _requestBackgroundLocationPermission.asStateFlow()

    init {
        fetchDangerousPois()
    }

    fun updateCurrentLocation(context: Context) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val userLocation = LatLng(loc.latitude, loc.longitude)
                _currentLocation.value = userLocation
                checkNearbyPois(context, userLocation)
            }
        }
    }

    fun onToggleBackgroundTracking(context: Context) {
        if (_isTracking.value) {
            context.stopService(Intent(context, LocationService::class.java))
            _isTracking.value = false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    context.startService(Intent(context, LocationService::class.java))
                    _isTracking.value = true
                } else {
                    _requestBackgroundLocationPermission.value = true
                }
            } else {
                context.startService(Intent(context, LocationService::class.java))
                _isTracking.value = true
            }
        }
    }

    fun onPermissionResult(granted: Boolean, context: Context) {
        _requestBackgroundLocationPermission.value = false
        if (granted) {
            context.startService(Intent(context, LocationService::class.java))
            _isTracking.value = true
        }
    }

    fun checkServiceState(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationService::class.java.name == service.service.className) {
                _isTracking.value = true
                return
            }
        }
        _isTracking.value = false
    }


    private fun fetchDangerousPois() {
        viewModelScope.launch {
            db.collection("pois").whereEqualTo("dangerous", true)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        // Handle error
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        _dangerousPois.value = snapshot.toObjects(Poi::class.java)
                    }
                }
        }
    }

    private fun checkNearbyPois(context: Context, userLocation: LatLng) {
        _dangerousPois.value.forEach { poi ->
            val poiLocation = LatLng(poi.latitude, poi.longitude)
            val distance = FloatArray(1)
            android.location.Location.distanceBetween(
                userLocation.latitude,
                userLocation.longitude,
                poiLocation.latitude,
                poiLocation.longitude,
                distance
            )
            if (distance[0] < 100) { // 100 meters radius
                sendNotification(context, poi)
            }
        }
    }

    private fun sendNotification(context: Context, poi: Poi) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("dangerous_poi_channel", "Dangerous POIs", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "dangerous_poi_channel")
            .setContentTitle("Dangerous POI nearby")
            .setContentText("You are near a dangerous POI: ${poi.name}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        notificationManager.notify(poi.hashCode(), notification)
    }
}