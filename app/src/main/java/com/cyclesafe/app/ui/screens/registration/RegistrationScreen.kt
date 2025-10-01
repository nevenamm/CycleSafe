package com.cyclesafe.app.ui.screens.registration

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.cyclesafe.app.ui.theme.CycleSafeYellow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(navController: NavController, onLoginSuccess: () -> Unit, viewModel: RegistrationViewModel = viewModel()) {
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val tempUri by viewModel.tempUri.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val registrationState by viewModel.registrationState.collectAsState()

    val usernameError by viewModel.usernameError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val firstNameError by viewModel.firstNameError.collectAsState()
    val lastNameError by viewModel.lastNameError.collectAsState()
    val phoneError by viewModel.phoneError.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onSnackbarMessageShown()
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

    fun createImageUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.externalCacheDir)
        return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = createImageUri()
            viewModel.onTempUriChange(uri)
            cameraLauncher.launch(uri)
        } else {
            // Permission denied
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowDialogChange(false) },
            title = { Text("Choose an option") },
            text = { Text("Select a profile photo from the gallery or take a new one with the camera.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onShowDialogChange(false)
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                val uri = createImageUri()
                                viewModel.onTempUriChange(uri)
                                cameraLauncher.launch(uri)
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }
                ) {
                    Text("Camera")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.onShowDialogChange(false)
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Text("Gallery")
                }
            }
        )
    }


    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create an Account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { viewModel.onShowDialogChange(true) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Placeholder",
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { viewModel.onShowDialogChange(true) }) {
                Text("Select Profile Photo", color = CycleSafeYellow)
            }


            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { viewModel.onFirstNameChange(it) },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = firstNameError != null,
                supportingText = { firstNameError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = lastName,
                onValueChange = { viewModel.onLastNameChange(it) },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = lastNameError != null,
                supportingText = { lastNameError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { viewModel.onUsernameChange(it) },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = usernameError != null,
                supportingText = { usernameError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = passwordError != null,
                supportingText = { passwordError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { viewModel.onPhoneChange(it) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = phoneError != null,
                supportingText = { phoneError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.registerUser() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = CycleSafeYellow),
                enabled = registrationState !is RegistrationState.Loading
            ) {
                if (registrationState is RegistrationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("Register", color = MaterialTheme.colorScheme.onSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = registrationState) {
                is RegistrationState.Success -> {
                    LaunchedEffect(Unit) {
                        onLoginSuccess()
                    }
                }
                is RegistrationState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}