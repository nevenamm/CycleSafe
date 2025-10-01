package com.cyclesafe.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.cyclesafe.app.services.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class LocationViewModel : ViewModel() {

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    private val _requestBackgroundLocationPermission = MutableStateFlow(false)
    val requestBackgroundLocationPermission = _requestBackgroundLocationPermission.asStateFlow()

    fun updateCurrentLocation(context: Context) {
        Log.d("LocationViewModel", "updateCurrentLocation called")
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                Log.d("LocationViewModel", "Location received: ${loc.latitude}, ${loc.longitude}")
                val userLocation = LatLng(loc.latitude, loc.longitude)
                _currentLocation.value = userLocation
            } else {
                Log.w("LocationViewModel", "Location received is null.")
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
        Log.d("LocationViewModel", "onPermissionResult called. Granted: $granted")
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
}