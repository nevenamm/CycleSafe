package com.cyclesafe.app.data.model

import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId
    val firestoreId: String = "",
    val poiId: String = "",
    val userId: String = "",
    val userName: String = "",
    val comment: String = "",
    val createdAt: com.google.firebase.Timestamp? = null
)