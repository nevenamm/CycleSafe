package com.cyclesafe.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val poiId: String = "",
    val userId: String = "",
    val userName: String = "",
    val comment: String = "",
    val createdAt: com.google.firebase.Timestamp? = null
)