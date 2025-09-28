package com.cyclesafe.app.ui.screens.home

import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.screens.add_poi.PoiType
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.MenuAnchorType

import androidx.compose.material3.Switch


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun HomeScreen(navController: NavController, locationViewModel: LocationViewModel = viewModel(), homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current
    val location by locationViewModel.currentLocation.collectAsState()
    val pois by homeViewModel.pois.collectAsState()
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isTracking by locationViewModel.isTracking.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val searchRadius by homeViewModel.searchRadius.collectAsState()
    val requestPermission by locationViewModel.requestBackgroundLocationPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        locationViewModel.updateCurrentLocation(context)
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationViewModel.onPermissionResult(granted, context)
    }

    LaunchedEffect(requestPermission) {
        if (requestPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
    }

    LaunchedEffect(Unit) {
        locationViewModel.checkServiceState(context)
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(location) {
        location?.let {
            homeViewModel.init(it)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Default.Home, //change with logo of the app
                    contentDescription = "App Icon",
                    tint = Color(0xFF00BFA5) // tirkizna
                )
                Text(
                    text = "CycleSafe",
                    style = MaterialTheme.typography.titleLarge
                )
                Switch(
                    checked = isTracking,
                    onCheckedChange = { locationViewModel.onToggleBackgroundTracking(context) }
                )
                IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile"
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { homeViewModel.onSearchQueryChanged(it) },
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    TextField(
                        value = homeViewModel.selectedPoiType.value?.name ?: "All",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                homeViewModel.onPoiTypeSelected(null)
                                expanded = false
                            }
                        )
                        PoiType.values().forEach { poiType ->
                            DropdownMenuItem(
                                text = { Text(poiType.name) },
                                onClick = {
                                    homeViewModel.onPoiTypeSelected(poiType)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                var authorExpanded by remember { mutableStateOf(false) }
                val authors by homeViewModel.authors.collectAsState()

                ExposedDropdownMenuBox(expanded = authorExpanded, onExpandedChange = { authorExpanded = !authorExpanded }) {
                    TextField(
                        value = homeViewModel.selectedAuthor.value ?: "All",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authorExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(expanded = authorExpanded, onDismissRequest = { authorExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                homeViewModel.onAuthorSelected(null)
                                authorExpanded = false
                            }
                        )
                        authors.forEach { author ->
                            DropdownMenuItem(
                                text = { Text(author) },
                                onClick = {
                                    homeViewModel.onAuthorSelected(author)
                                    authorExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(text = "Search Radius: ${searchRadius.toInt()}m")
                Slider(
                    value = searchRadius,
                    onValueChange = { homeViewModel.onSearchRadiusChanged(it) },
                    valueRange = 100f..10000f
                )
            }


            val cameraPositionState = rememberCameraPositionState()

            LaunchedEffect(location) {
                location?.let {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
                }
            }

            GoogleMap(
                modifier = Modifier.weight(1f),
                cameraPositionState = cameraPositionState
            ) {
                location?.let { Marker(state = MarkerState(position = it), title = "You are here") }
                pois.forEach { poi ->
                    Marker(
                        state = MarkerState(position = LatLng(poi.latitude, poi.longitude)),
                        title = poi.name,
                        snippet = poi.description,
                        onInfoWindowClick = {
                            navController.navigate(Screen.PoiDetails.createRoute(poi.firestoreId))
                        }
                    )
                }
            }
        }

        // FAB dugme
        FloatingActionButton(
            onClick = { navController.navigate(Screen.AddPoi.route) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Color(0xFF00BFA5)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add POI")
        }

        // Donji meni (Bottom Navigation)
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            NavigationBarItem(
                selected = true,
                onClick = { /* Already on map */ },
                icon = { Icon(Icons.Default.Home, contentDescription = "Map") },
                label = { Text("Map") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(Screen.PoiList.route) },
                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "POIs") },
                label = { Text("POIs") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(Screen.Ranking.route) },
                icon = { Icon(Icons.Default.Star, contentDescription = "Ranking") },
                label = { Text("Ranking") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(Screen.Profile.route) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile") }
            )
        }
    }
}