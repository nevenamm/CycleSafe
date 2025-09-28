package com.cyclesafe.app.data.repository

import com.cyclesafe.app.data.model.Comment
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.model.Rating
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class PoiRepository(
    private val firestore: FirebaseFirestore,
    private val poiDao: com.cyclesafe.app.data.model.PoiDao,
    private val ratingDao: com.cyclesafe.app.data.model.RatingDao,
    private val commentDao: com.cyclesafe.app.data.model.CommentDao
) {

    fun getAllPois(): Flow<List<Poi>> {
        return poiDao.getAll()
    }

    suspend fun refreshPois() {
        val snapshot = firestore.collection("pois").get().await()
        val pois = snapshot.toObjects(Poi::class.java)
        poiDao.insertAll(pois)
    }

    fun getCommentsForPoi(poiId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPoi(poiId)
    }

    suspend fun refreshComments(poiId: String) {
        val snapshot = firestore.collection("pois").document(poiId).collection("comments").get().await()
        val comments = snapshot.toObjects(Comment::class.java)
        commentDao.insertAll(comments)
    }

    fun getRatingForUser(poiId: String, userId: String): Flow<Rating?> {
        return ratingDao.getRatingForUser(poiId, userId)
    }

    suspend fun addRating(poiId: String, userId: String, rating: Float) {
        val ratingRef = firestore.collection("pois").document(poiId).collection("ratings").document(userId)
        val poiRef = firestore.collection("pois").document(poiId)

        firestore.runTransaction { transaction ->
            val poiSnapshot = transaction.get(poiRef)
            val oldRatingDoc = transaction.get(ratingRef)

            val oldRating = oldRatingDoc.toObject(Rating::class.java)?.rating ?: 0f
            val newRatingCount = poiSnapshot.getLong("ratingCount")!!.toInt() + if (oldRatingDoc.exists()) 0 else 1
            val newAverageRating = (poiSnapshot.getDouble("averageRating")!! * (newRatingCount - 1) + rating - oldRating) / newRatingCount

            transaction.set(ratingRef, Rating(poiId, userId, rating))
            transaction.update(poiRef, "averageRating", newAverageRating)
            transaction.update(poiRef, "ratingCount", newRatingCount)
            null
        }.await()
    }

    suspend fun addComment(poiId: String, userId: String, userName: String, commentText: String) {
        val comment = Comment(
            poiId = poiId,
            userId = userId,
            userName = userName,
            comment = commentText,
            createdAt = com.google.firebase.Timestamp.now()
        )
        firestore.collection("pois").document(poiId).collection("comments").add(comment).await()
    }

    suspend fun addPoi(
        name: String,
        description: String,
        poiType: com.cyclesafe.app.ui.screens.add_poi.PoiType,
        latitude: Double,
        longitude: Double,
        isDangerous: Boolean,
        imageUrl: String?,
        authorId: String,
        authorName: String
    ) {
        val poi = Poi(
            name = name,
            description = description,
            type = poiType.name,
            latitude = latitude,
            longitude = longitude,
            dangerous = isDangerous,
            imageUrl = imageUrl,
            authorId = authorId,
            authorName = authorName,
            createdAt = com.google.firebase.Timestamp.now()
        )
        firestore.collection("pois").add(poi).await()
    }
}