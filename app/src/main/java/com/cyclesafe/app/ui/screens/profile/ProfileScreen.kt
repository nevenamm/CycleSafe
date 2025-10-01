package com.cyclesafe.app.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.cyclesafe.app.R
import com.cyclesafe.app.ui.navigation.Screen
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private fun getTransformedImageUrl(originalUrl: String): String {
    val parts = originalUrl.split("/upload/")
    if (parts.size == 2) {
        return "${parts[0]}/upload/w_256,h_256,c_fill,g_face/${parts[1]}"
    }
    return originalUrl
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, onLogout: () -> Unit, viewModel: ProfileViewModel = viewModel()) {
    val profileState by viewModel.profileState.collectAsState()
    val editableUser by viewModel.editableUser.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val oldPassword by viewModel.oldPassword.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val isUpdatingProfile by viewModel.isUpdatingProfile.collectAsState()
    val isUpdatingPassword by viewModel.isUpdatingPassword.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onImageUriChange(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onImageUriChange(tempUri)
        }
    }

    fun createImageUri(): Uri? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.externalCacheDir)
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
        } catch (e: IOException) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to create image file")
            }
            return null
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageUri()
            if (uri != null) {
                tempUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos.")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose an option") },
            text = { Text("Select a profile photo from the gallery or take a new one with the camera.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) -> {
                            val uri = createImageUri()
                            if (uri != null) {
                                tempUri = uri
                                cameraLauncher.launch(uri)
                            }
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }) { Text("Camera") }
            },
            dismissButton = {
                Button(onClick = {
                    showDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text("Gallery") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
                                selected = true,
                                onClick = { /* Already on profile */ },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (profileState) {
                is ProfileState.Loading -> {
                    CircularProgressIndicator()
                }
                is ProfileState.Success -> {
                    val user = editableUser
                    if (user != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { showDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = if (imageUri != null) {
                                rememberAsyncImagePainter(model = imageUri)
                            } else if (user.imageUrl != null) {
                                rememberAsyncImagePainter(model = getTransformedImageUrl(user.imageUrl))
                            } else {
                                painterResource(id = R.drawable.ic_launcher_foreground)
                            }
                            Image(
                                painter = painter,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { showDialog = true }) {
                            Text("Change Profile Photo", color = CycleSafeYellow)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = user.firstName,
                            onValueChange = { viewModel.onUserChange(user.copy(firstName = it)) },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = user.lastName,
                            onValueChange = { viewModel.onUserChange(user.copy(lastName = it)) },
                            label = { Text("Last Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = user.phone,
                            onValueChange = { viewModel.onUserChange(user.copy(phone = it)) },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { viewModel.updateProfile() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow),
                            enabled = !isUpdatingProfile
                        ) {
                            if (isUpdatingProfile) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                            } else {
                                Text("Save Changes", color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { viewModel.onOldPasswordChange(it) },
                            label = { Text("Old Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { viewModel.onNewPasswordChange(it) },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.updatePassword() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow),
                            enabled = !isUpdatingPassword
                        ) {
                            if (isUpdatingPassword) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                            } else {
                                Text("Change Password", color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Log out")
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
                else -> {}
            }
        }
    }
}