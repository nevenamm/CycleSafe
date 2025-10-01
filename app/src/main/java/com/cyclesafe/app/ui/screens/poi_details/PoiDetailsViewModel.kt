package com.cyclesafe.app.ui.screens.poi_details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyclesafe.app.data.model.Comment
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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

    private val _userRating = MutableStateFlow(0f)
    val userRating = _userRating.asStateFlow()

    private val _userComment = MutableStateFlow("")
    val userComment = _userComment.asStateFlow()

    fun getPoiDetails(poiId: String) {
        viewModelScope.launch {
            _poiDetailsState.value = PoiDetailsState.Loading
            try {
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

                combine(poiFlow, commentsFlow, userRatingFlow) { poi, comments, rating ->
                    _userRating.value = rating
                    PoiDetailsState.Success(poi, comments, rating)
                }.collect { state ->
                    _poiDetailsState.value = state
                }
            } catch (e: Exception) {
                _poiDetailsState.value = PoiDetailsState.Error(e.message ?: "Failed to fetch POI details")
            }
        }
    }

    fun onUserRatingChange(rating: Float) {
        _userRating.value = rating
    }

    fun onUserCommentChange(comment: String) {
        _userComment.value = comment
    }

    fun addRatingAndComment(poiId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val ratingJob = async { poiRepository.addRating(poiId, userId, _userRating.value) }
                val awardPointsForRatingJob = async { awardPoints(5) }
                awaitAll(ratingJob, awardPointsForRatingJob)

                if (_userComment.value.isNotBlank()) {
                    val user = userRepository.getUser(userId).firstOrNull()
                    val userName = user?.let { "${it.firstName} ${it.lastName}" } ?: "Anonymous"
                    val commentJob = async { poiRepository.addComment(poiId, userId, userName, _userComment.value) }
                    val awardPointsForCommentJob = async { awardPoints(2) }
                    awaitAll(commentJob, awardPointsForCommentJob)
                    _userComment.value = ""
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun awardPoints(points: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            userRepository.awardPoints(userId, points)
        }
    }
}