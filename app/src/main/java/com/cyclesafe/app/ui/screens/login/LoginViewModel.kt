package com.cyclesafe.app.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    private val _username = MutableStateFlow("")
    val username = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    fun onUsernameChange(username: String) {
        _username.value = username
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun loginUser() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val email = "${_username.value}@cyclesafe.app"
                auth.signInWithEmailAndPassword(email, _password.value).await()
                _loginState.value = LoginState.Success
            } catch (e: FirebaseAuthInvalidUserException) {
                _loginState.value = LoginState.Error("User with this username does not exist.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _loginState.value = LoginState.Error("Incorrect password. Please try again.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }
}