package com.cyclesafe.app.data.model

import com.google.firebase.firestore.GeoPoint

data class User(
    val uid: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = "",
    val imageUrl: String? = null,
    val points: Int = 0,
    val lastLocation: GeoPoint? = null
)