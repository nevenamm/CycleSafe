package com.cyclesafe.app.ui.screens.add_poi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.utils.rememberImagePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPoiScreen(navController: NavController, locationViewModel: LocationViewModel, addPoiViewModel: AddPoiViewModel = viewModel()) {
    val location by locationViewModel.currentLocation.collectAsState()
    val addPoiState by addPoiViewModel.addPoiState.collectAsState()
    val name by addPoiViewModel.name.collectAsState()
    val description by addPoiViewModel.description.collectAsState()
    val selectedPoiType by addPoiViewModel.selectedPoiType.collectAsState()
    val isDangerous by addPoiViewModel.isDangerous.collectAsState()
    val imageUri by addPoiViewModel.imageUri.collectAsState()
    val nameError by addPoiViewModel.nameError.collectAsState()
    val descriptionError by addPoiViewModel.descriptionError.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        locationViewModel.updateCurrentLocation(navController.context)
    }

    LaunchedEffect(addPoiState) {
        when (val state = addPoiState) {
            is AddPoiState.Success -> {
                snackbarHostState.showSnackbar("POI added successfully!")
                delay(20)
                navController.popBackStack()
            }
            is AddPoiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    val showImagePicker = rememberImagePicker {
        addPoiViewModel.onImageUriChange(it)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add POI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            imageUri?.let {
                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = "Selected image preview",
                    modifier = Modifier.size(128.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(onClick = { showImagePicker() }) {
                Text("Add Photo (Optional)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { addPoiViewModel.onNameChange(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError != null,
                singleLine = true,
                supportingText = { nameError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { addPoiViewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                singleLine = true,
                supportingText = { descriptionError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    value = selectedPoiType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PoiType.entries.forEach { poiType ->
                        DropdownMenuItem(
                            text = { Text(poiType.displayName) },
                            onClick = {
                                addPoiViewModel.onPoiTypeSelected(poiType)
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = isDangerous, onCheckedChange = { addPoiViewModel.onIsDangerousChange(it) })
                Text("Mark as Dangerous")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    location?.let {
                        addPoiViewModel.addPoi(it.latitude, it.longitude)
                    } ?: run {
                        scope.launch {
                            snackbarHostState.showSnackbar("Location is null, cannot add POI.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = addPoiState !is AddPoiState.Loading
            ) {
                if (addPoiState is AddPoiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Add POI")
                }
            }
        }
    }
}