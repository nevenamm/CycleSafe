package com.cyclesafe.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.services.LocationService
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.cyclesafe.app.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository: UserPreferencesRepository = Injection.provideUserPreferencesRepository(application.applicationContext)

    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    val isTracking = prefsRepository.isTrackingEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _requestBackgroundLocationPermission = MutableSharedFlow<Unit>()
    val requestBackgroundLocationPermission = _requestBackgroundLocationPermission.asSharedFlow()

    // one-time location update, called when the map screen is opened
    @SuppressLint("MissingPermission")
    fun updateCurrentLocation(context: Context) {
        if (hasLocationPermission(context)) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { _currentLocation.value = LatLng(it.latitude, it.longitude) }
            }
        }
    }

    // notification bell
    fun onToggleBackgroundTracking(context: Context) {
        viewModelScope.launch {
            val wasTracking = isTracking.first()
            prefsRepository.updateIsTrackingEnabled(!wasTracking)
            if (!wasTracking) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    _requestBackgroundLocationPermission.emit(Unit)
                } else {
                    startService(context)
                }
            } else {
                stopService(context)
            }
        }
    }

    // called after the user responds to the background location permission request
    fun onPermissionResult(isGranted: Boolean, context: Context) {
        if (isGranted) {
            startService(context)
        } else {
            viewModelScope.launch {
                prefsRepository.updateIsTrackingEnabled(false)
            }
        }
    }

    fun checkServiceState(context: Context) {
        viewModelScope.launch {
            if (isTracking.first()) {
                startService(context)
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startService(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService(context: Context) {
        val intent = Intent(context, LocationService::class.java)
        context.stopService(intent)
    }
}