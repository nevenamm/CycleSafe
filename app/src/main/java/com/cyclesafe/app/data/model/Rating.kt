package com.cyclesafe.app.data.model

import androidx.room.Entity

@Entity(tableName = "ratings", primaryKeys = ["poiId", "userId"])
data class Rating(
    val poiId: String = "",
    val userId: String = "",
    val rating: Float = 0f
)