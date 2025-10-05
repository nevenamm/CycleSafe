package com.cyclesafe.app.ui.screens.poi_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

data class PoiListItem(
    val poi: Poi,
    val authorName: String
)

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

sealed interface PoiListUiState {
    object Loading : PoiListUiState
    data class Success(val pois: List<PoiListItem>) : PoiListUiState
    data class Error(val message: String) : PoiListUiState
}

class PoiListViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application)
    private val userRepository: UserRepository = Injection.provideUserRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedPoiType = MutableStateFlow<PoiType?>(null)
    val selectedPoiType = _selectedPoiType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST_FIRST)
    val sortOrder = _sortOrder.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val poisState: StateFlow<PoiListUiState> = combine(
        selectedPoiType, sortOrder
    ) { type, sort -> type to sort }
        .flatMapLatest { (type, sort) ->
            poiRepository.getFilteredPois(type, sort)
        }.flatMapLatest { pois ->
            if (pois.isEmpty()) {
                flowOf(emptyList())
            } else {
                val userFlows = pois.map { poi ->
                    userRepository.getUser(poi.authorId)
                }
                combine(userFlows) { users ->
                    pois.mapIndexed { index, poi ->
                        val user = users[index]
                        PoiListItem(
                            poi = poi,
                            authorName = "${user.firstName} ${user.lastName}"
                        )
                    }
                }
            }
        }
        .combine(searchQuery.debounce(300)) { items, query ->
            if (query.isBlank()) {
                items
            } else {
                items.filter { it.poi.name.contains(query, ignoreCase = true) || it.authorName.contains(query, ignoreCase = true) }
            }
        }
        .map<List<PoiListItem>, PoiListUiState> { PoiListUiState.Success(it) }
        .catch { emit(PoiListUiState.Error(it.message ?: "An unknown error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PoiListUiState.Loading
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
