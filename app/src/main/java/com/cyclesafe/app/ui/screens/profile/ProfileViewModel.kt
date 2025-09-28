package com.cyclesafe.app.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    data class Success(val user: User) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState = _profileState.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                userRepository.refreshUser(userId)
                userRepository.getUser(userId).collect { user ->
                    _profileState.value = ProfileState.Success(user)
                }
            } else {
                _profileState.value = ProfileState.Error("User not logged in")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}