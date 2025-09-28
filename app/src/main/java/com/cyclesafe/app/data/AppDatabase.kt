package com.cyclesafe.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cyclesafe.app.data.model.*

@Database(
    entities = [Poi::class, User::class, Rating::class, Comment::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun poiDao(): PoiDao
    abstract fun userDao(): UserDao
    abstract fun ratingDao(): RatingDao
    abstract fun commentDao(): CommentDao
}