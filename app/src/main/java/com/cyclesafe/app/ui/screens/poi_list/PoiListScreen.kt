package com.cyclesafe.app.ui.screens.poi_list

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.R
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiListScreen(navController: NavController, viewModel: PoiListViewModel = viewModel()) {
    val uiState by viewModel.poisState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Points of Interest") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.mipmap.bicycle_icon),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(40.dp).padding(start = 8.dp)
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar {
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
                )
            }
        }
    ) { paddingValues ->
        val configuration = LocalConfiguration.current
        val orientation = configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            PoiListContentLandscape(navController, viewModel, uiState, paddingValues)
        } else {
            PoiListContentPortrait(navController, viewModel, uiState, paddingValues)
        }
    }
}

@Composable
private fun PoiListContentPortrait(
    navController: NavController,
    viewModel: PoiListViewModel,
    uiState: PoiListUiState,
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
    ) {
        SearchAndFilterUi(viewModel)
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is PoiListUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is PoiListUiState.Success -> {
                if (state.pois.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results found.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.pois) { poiListItem ->
                            PoiListItem(navController, poiListItem)
                        }
                    }
                }
            }
            is PoiListUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
        }
    }
}

@Composable
private fun PoiListContentLandscape(
    navController: NavController,
    viewModel: PoiListViewModel,
    uiState: PoiListUiState,
    paddingValues: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SearchAndFilterUi(viewModel)
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (val state = uiState) {
            is PoiListUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is PoiListUiState.Success -> {
                if (state.pois.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No results found.")
                        }
                    }
                } else {
                    items(state.pois) { poiListItem ->
                        PoiListItem(navController, poiListItem)
                    }
                }
            }
            is PoiListUiState.Error -> {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFilterUi(viewModel: PoiListViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPoiType by viewModel.selectedPoiType.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
    )

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedPoiType == null,
                onClick = { viewModel.onPoiTypeSelected(null) },
                label = { Text("All Types") },
                colors = chipColors
            )
            PoiType.entries.forEach { type ->
                FilterChip(
                    selected = selectedPoiType == type,
                    onClick = { viewModel.onPoiTypeSelected(type) },
                    label = { Text(type.displayName) },
                    colors = chipColors
                )
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sort by:", modifier = Modifier.align(Alignment.CenterVertically))
            FilterChip(
                selected = sortOrder == SortOrder.NEWEST_FIRST,
                onClick = { viewModel.onSortOrderChange(SortOrder.NEWEST_FIRST) },
                label = { Text("Newest first") },
                colors = chipColors
            )
            FilterChip(
                selected = sortOrder == SortOrder.OLDEST_FIRST,
                onClick = { viewModel.onSortOrderChange(SortOrder.OLDEST_FIRST) },
                label = { Text("Oldest first") },
                colors = chipColors
            )
        }
    }
}

@Composable
private fun PoiListItem(navController: NavController, poiListItem: PoiListItem) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            navController.navigate(Screen.PoiDetails.createRoute(poiListItem.poi.firestoreId))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = poiListItem.poi.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "By: ${poiListItem.authorName}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = PoiType.valueOf(poiListItem.poi.type).displayName, style = MaterialTheme.typography.bodyMedium)
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