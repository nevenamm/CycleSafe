package com.cyclesafe.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId

@Entity(tableName = "pois")
data class Poi(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @DocumentId
    val firestoreId: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val dangerous: Boolean = false,
    val imageUrl: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val averageRating: Float = 0f,
    val ratingCount: Int = 0
)