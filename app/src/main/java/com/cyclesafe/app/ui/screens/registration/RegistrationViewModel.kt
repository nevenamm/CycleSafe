package com.cyclesafe.app.ui.screens.registration

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    object Success : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState = _registrationState.asStateFlow()

    fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user != null) {
                    if (imageUri != null) {
                        MediaManager.get().upload(imageUri).callback(object : UploadCallback {
                            override fun onStart(requestId: String?) {
                                _registrationState.value = RegistrationState.Loading
                            }

                            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                                val imageUrl = resultData?.get("url").toString()
                                saveUser(user.uid, email, firstName, lastName, phone, imageUrl)
                            }

                            override fun onError(requestId: String?, error: ErrorInfo?) {
                                _registrationState.value = RegistrationState.Error(error?.description ?: "Image upload failed")
                            }

                            override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                        }).dispatch()
                    } else {
                        saveUser(user.uid, email, firstName, lastName, phone, null)
                    }
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun saveUser(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        imageUrl: String?
    ) {
        viewModelScope.launch {
            try {
                val user = User(uid, email, firstName, lastName, phone, imageUrl)
                userRepository.insertUser(user)
                _registrationState.value = RegistrationState.Success
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Failed to save user data")
            }
        }
    }
}