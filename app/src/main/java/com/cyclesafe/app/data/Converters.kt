package com.cyclesafe.app.data

import androidx.room.TypeConverter
import com.google.firebase.Timestamp
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Timestamp? {
        return value?.let { Timestamp(Date(it)) }
    }

    @TypeConverter
    fun dateToTimestamp(timestamp: Timestamp?): Long? {
        return timestamp?.toDate()?.time
    }
}