package com.cyclesafe.app.data.repository

import com.cyclesafe.app.data.model.Comment
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.model.Rating
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PoiRepository(private val firestore: FirebaseFirestore) {

    fun getAllPois(): Flow<List<Poi>> = callbackFlow {
        val listenerRegistration = firestore.collection("pois")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val pois = snapshot?.toObjects(Poi::class.java)
                if (pois != null) {
                    trySend(pois)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getCommentsForPoi(poiId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = firestore.collection("pois").document(poiId).collection("comments")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val comments = snapshot?.toObjects(Comment::class.java)
                if (comments != null) {
                    trySend(comments)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getRatingForUser(poiId: String, userId: String): Flow<Rating?> = callbackFlow {
        val listenerRegistration = firestore.collection("pois").document(poiId).collection("ratings").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val rating = snapshot?.toObject(Rating::class.java)
                trySend(rating)
            }
        awaitClose { listenerRegistration.remove() }
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
        poiType: String,
        latitude: Double,
        longitude: Double,
        isDangerous: Boolean,
        imageUrl: String?,
        authorId: String
    ) {
        val poi = Poi(
            name = name,
            description = description,
            type = poiType,
            latitude = latitude,
            longitude = longitude,
            dangerous = isDangerous,
            imageUrl = imageUrl,
            authorId = authorId,
            createdAt = com.google.firebase.Timestamp.now()
        )
        firestore.collection("pois").add(poi).await()
    }
}