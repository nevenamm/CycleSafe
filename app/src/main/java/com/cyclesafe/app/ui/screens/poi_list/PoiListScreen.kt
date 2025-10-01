package com.cyclesafe.app.ui.screens.poi_list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.R
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(navController: NavController, viewModel: PoiListViewModel = viewModel()) {
    val pois by viewModel.pois.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPoiType by viewModel.selectedPoiType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val configuration = LocalConfiguration.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Points of Interest") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.mipmap.bicycle_icon),
                        contentDescription = "App Logo",
                        colorFilter = ColorFilter.tint(CycleSafeYellow),
                        modifier = Modifier.size(40.dp).padding(start = 8.dp)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Map") },
                                label = { Text("Map") }
                            )
                            NavigationBarItem(
                                selected = true,
                                onClick = { /* Already on POI list */ },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "POIs") },
                                label = { Text("POIs") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.Ranking.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Ranking") },
                                label = { Text("Ranking") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { navController.navigate(Screen.Profile.route) { popUpTo(Screen.Home.route) } },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )            }
        }
    ) { paddingValues ->
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                item {
                    SearchAndFilterUi(viewModel, searchQuery, selectedPoiType, sortOrder)
                }
                items(pois) { poiListItem ->
                    PoiListItem(navController, poiListItem)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                SearchAndFilterUi(viewModel, searchQuery, selectedPoiType, sortOrder)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pois) { poiListItem ->
                        PoiListItem(navController, poiListItem)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterUi(
    viewModel: PoiListViewModel,
    searchQuery: String,
    selectedPoiType: PoiType?,
    sortOrder: SortOrder
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        // Filteri: Tip i Autor kao Chipovi
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Filter by type:", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPoiType == null,
                    onClick = { viewModel.onPoiTypeSelected(null) },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CycleSafeYellow
                    )
                )

                PoiType.values().forEach { type ->
                    FilterChip(
                        selected = selectedPoiType == type,
                        onClick = { viewModel.onPoiTypeSelected(type) },
                        label = { Text(type.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CycleSafeYellow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text("Sort by date:", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortOrder == SortOrder.NONE,
                    onClick = { viewModel.onSortOrderChange(SortOrder.NONE) },
                    label = { Text("None") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CycleSafeYellow
                    )
                )
                FilterChip(
                    selected = sortOrder == SortOrder.NEWEST_FIRST,
                    onClick = { viewModel.onSortOrderChange(SortOrder.NEWEST_FIRST) },
                    label = { Text("Newest first") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CycleSafeYellow
                    )
                )
                FilterChip(
                    selected = sortOrder == SortOrder.OLDEST_FIRST,
                    onClick = { viewModel.onSortOrderChange(SortOrder.OLDEST_FIRST) },
                    label = { Text("Oldest first") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CycleSafeYellow
                    )
                )
            }
        }
    }
}

@Composable
private fun PoiListItem(navController: NavController, poiListItem: PoiListItem) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            navController.navigate(Screen.PoiDetails.createRoute(poiListItem.poi.firestoreId))
        }.padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = poiListItem.poi.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Type: ${poiListItem.poi.type}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Author: ${poiListItem.authorName}", style = MaterialTheme.typography.bodyMedium)
            }
            poiListItem.poi.createdAt?.toDate()?.let {
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}