package com.cyclesafe.app.ui.screens.home

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.preferences.UserPreferencesRepository
import com.cyclesafe.app.data.repository.PoiRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)
    private val prefsRepository: UserPreferencesRepository = Injection.provideUserPreferencesRepository(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois = _pois.asStateFlow()

    val searchRadius = prefsRepository.searchRadius.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 2500f
    )

    val isDangerousFilter = prefsRepository.isDangerousFilter.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private var _currentLocation = MutableStateFlow<LatLng?>(null)

    init {
        viewModelScope.launch {
            combine(
                isDangerousFilter,
                searchRadius,
                searchQuery.debounce(300),
                _currentLocation
            ) { isDangerous, radius, query, location ->
                Triple(isDangerous, radius, query to location)
            }.flatMapLatest { (isDangerous, radius, queryLocationPair) ->
                val (query, location) = queryLocationPair
                poiRepository.getMapPois(isDangerous).map {
                    val filteredByLocation = if (location == null) {
                        it
                    } else {
                        it.filter { poi ->
                            val distance = FloatArray(1)
                            Location.distanceBetween(
                                location.latitude,
                                location.longitude,
                                poi.latitude,
                                poi.longitude,
                                distance
                            )
                            distance[0] <= radius
                        }
                    }

                    if (query.isBlank()) {
                        filteredByLocation
                    } else {
                        filteredByLocation.filter { poi ->
                            poi.name.contains(query, ignoreCase = true) || poi.description.contains(query, ignoreCase = true)
                        }
                    }
                }
            }.collect { _pois.value = it }
        }
    }

    fun onSearchRadiusChanged(radius: Float) {
        viewModelScope.launch {
            prefsRepository.updateSearchRadius(radius)
        }
    }

    fun onIsDangerousFilterChanged(isDangerous: Boolean) {
        viewModelScope.launch {
            prefsRepository.updateIsDangerousFilter(isDangerous)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        viewModelScope.launch {
            _searchQuery.value = ""
            prefsRepository.updateIsDangerousFilter(false)
            prefsRepository.updateSearchRadius(2500f)
        }
    }

    fun init(currentLocation: LatLng?) {
        _currentLocation.value = currentLocation
    }
}