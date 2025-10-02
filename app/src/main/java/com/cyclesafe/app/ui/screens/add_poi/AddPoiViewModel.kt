package com.cyclesafe.app.ui.screens.add_poi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.cyclesafe.app.utils.CloudinaryUploader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import android.content.Context
import com.cyclesafe.app.data.model.PoiType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class AddPoiState {
    object Idle : AddPoiState()
    object Loading : AddPoiState()
    object Success : AddPoiState()
    data class Error(val message: String) : AddPoiState()
}

class AddPoiViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)
    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _addPoiState = MutableStateFlow<AddPoiState>(AddPoiState.Idle)
    val addPoiState = _addPoiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    private val _selectedPoiType = MutableStateFlow(PoiType.PATH)
    val selectedPoiType = _selectedPoiType.asStateFlow()

    private val _isDangerous = MutableStateFlow(false)
    val isDangerous = _isDangerous.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _tempImageUri = MutableStateFlow<Uri?>(null)
    val tempImageUri = _tempImageUri.asStateFlow()

    fun onNameChange(name: String) {
        _name.value = name
    }

    fun onDescriptionChange(description: String) {
        _description.value = description
    }

    fun onPoiTypeSelected(poiType: PoiType) {
        _selectedPoiType.value = poiType
    }

    fun onIsDangerousChange(isDangerous: Boolean) {
        _isDangerous.value = isDangerous
    }

    fun onImageUriChange(uri: Uri?) {
        _imageUri.value = uri
    }

    fun onTempImageUriChange(uri: Uri?) {
        _tempImageUri.value = uri
    }

    fun createImageUri(context: Context): Uri? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.externalCacheDir)
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
        } catch (e: IOException) {
            _addPoiState.value = AddPoiState.Error("Failed to create image file: ${e.localizedMessage}")
            return null
        }
    }

    private fun validateInput(): Boolean {
        if (_name.value.isBlank()) {
            _addPoiState.value = AddPoiState.Error("Name cannot be empty")
            return false
        }
        if (_description.value.isBlank()) {
            _addPoiState.value = AddPoiState.Error("Description cannot be empty")
            return false
        }
        return true
    }

    fun addPoi(latitude: Double, longitude: Double) {
        if (!validateInput()) {
            return
        }
        viewModelScope.launch {
            _addPoiState.value = AddPoiState.Loading
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _addPoiState.value = AddPoiState.Error("User not logged in")
                    return@launch
                }

                val user = userRepository.getUser(userId).first()
                val authorName = "${user.firstName} ${user.lastName}"

                val imageUrl = _imageUri.value?.let { CloudinaryUploader.uploadImage(it) }
                savePoi(_name.value, _description.value, _selectedPoiType.value, latitude, longitude, _isDangerous.value, imageUrl, userId, authorName)

            } catch (e: Exception) {
                _addPoiState.value = AddPoiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun savePoi(
        name: String,
        description: String,
        poiType: PoiType,
        latitude: Double,
        longitude: Double,
        isDangerous: Boolean,
        imageUrl: String?,
        userId: String,
        authorName: String
    ) {
        viewModelScope.launch {
            try {
                val addPoiJob = async { poiRepository.addPoi(name, description, poiType.name, latitude, longitude, isDangerous, imageUrl, userId, authorName) }
                val awardPointsJob = async { userRepository.awardPoints(userId, 10) }
                awaitAll(addPoiJob, awardPointsJob)
                _addPoiState.value = AddPoiState.Success
            } catch (e: Exception) {
                _addPoiState.value = AddPoiState.Error(e.message ?: "Failed to add POI")
            }
        }
    }
}