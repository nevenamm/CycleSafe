package com.cyclesafe.app.ui.screens.ranking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface RankingUiState {
    object Loading : RankingUiState
    data class Success(val users: List<User>) : RankingUiState
    data class Error(val message: String) : RankingUiState
}

class RankingViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    val rankingState: StateFlow<RankingUiState> = userRepository.getAllUsers()
        .map<List<User>, RankingUiState> { RankingUiState.Success(it) }
        .catch { emit(RankingUiState.Error(it.message ?: "An unknown error occurred")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RankingUiState.Loading
        )

    val currentUserId: StateFlow<String?> = userRepository.getUser(auth.currentUser!!.uid).map { it.uid }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}