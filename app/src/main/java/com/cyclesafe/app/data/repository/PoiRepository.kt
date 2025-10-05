package com.cyclesafe.app.data.repository

import com.cyclesafe.app.data.model.Comment
import com.cyclesafe.app.data.model.Poi
import com.cyclesafe.app.data.model.PoiType
import com.cyclesafe.app.data.model.Rating
import com.cyclesafe.app.ui.screens.poi_list.SortOrder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PoiRepository(private val firestore: FirebaseFirestore) {

    fun getFilteredPois(poiType: PoiType?, sortOrder: SortOrder): Flow<List<Poi>> = callbackFlow {
        var query: Query = firestore.collection("pois")

        if (poiType != null) {
            query = query.whereEqualTo("type", poiType.name)
        }

        query = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> query.orderBy("createdAt", Query.Direction.DESCENDING)
            SortOrder.OLDEST_FIRST -> query.orderBy("createdAt", Query.Direction.ASCENDING)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
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

    fun getMapPois(dangerousOnly: Boolean): Flow<List<Poi>> = callbackFlow {
        var query: Query = firestore.collection("pois")

        if (dangerousOnly) {
            query = query.whereEqualTo("dangerous", true)
        }

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
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

    fun getPoiById(poiId: String): Flow<Poi?> = callbackFlow {
        val docRef = firestore.collection("pois").document(poiId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.toObject(Poi::class.java))
        }
        awaitClose { listenerRegistration.remove() }
    }

    fun getCommentsForPoi(poiId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = firestore.collection("pois").document(poiId).collection("comments")
            .orderBy("createdAt", Query.Direction.DESCENDING)
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

    suspend fun addRating(poiId: String, userId: String, rating: Float) {
        val ratingRef = firestore.collection("pois").document(poiId).collection("ratings").document()
        val poiRef = firestore.collection("pois").document(poiId)

        firestore.runTransaction { transaction ->
            val poiSnapshot = transaction.get(poiRef)
            val ratingCount = poiSnapshot.getLong("ratingCount")?.toInt() ?: 0
            val averageRating = poiSnapshot.getDouble("averageRating")?.toFloat() ?: 0f

            val newRatingCount = ratingCount + 1
            val newTotalRating = (averageRating * ratingCount) + rating
            val newAverageRating = if (newRatingCount > 0) newTotalRating / newRatingCount else 0f

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
        authorId: String,
        authorName: String
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
            authorName = authorName,
            createdAt = Timestamp.now()
        )
        firestore.collection("pois").add(poi).await()
    }

    suspend fun deletePoi(poiId: String): Unit = coroutineScope {
        val poiRef = firestore.collection("pois").document(poiId)

        val deleteCommentsJob = async {
            val commentsSnapshot = poiRef.collection("comments").get().await()
            for (doc in commentsSnapshot.documents) {
                doc.reference.delete().await()
            }
        }

        val deleteRatingsJob = async {
            val ratingsSnapshot = poiRef.collection("ratings").get().await()
            for (doc in ratingsSnapshot.documents) {
                doc.reference.delete().await()
            }
        }

        awaitAll(deleteCommentsJob, deleteRatingsJob)

        poiRef.delete().await()
    }
}