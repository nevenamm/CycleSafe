package com.cyclesafe.app.ui.screens.registration

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import com.cyclesafe.app.utils.CloudinaryUploader
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

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _firstName = MutableStateFlow("")
    val firstName = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName = _lastName.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone = _phone.asStateFlow()

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri = _imageUri.asStateFlow()

    private val _usernameError = MutableStateFlow<String?>(null)
    val usernameError = _usernameError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError = _passwordError.asStateFlow()

    private val _firstNameError = MutableStateFlow<String?>(null)
    val firstNameError = _firstNameError.asStateFlow()

    private val _lastNameError = MutableStateFlow<String?>(null)
    val lastNameError = _lastNameError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError = _phoneError.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun onUsernameChange(username: String) {
        _username.value = username
        validateUsername()
    }

    fun onPasswordChange(password: String) {
        _password.value = password
        validatePassword()
    }

    fun onFirstNameChange(firstName: String) {
        _firstName.value = firstName
        validateFirstName()
    }

    fun onLastNameChange(lastName: String) {
        _lastName.value = lastName
        validateLastName()
    }

    fun onPhoneChange(phone: String) {
        _phone.value = phone
        validatePhone()
    }

    fun onImageUriChange(imageUri: Uri?) {
        _imageUri.value = imageUri
    }

    fun onSnackbarMessageShown() {
        _snackbarMessage.value = null
    }

    private fun validateUsername() {
        if (_username.value.isBlank()) {
            _usernameError.value = "Username cannot be empty"
        } else {
            _usernameError.value = null
        }
    }

    private fun validatePassword() {
        if (_password.value.length < 6) {
            _passwordError.value = "Password must be at least 6 characters long"
        } else {
            _passwordError.value = null
        }
    }

    private fun validateFirstName() {
        if (_firstName.value.isBlank()) {
            _firstNameError.value = "First name cannot be empty"
        } else {
            _firstNameError.value = null
        }
    }

    private fun validateLastName() {
        if (_lastName.value.isBlank()) {
            _lastNameError.value = "Last name cannot be empty"
        } else {
            _lastNameError.value = null
        }
    }

    private fun validatePhone() {
        if (_phone.value.isBlank()) {
            _phoneError.value = "Phone number cannot be empty"
        } else {
            _phoneError.value = null
        }
    }

    private fun validateAll(): Boolean {
        validateUsername()
        validatePassword()
        validateFirstName()
        validateLastName()
        validatePhone()
        return _usernameError.value == null && _passwordError.value == null
                && _firstNameError.value == null && _lastNameError.value == null
                && _phoneError.value == null
    }

    fun registerUser() {
        if (!validateAll()) {
            _snackbarMessage.value = "Please fill the fields correctly"
            return
        }
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val email = "${_username.value}@cyclesafe.app"
                val authResult = auth.createUserWithEmailAndPassword(email, _password.value).await()
                val user = authResult.user
                if (user != null) {
                    val imageUrl = _imageUri.value?.let { CloudinaryUploader.uploadImage(it) }
                    saveUser(user.uid, _username.value, _firstName.value, _lastName.value, _phone.value, imageUrl)
                    auth.signInWithEmailAndPassword(email, _password.value).await()
                    _registrationState.value = RegistrationState.Success
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun saveUser(
        uid: String,
        username: String,
        firstName: String,
        lastName: String,
        phone: String,
        imageUrl: String?
    ) {
        viewModelScope.launch {
            try {
                val user = User(uid, username, firstName, lastName, phone, imageUrl)
                userRepository.insertUser(user)
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Failed to save user data")
            }
        }
    }
}