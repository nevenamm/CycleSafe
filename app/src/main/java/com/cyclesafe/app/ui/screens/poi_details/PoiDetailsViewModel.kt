package com.cyclesafe.app.ui.screens.poi_details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.Injection
import com.cyclesafe.app.data.model.Comment
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class PoiDetailsState {
    object Loading : PoiDetailsState()
    data class Success(val poi: Poi, val comments: List<Comment>, val userRating: Float) : PoiDetailsState()
    data class Error(val message: String) : PoiDetailsState()
}

class PoiDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)
    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _poiDetailsState = MutableStateFlow<PoiDetailsState>(PoiDetailsState.Loading)
    val poiDetailsState = _poiDetailsState.asStateFlow()

    fun getPoiDetails(poiId: String) {
        viewModelScope.launch {
            _poiDetailsState.value = PoiDetailsState.Loading
            try {
                poiRepository.refreshPois()
                poiRepository.refreshComments(poiId)

                val poiFlow = poiRepository.getAllPois().mapNotNull { pois ->
                    pois.find { it.firestoreId == poiId }
                }

                val commentsFlow = poiRepository.getCommentsForPoi(poiId)

                val userId = auth.currentUser?.uid
                val userRatingFlow = if (userId != null) {
                    poiRepository.getRatingForUser(poiId, userId).map { it?.rating ?: 0f }
                } else {
                    flowOf(0f)
                }

                combine(poiFlow, commentsFlow, userRatingFlow) { poi, comments, userRating ->
                    PoiDetailsState.Success(poi, comments, userRating)
                }.collect {
                    _poiDetailsState.value = it
                }
            } catch (e: Exception) {
                _poiDetailsState.value = PoiDetailsState.Error(e.message ?: "Failed to fetch POI details")
            }
        }
    }

    fun addRating(poiId: String, rating: Float) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            poiRepository.addRating(poiId, userId, rating)
            awardPoints(5)
        }
    }

    fun addComment(poiId: String, commentText: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val userName = auth.currentUser?.displayName ?: "Anonymous"
            poiRepository.addComment(poiId, userId, userName, commentText)
            awardPoints(2)
            // Refetch details to show new comment
            poiRepository.refreshComments(poiId)
        }
    }

    private fun awardPoints(points: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            userRepository.awardPoints(userId, points)
        }
    }
}