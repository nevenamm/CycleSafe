package com.cyclesafe.app.ui.screens.add_poi

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.location.LocationViewModel
import java.io.File

enum class PoiType {
    PATH,
    RISKY_INTERSECTION,
    BAD_ASPHALT,
    USEFUL_FACILITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPoiScreen(navController: NavController, locationViewModel: LocationViewModel = viewModel(), addPoiViewModel: AddPoiViewModel = viewModel()) {
    val location by locationViewModel.currentLocation.collectAsState()
    val addPoiState by addPoiViewModel.addPoiState.collectAsState()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedPoiType by remember { mutableStateOf(PoiType.PATH) }
    var isDangerous by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            imageUri = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Add POI", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier.size(128.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                value = selectedPoiType.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PoiType.values().forEach { poiType ->
                    DropdownMenuItem(
                        text = { Text(poiType.name) },
                        onClick = {
                            selectedPoiType = poiType
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isDangerous, onCheckedChange = { isDangerous = it })
            Text("Dangerous")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { galleryLauncher.launch("image/*") }) {
                Text("From Gallery")
            }
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Button(onClick = {
                val file = File(context.cacheDir, "temp_image.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                imageUri = uri
                cameraLauncher.launch(uri)
            }) {
                Text("Take Photo")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            location?.let {
                addPoiViewModel.addPoi(name, description, selectedPoiType, it.latitude, it.longitude, isDangerous, imageUri)
            }
        }) {
            Text("Add POI")
        }

        when (addPoiState) {
            is AddPoiState.Loading -> {
                CircularProgressIndicator()
            }
            is AddPoiState.Success -> {
                navController.popBackStack()
            }
            is AddPoiState.Error -> {
                Text(text = (addPoiState as AddPoiState.Error).message)
            }
            else -> {}
        }
    }
}