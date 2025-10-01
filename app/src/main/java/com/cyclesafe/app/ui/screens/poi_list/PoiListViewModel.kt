package com.cyclesafe.app.ui.screens.poi_list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.cyclesafe.app.data.model.PoiType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PoiListItem(
    val poi: com.cyclesafe.app.data.model.Poi,
    val authorName: String
)

enum class SortOrder {
    NONE,
    NEWEST_FIRST,
    OLDEST_FIRST
}

class PoiListViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application)
    private val userRepository: UserRepository = Injection.provideUserRepository(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedPoiType = MutableStateFlow<PoiType?>(null)
    val selectedPoiType = _selectedPoiType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NONE)
    val sortOrder = _sortOrder.asStateFlow()

    val authors: StateFlow<List<String>> = combine(
        poiRepository.getAllPois(),
        userRepository.getAllUsers()
    ) { pois, users ->
        val usersById = users.associateBy { it.uid }
        pois.map { poi ->
            usersById[poi.authorId]?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
        }.distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pois: StateFlow<List<PoiListItem>> = combine(
        listOf(
            poiRepository.getAllPois(),
            userRepository.getAllUsers(),
            searchQuery.debounce(300),
            selectedPoiType,
            sortOrder
        )
    ) { args ->
        val poiList = args[0] as List<com.cyclesafe.app.data.model.Poi>
        val users = args[1] as List<com.cyclesafe.app.data.model.User>
        val query = args[2] as String
        val type = args[3] as PoiType?
        val sortOrder = args[4] as SortOrder

        val usersById = users.associateBy { it.uid }
        val poiListItems = poiList.map { poi ->
            PoiListItem(
                poi = poi,
                authorName = usersById[poi.authorId]?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
            )
        }

        val filteredByQuery = if (query.isEmpty()) poiListItems
        else poiListItems.filter { it.poi.name.contains(query, ignoreCase = true) || it.authorName.contains(query, ignoreCase = true) }

        val filteredByType = type?.let { t -> filteredByQuery.filter { it.poi.type == t.name } } ?: filteredByQuery
        
        val sortedList = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> filteredByType.sortedByDescending { it.poi.createdAt?.toDate()?.time ?: 0L }
            SortOrder.OLDEST_FIRST -> filteredByType.sortedBy { it.poi.createdAt?.toDate()?.time ?: 0L }
            SortOrder.NONE -> filteredByType
        }

        sortedList
    }.stateIn(
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