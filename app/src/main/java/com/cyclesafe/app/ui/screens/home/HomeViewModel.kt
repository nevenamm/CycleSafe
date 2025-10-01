package com.cyclesafe.app.ui.screens.home

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.repository.PoiRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois = _pois.asStateFlow()

    private val _searchRadius = MutableStateFlow(1000f)
    val searchRadius = _searchRadius.asStateFlow()

    private val _isDangerousFilter = MutableStateFlow(false)
    val isDangerousFilter = _isDangerousFilter.asStateFlow()

    private val _triggerMapAdjustment = MutableSharedFlow<Unit>()
    val triggerMapAdjustment = _triggerMapAdjustment.asSharedFlow()

    fun onSearchRadiusChanged(radius: Float) {
        _searchRadius.value = radius
    }

    fun onSearchClicked() {
        viewModelScope.launch {
            _triggerMapAdjustment.emit(Unit)
        }
    }

    fun onIsDangerousFilterChanged(isDangerous: Boolean) {
        _isDangerousFilter.value = isDangerous
    }

    fun init(currentLocation: LatLng?) {
        viewModelScope.launch {
            poiRepository.getAllPois()
                .combine(searchRadius) { pois, radius ->
                    if (currentLocation == null) {
                        pois
                    } else {
                        pois.filter { poi ->
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                currentLocation.latitude,
                                currentLocation.longitude,
                                poi.latitude,
                                poi.longitude,
                                distance
                            )
                            distance[0] <= radius
                        }
                    }
                }
                .combine(isDangerousFilter) { pois, isDangerous ->
                    if (!isDangerous) {
                        pois
                    } else {
                        pois.filter { it.dangerous }
                    }
                }
                .collect { _pois.value = it }
        }
    }
}