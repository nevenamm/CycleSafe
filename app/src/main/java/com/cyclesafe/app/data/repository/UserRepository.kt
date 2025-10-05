package com.cyclesafe.app.data.repository

import com.cyclesafe.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions.merge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(private val firestore: FirebaseFirestore) {

    fun getUser(uid: String): Flow<User> = callbackFlow {
        val listenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                if (user != null) {
                    trySend(user)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    fun getAllUsers(): Flow<List<User>> = callbackFlow {
        val listenerRegistration = firestore.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java)
                if (users != null) {
                    trySend(users)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun awardPoints(uid: String, points: Int) {
        val userRef = firestore.collection("users").document(uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val newPoints = snapshot.getLong("points")!! + points
            transaction.update(userRef, "points", newPoints)
            null
        }.await()
    }

    suspend fun insertUser(user: User) {
        firestore.collection("users").document(user.uid).set(user).await()
    }

    suspend fun updateUser(user: User) {
        firestore.collection("users").document(user.uid).set(user, merge()).await()
    }
}