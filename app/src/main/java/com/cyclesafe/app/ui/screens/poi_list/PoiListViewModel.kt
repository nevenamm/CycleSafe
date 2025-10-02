package com.cyclesafe.app.ui.screens.poi_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.data.repository.PoiRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class PoiListItem(
    val poi: com.cyclesafe.app.data.model.Poi,
    val authorName: String
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

class PoiListViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedPoiType = MutableStateFlow<PoiType?>(null)
    val selectedPoiType = _selectedPoiType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder = _sortOrder.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val pois: StateFlow<List<PoiListItem>> = combine(
        selectedPoiType, sortOrder
    ) { type, sort -> type to sort }
        .flatMapLatest { (type, sort) ->
            poiRepository.getFilteredPois(type, sort)
        }.map { pois ->
            pois.map {
                PoiListItem(
                    poi = it,
                    authorName = it.authorName
                )
            }
        }
        .combine(searchQuery.debounce(300)) { items, query ->
            if (query.isBlank()) {
                items
            } else {
                items.filter { it.poi.name.contains(query, ignoreCase = true) || it.authorName.contains(query, ignoreCase = true) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onPoiTypeSelected(poiType: PoiType?) {
        _selectedPoiType.value = poiType
    }

    fun onSortOrderChange(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }
}
