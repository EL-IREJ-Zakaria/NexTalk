package com.example.nextalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey
    val id: String = "",
    val users: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val unreadCount: Int = 0
) {
    // Constructeur sans argument pour Firebase
    constructor() : this("")

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "users" to users,
        "lastMessage" to lastMessage,
        "lastMessageTime" to lastMessageTime,
        "lastMessageSenderId" to lastMessageSenderId,
        "unreadCount" to unreadCount
    )

    // Obtenir l'ID de l'autre utilisateur
    fun getOtherUserId(currentUserId: String): String {
        return users.firstOrNull { it != currentUserId } ?: ""
    }
}
