package com.cyclesafe.app.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cyclesafe.app.R
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.utils.LockScreenOrientation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, locationViewModel: LocationViewModel, homeViewModel: HomeViewModel = viewModel()) {
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                Button(onClick = { (context as? Activity)?.finish() }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                Button(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    BackHandler { showExitDialog = true }

    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

    val location by locationViewModel.currentLocation.collectAsState()
    val isTracking by locationViewModel.isTracking.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        locationViewModel.requestBackgroundLocationPermission.collectLatest {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CycleSafe") },
                navigationIcon = {
                    Image(
                        painter = painterResource(id = R.mipmap.bicycle_icon),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(40.dp).padding(start = 8.dp)
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.AddPoi.route) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add POI",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { locationViewModel.onToggleBackgroundTracking(context) }) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Toggle Tracking",
                            tint = if (isTracking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = "Filter")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
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
    ) { paddingValues ->
        val pois by homeViewModel.pois.collectAsState()

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(location ?: LatLng(44.7866, 20.4489), 10f)
        }
        var hasCenteredMap by remember { mutableStateOf(false) }

        LaunchedEffect(location) {
            val currentLocation = location
            if (currentLocation != null && !hasCenteredMap) {
                homeViewModel.init(currentLocation)
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                hasCenteredMap = true
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        ) {
            pois.forEach { poi ->
                Marker(
                    state = rememberUpdatedMarkerState(position = LatLng(poi.latitude, poi.longitude)),
                    title = poi.name,
                    snippet = poi.description,
                    onInfoWindowClick = { navController.navigate(Screen.PoiDetails.createRoute(poi.firestoreId)) }
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                FilterControls(homeViewModel)
            }
        }
    }
}

@Composable
private fun FilterControls(homeViewModel: HomeViewModel) {
    val searchQuery by homeViewModel.searchQuery.collectAsState()
    val isDangerousFilter by homeViewModel.isDangerousFilter.collectAsState()
    val searchRadius by homeViewModel.searchRadius.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filter & Search", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { homeViewModel.clearFilters() }) {
                Text("Clear all")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { homeViewModel.onSearchQueryChanged(it) },
            label = { Text("Search by name or description") },
            modifier = Modifier.fillMaxWidth()
        )

        Column {
            Text("Search Radius: ${searchRadius.toInt()}m")
            Slider(
                value = searchRadius,
                onValueChange = { homeViewModel.onSearchRadiusChanged(it) },
                valueRange = 0f..5000f
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = isDangerousFilter,
                onCheckedChange = { homeViewModel.onIsDangerousFilterChanged(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.background)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Show Dangerous POIs only")
        }
    }
}