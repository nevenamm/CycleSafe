package com.cyclesafe.app.ui.screens.home

import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.theme.CycleSafeTurquoise
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import com.cyclesafe.app.ui.theme.CycleSafeRed
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.cyclesafe.app.R

import android.content.pm.ActivityInfo
import com.cyclesafe.app.utils.LockScreenOrientation

import androidx.activity.compose.BackHandler
import android.app.Activity

@Composable
fun HomeScreen(navController: NavController, locationViewModel: LocationViewModel, homeViewModel: HomeViewModel = viewModel()) {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Are you sure you want to exit?", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(
                    onClick = {
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow)
                ) {
                    Text("Exit", color = MaterialTheme.colorScheme.onSecondary)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    BackHandler {
        showExitDialog = true
    }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    val location by locationViewModel.currentLocation.collectAsState()
    val pois by homeViewModel.pois.collectAsState()
    val isTracking by locationViewModel.isTracking.collectAsState()
    val isDangerousFilter by homeViewModel.isDangerousFilter.collectAsState()
    val searchRadius by homeViewModel.searchRadius.collectAsState()
    val requestPermission by locationViewModel.requestBackgroundLocationPermission.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val granted = permissionsMap.values.all { it }
        if (granted) locationViewModel.updateCurrentLocation(context)
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationViewModel.onPermissionResult(granted, context)
    }

    // permissions
    LaunchedEffect(requestPermission) {
        if (requestPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }
    LaunchedEffect(Unit) {
        locationViewModel.checkServiceState(context)
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    LaunchedEffect(location) {
        location?.let { homeViewModel.init(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo levo
            Image(
                painter = painterResource(id = R.mipmap.bicycle_icon),
                contentDescription = "App Logo",
                colorFilter = ColorFilter.tint(CycleSafeYellow),
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Naziv aplikacije centrirano (flexibilno)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "CycleSafe",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // IconButton desno od naziva
            IconButton(
                onClick = { locationViewModel.onToggleBackgroundTracking(context) }
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = "Dangerous POI Alert",
                    tint = if (isTracking) CycleSafeTurquoise else Color.Gray
                )
            }
        }

        // Controls: tracking switch, search radius slider, dangerous filter
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
        ) {
            // Search controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Search Radius: ${searchRadius.toInt()}m",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Slider(
                            value = searchRadius,
                            onValueChange = { homeViewModel.onSearchRadiusChanged(it) },
                            valueRange = 0f..2000f,
                            modifier = Modifier.fillMaxWidth(),
                            colors = SliderDefaults.colors(
                                thumbColor = CycleSafeTurquoise,
                                activeTrackColor = CycleSafeTurquoise
                            )
                        )
                    }


                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { homeViewModel.onSearchClicked() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CycleSafeYellow,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Search")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isDangerousFilter,
                            onCheckedChange = { homeViewModel.onIsDangerousFilterChanged(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CycleSafeYellow)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Show Dangerous POIs only",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Map with POIs + FAB
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(location ?: LatLng(44.7866, 20.4489), 10f)
            }

            LaunchedEffect(location) {
                location?.let {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
                }
            }

            LaunchedEffect(homeViewModel.triggerMapAdjustment) {
                homeViewModel.triggerMapAdjustment.collect {
                    location?.let { userLocation ->
                        val currentPois = homeViewModel.pois.value
                        val builder = LatLngBounds.builder().include(userLocation)
                        if (currentPois.isNotEmpty()) {
                            currentPois.forEach { poi ->
                                builder.include(LatLng(poi.latitude, poi.longitude))
                            }
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngBounds(builder.build(), 100)
                            )
                        } else {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(userLocation, 15f)
                            )
                        }
                    }
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
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

            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddPoi.route) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                containerColor = CycleSafeTurquoise,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add POI")
            }
        }

        // Bottom navigation
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            NavigationBarItem(
                selected = true,
                onClick = { /* Already on map */ },
                icon = { Icon(Icons.Default.Home, contentDescription = "Map") },
                label = { Text("Map") }
            )
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate(Screen.PoiList.route) { popUpTo(Screen.Home.route) } },
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
}
