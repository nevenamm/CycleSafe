package com.cyclesafe.app.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<Comment>)

    @Query("SELECT * FROM comments WHERE poiId = :poiId ORDER BY createdAt DESC")
    fun getCommentsForPoi(poiId: String): Flow<List<Comment>>
}