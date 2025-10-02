package com.cyclesafe.app.ui.screens.add_poi

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.R
import com.cyclesafe.app.location.LocationViewModel
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import kotlinx.coroutines.launch
import com.cyclesafe.app.data.model.PoiType
import java.io.IOException

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
    val tempImageUri by addPoiViewModel.tempImageUri.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(addPoiState) {
        when (addPoiState) {
            is AddPoiState.Success -> {
                navController.popBackStack()
            }
            is AddPoiState.Error -> {
                snackbarHostState.showSnackbar((addPoiState as AddPoiState.Error).message)
            }
            else -> {}
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        addPoiViewModel.onImageUriChange(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            addPoiViewModel.onImageUriChange(tempImageUri)
        } else {
            addPoiViewModel.onImageUriChange(null)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = addPoiViewModel.createImageUri(context)
            if (uri != null) {
                addPoiViewModel.onTempImageUriChange(uri)
                cameraLauncher.launch(uri)
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos.")
            }
        }
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
                    contentDescription = null,
                    modifier = Modifier.size(128.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = name,
                onValueChange = { addPoiViewModel.onNameChange(it) },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { addPoiViewModel.onDescriptionChange(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
                TextField(
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                    value = selectedPoiType.name,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PoiType.entries.forEach { poiType ->
                        DropdownMenuItem(
                            text = { Text(poiType.name) },
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
                Text("Dangerous")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Button(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("From Gallery")
                }
                Button(onClick = {
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            val uri = addPoiViewModel.createImageUri(context)
                            if (uri != null) {
                                addPoiViewModel.onTempImageUriChange(uri)
                                cameraLauncher.launch(uri)
                            }
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }) {
                    Text("Take Photo")
                }
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add POI")
            }

            when (addPoiState) {
                is AddPoiState.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {}
            }
        }
    }
}