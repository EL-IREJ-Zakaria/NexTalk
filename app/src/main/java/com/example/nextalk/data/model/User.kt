package com.example.nextalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.PropertyName

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val birthDate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Constructeur sans argument pour Firebase
    constructor() : this("")

    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "name" to name,
        "email" to email,
        "photoUrl" to photoUrl,
        "isOnline" to isOnline,
        "lastSeen" to lastSeen,
        "fcmToken" to fcmToken,
        "birthDate" to birthDate,
        "createdAt" to createdAt
    )
}
