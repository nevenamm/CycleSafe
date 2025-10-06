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
    data class Success(val poi: Poi, val authorName: String, val comments: List<Comment>) : PoiDetailsState()
    data class Error(val message: String) : PoiDetailsState()
}

sealed class DeleteState {
    object Idle : DeleteState()
    object Loading : DeleteState()
    object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    object SubmissionInProgress : UiEvent()
    object SubmissionFinished : UiEvent()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PoiDetailsViewModel(application: Application, private val poiId: String) : AndroidViewModel(application) {

    private val poiRepository: PoiRepository = Injection.providePoiRepository(application.applicationContext)
    private val userRepository: UserRepository = Injection.provideUserRepository(application.applicationContext)
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _poiDetailsState = MutableStateFlow<PoiDetailsState>(PoiDetailsState.Loading)
    val poiDetailsState = _poiDetailsState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private val _userComment = MutableStateFlow("")
    val userComment = _userComment.asStateFlow()

    private val _userRating = MutableStateFlow(0f)
    val userRating = _userRating.asStateFlow()


    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState = _deleteState.asStateFlow()

    val canDeletePoi: StateFlow<Boolean> = poiDetailsState.map { state ->
        if (state is PoiDetailsState.Success) {
            state.poi.authorId == auth.currentUser?.uid
        } else {
            false
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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

                        combine(authorFlow, commentsFlow) { author, comments ->
                            val authorName = "${author.firstName} ${author.lastName}"
                            PoiDetailsState.Success(poi, authorName, comments)
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

    fun onUserCommentChange(comment: String) {
        _userComment.value = comment
    }

    fun onUserRatingChange(rating: Float) {
        _userRating.value = rating
    }

    fun addRatingAndComment() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val poiState = _poiDetailsState.value
            if (poiState !is PoiDetailsState.Success) return@launch

            val rating = _userRating.value
            val comment = _userComment.value

            if (rating == 0f && comment.isBlank()) {
                _eventFlow.emit(UiEvent.ShowSnackbar("Rating and comment cannot both be empty."))
                return@launch
            }

            _eventFlow.emit(UiEvent.SubmissionInProgress)
            try {
                if (rating > 0f) {
                    poiRepository.addRating(poiId, userId, rating)
                    userRepository.awardPoints(userId, 5) // 5 points for rating
                }

                if (comment.isNotBlank()) {
                    val user = userRepository.getUser(userId).first()
                    val userName = "${user.firstName} ${user.lastName}"
                    poiRepository.addComment(poiId, userId, userName, comment)
                    userRepository.awardPoints(userId, 2) // 2 points for commenting
                }

                // Clear inputs on success
                _userComment.value = ""
                _userRating.value = 0f

                _eventFlow.emit(UiEvent.ShowSnackbar("Rating and comment submitted successfully."))

                // Refresh POI details
                getPoiDetails()

            } catch (e: Exception) {
                _eventFlow.emit(UiEvent.ShowSnackbar(e.message ?: "An error occurred."))
                android.util.Log.e("PoiDetailsViewModel", "Error adding rating and comment", e)
            } finally {
                _eventFlow.emit(UiEvent.SubmissionFinished)
            }
        }
    }

    fun deletePoi() {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading
            try {
                poiRepository.deletePoi(poiId)
                _deleteState.value = DeleteState.Success
            } catch (e: Exception) {
                _deleteState.value = DeleteState.Error(e.message ?: "Failed to delete POI")
            }
        }
    }
}
