package com.cyclesafe.app.ui.screens.add_poi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun addPoi(
        name: String,
        description: String,
        poiType: PoiType,
        latitude: Double,
        longitude: Double,
        isDangerous: Boolean,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _addPoiState.value = AddPoiState.Loading
            try {
                if (imageUri != null) {
                    MediaManager.get().upload(imageUri).callback(object : UploadCallback {
                        override fun onStart(requestId: String) {}
                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val imageUrl = resultData["url"] as String
                            savePoi(name, description, poiType, latitude, longitude, isDangerous, imageUrl)
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            _addPoiState.value = AddPoiState.Error(error.description)
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {}
                    }).dispatch()
                } else {
                    savePoi(name, description, poiType, latitude, longitude, isDangerous, null)
                }
            } catch (e: Exception) {
                _addPoiState.value = AddPoiState.Error(e.message ?: "Failed to add POI")
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
        imageUrl: String?
    ) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val userName = auth.currentUser?.displayName ?: "Anonymous"
            poiRepository.addPoi(name, description, poiType, latitude, longitude, isDangerous, imageUrl, userId, userName)
            userRepository.awardPoints(userId, 10)
            _addPoiState.value = AddPoiState.Success
        }
    }
}