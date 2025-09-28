package com.cyclesafe.app.data.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(poi: Poi)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pois: List<Poi>)

    @Update
    suspend fun update(poi: Poi)

    @Query("SELECT * FROM pois")
    fun getAll(): Flow<List<Poi>>

    @Query("DELETE FROM pois")
    suspend fun deleteAll()
}