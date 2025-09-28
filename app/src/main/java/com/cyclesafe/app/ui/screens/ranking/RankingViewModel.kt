package com.cyclesafe.app.ui.screens.ranking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.model.User
import com.cyclesafe.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class RankingViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getAllUsers().collect {
                _users.value = it
            }
        }
    }
}