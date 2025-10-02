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
import kotlinx.coroutines.launch

sealed class PoiDetailsState {
    object Loading : PoiDetailsState()
    data class Success(val poi: Poi, val authorName: String, val comments: List<Comment>, val userRating: Float) : PoiDetailsState()
    data class Error(val message: String) : PoiDetailsState()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PoiDetailsViewModel(application: Application, private val poiId: String) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)
    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _poiDetailsState = MutableStateFlow<PoiDetailsState>(PoiDetailsState.Loading)
    val poiDetailsState = _poiDetailsState.asStateFlow()

    private val _userRating = MutableStateFlow(0f)
    val userRating = _userRating.asStateFlow()

    private val _userComment = MutableStateFlow("")
    val userComment = _userComment.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting = _isSubmitting.asStateFlow()

    init {
        getPoiDetails()
    }

    private fun getPoiDetails() {
        viewModelScope.launch {
            _poiDetailsState.value = PoiDetailsState.Loading
            try {
                poiRepository.getPoiById(poiId).flatMapLatest { poi ->
                    if (poi == null) {
                        flowOf(PoiDetailsState.Error("POI not found"))
                    } else {
                        val authorFlow = userRepository.getUser(poi.authorId)
                        val commentsFlow = poiRepository.getCommentsForPoi(poiId)
                        val userId = auth.currentUser?.uid
                        val userRatingFlow = if (userId != null) {
                            poiRepository.getRatingForUser(poiId, userId).map { it?.rating ?: 0f }
                        } else {
                            flowOf(0f)
                        }

                        combine(authorFlow, commentsFlow, userRatingFlow) { author, comments, rating ->
                            _userRating.value = rating
                            val authorName = "${author.firstName} ${author.lastName}"
                            PoiDetailsState.Success(poi, authorName, comments, rating)
                        }
                    }
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

    fun addRatingAndComment() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val poiState = _poiDetailsState.value
            if (poiState !is PoiDetailsState.Success) return@launch

            _isSubmitting.value = true
            try {
                // Award points for rating if it's a new rating
                val currentRating = poiState.userRating
                if (currentRating == 0f && _userRating.value > 0f) {
                    poiRepository.addRating(poiId, userId, _userRating.value)
                    userRepository.awardPoints(userId, 5) // 5 points for rating
                }

                // Award points for comment
                if (_userComment.value.isNotBlank()) {
                    val user = userRepository.getUser(userId).first()
                    val userName = "${user.firstName} ${user.lastName}"
                    poiRepository.addComment(poiId, userId, userName, _userComment.value)
                    userRepository.awardPoints(userId, 2) // 2 points for commenting
                    _userComment.value = ""
                }
            } catch (e: Exception) {
                // Handle error, e.g., show a snackbar
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}