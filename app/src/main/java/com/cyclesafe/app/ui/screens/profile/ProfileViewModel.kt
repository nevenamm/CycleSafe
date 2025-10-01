package com.cyclesafe.app.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import com.cyclesafe.app.utils.CloudinaryUploader
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface ProfileState {
    object Loading : ProfileState
    data class Success(val user: User) : ProfileState
    data class Error(val message: String) : ProfileState
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val profileState: StateFlow<ProfileState> = userRepository.getUser(auth.currentUser!!.uid)
        .map<User, ProfileState> { ProfileState.Success(it) }
        .catch { emit(ProfileState.Error(it.message ?: "An unknown error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileState.Loading
        )

    private val _editableUser = MutableStateFlow<User?>(null)
    val editableUser = _editableUser.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword = _newPassword.asStateFlow()

    private val _oldPassword = MutableStateFlow("")
    val oldPassword = _oldPassword.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile = _isUpdatingProfile.asStateFlow()

    private val _isUpdatingPassword = MutableStateFlow(false)
    val isUpdatingPassword = _isUpdatingPassword.asStateFlow()

    init {
        viewModelScope.launch {
            profileState.collect { state ->
                if (state is ProfileState.Success) {
                    _editableUser.value = state.user
                }
            }
        }
    }

    fun onUserChange(user: User) {
        _editableUser.value = user
    }

    fun onImageUriChange(uri: Uri?) {
        _imageUri.value = uri
    }

    fun onNewPasswordChange(password: String) {
        _newPassword.value = password
    }

    fun onOldPasswordChange(password: String) {
        _oldPassword.value = password
    }

    fun updateProfile() {
        viewModelScope.launch {
            _isUpdatingProfile.value = true
            try {
                val userToUpdate = _editableUser.value
                if (userToUpdate != null) {
                    val imageUrl = _imageUri.value?.let { CloudinaryUploader.uploadImage(it) }
                    val updatedUser = userToUpdate.copy(
                        imageUrl = imageUrl ?: userToUpdate.imageUrl
                    )
                    userRepository.updateUser(updatedUser)
                    _toastMessage.emit("Profile updated successfully")
                }
            } catch (e: Exception) {
                _toastMessage.emit("Failed to update profile: ${e.localizedMessage}")
            }
            finally {
                _isUpdatingProfile.value = false
            }
        }
    }

    fun updatePassword() {
        viewModelScope.launch {
            _isUpdatingPassword.value = true
            try {
                val user = auth.currentUser
                if (user != null && user.email != null) {
                    val credential = EmailAuthProvider.getCredential(user.email!!, _oldPassword.value)
                    user.reauthenticate(credential).await()
                    user.updatePassword(_newPassword.value).await()
                    _newPassword.value = ""
                    _oldPassword.value = ""
                    _toastMessage.emit("Password updated successfully")
                } else {
                    _toastMessage.emit("User not found or email is null")
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password is too weak. Please choose a stronger password."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect old password. Please try again."
                    else -> "An unexpected error occurred: ${e.localizedMessage}"
                }
                _toastMessage.emit(errorMessage)
            }
            finally {
                _isUpdatingPassword.value = false
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}