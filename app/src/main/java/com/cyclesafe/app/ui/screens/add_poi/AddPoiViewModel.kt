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

    private val _nameError = MutableStateFlow<String?>(null)
    val nameError = _nameError.asStateFlow()

    private val _descriptionError = MutableStateFlow<String?>(null)
    val descriptionError = _descriptionError.asStateFlow()

    fun onNameChange(name: String) {
        _name.value = name
        validateName()
    }

    fun onDescriptionChange(description: String) {
        _description.value = description
        validateDescription()
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

    private fun validateName() {
        if (_name.value.isBlank()) {
            _nameError.value = "Name cannot be empty"
        } else {
            _nameError.value = null
        }
    }

    private fun validateDescription() {
        if (_description.value.isBlank()) {
            _descriptionError.value = "Description cannot be empty"
        } else {
            _descriptionError.value = null
        }
    }

    private fun validateInput(): Boolean {
        validateName()
        validateDescription()
        return _nameError.value == null && _descriptionError.value == null
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
