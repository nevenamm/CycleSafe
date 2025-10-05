package com.cyclesafe.app.utils

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri?) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onImageSelected(tempUri)
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
            tempUri = uri
            cameraLauncher.launch(uri)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose an option") },
            text = { Text("Select a profile photo from the gallery or take a new one with the camera.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) -> {
                                val uri = createImageUri()
                                tempUri = uri
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
                        showDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Text("Gallery")
                }
            }
        )
    }

    return {
        showDialog = true
    }
}
