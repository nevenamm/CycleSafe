package com.cyclesafe.app.ui.screens.home

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.ui.screens.add_poi.PoiType
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)

    private val _pois = MutableStateFlow<List<Poi>>(emptyList())
    val pois = _pois.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedPoiType = MutableStateFlow<PoiType?>(null)
    val selectedPoiType = _selectedPoiType.asStateFlow()

    private val _authors = MutableStateFlow<List<String>>(emptyList())
    val authors = _authors.asStateFlow()

    private val _selectedAuthor = MutableStateFlow<String?>(null)
    val selectedAuthor = _selectedAuthor.asStateFlow()

    private val _searchRadius = MutableStateFlow(5000f) // 5km default
    val searchRadius = _searchRadius.asStateFlow()

    fun onSearchRadiusChanged(radius: Float) {
        _searchRadius.value = radius
    }

    fun init(currentLocation: LatLng?) {
        viewModelScope.launch {
            poiRepository.refreshPois()
            poiRepository.getAllPois()
                .combine(searchQuery.debounce(300)) { pois, query ->
                    if (query.isEmpty()) {
                        pois
                    } else {
                        pois.filter { it.name.contains(query, ignoreCase = true) }
                    }
                }
                .combine(selectedPoiType) { pois, type ->
                    if (type == null) {
                        pois
                    } else {
                        pois.filter { it.type == type.name }
                    }
                }
                .combine(selectedAuthor) { pois, author ->
                    if (author == null) {
                        pois
                    } else {
                        pois.filter { it.authorName == author }
                    }
                }
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
                .collect { _pois.value = it }
        }

        viewModelScope.launch {
            poiRepository.getAllPois().collect { pois ->
                _authors.value = pois.map { it.authorName }.distinct()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onPoiTypeSelected(poiType: PoiType?) {
        _selectedPoiType.value = poiType
    }

    fun onAuthorSelected(author: String?) {
        _selectedAuthor.value = author
    }
}