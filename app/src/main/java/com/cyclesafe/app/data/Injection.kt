package com.cyclesafe.app.data

import android.content.Context
import androidx.room.Room
import com.cyclesafe.app.data.repository.PoiRepository
import com.cyclesafe.app.data.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore

object Injection {

    private fun provideDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "cycle_safe_db").build()
    }

    fun providePoiRepository(context: Context): PoiRepository {
        val database = provideDatabase(context)
        return PoiRepository(
            FirebaseFirestore.getInstance(),
            database.poiDao(),
            database.ratingDao(),
            database.commentDao()
        )
    }

    fun provideUserRepository(context: Context): UserRepository {
        val database = provideDatabase(context)
        return UserRepository(FirebaseFirestore.getInstance(), database.userDao())
    }
}