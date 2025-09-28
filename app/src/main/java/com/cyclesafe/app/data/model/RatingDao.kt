package com.cyclesafe.app.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RatingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rating: Rating)

    @Query("SELECT * FROM ratings WHERE poiId = :poiId")
    fun getRatingsForPoi(poiId: String): Flow<List<Rating>>

    @Query("SELECT * FROM ratings WHERE poiId = :poiId AND userId = :userId")
    fun getRatingForUser(poiId: String, userId: String): Flow<Rating?>
}