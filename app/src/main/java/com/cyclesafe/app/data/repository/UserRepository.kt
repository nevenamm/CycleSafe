package com.cyclesafe.app.data.repository

import com.cyclesafe.app.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore,
    private val userDao: com.cyclesafe.app.data.model.UserDao
) {

    fun getUser(uid: String): Flow<User> {
        return userDao.getUser(uid)
    }

    suspend fun refreshUser(uid: String) {
        val snapshot = firestore.collection("users").document(uid).get().await()
        val user = snapshot.toObject(User::class.java)
        if (user != null) {
            userDao.insert(user)
        }
    }

    fun getAllUsers(): Flow<List<User>> {
        // TODO: Get users from Firestore and cache them in Room
        // For now, just return an empty flow
        return kotlinx.coroutines.flow.flowOf(emptyList())
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
        userDao.insert(user)
    }
}