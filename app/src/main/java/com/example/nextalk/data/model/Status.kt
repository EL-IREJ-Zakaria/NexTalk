package com.example.nextalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Types de statuts
 */
enum class StatusType {
    TEXT,      // Texte simple
    IMAGE,     // Photo
    VIDEO      // Vidéo
}

/**
 * Modèle de statut
 */
@Entity(tableName = "statuses")
data class Status(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val content: String = "",  // Texte ou URL de l'image/vidéo
    val type: StatusType = StatusType.TEXT,
    val backgroundColor: String = "#075E54",  // Pour les statuts texte
    val textColor: String = "#FFFFFF",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000,  // 24h
    val duration: Long = 5000,  // Durée d'affichage en ms
    val viewedBy: List<String> = emptyList(),  // Utilisateurs qui ont vu
    val replies: List<StatusReply> = emptyList()  // Réponses au statut
) {
    constructor() : this("")

    /**
     * Vérifie si le statut a expiré
     */
    fun hasExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    /**
     * Convertit en map pour Firebase
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "userId" to userId,
        "userName" to userName,
        "userPhotoUrl" to userPhotoUrl,
        "content" to content,
        "type" to type.name,
        "backgroundColor" to backgroundColor,
        "textColor" to textColor,
        "createdAt" to createdAt,
        "expiresAt" to expiresAt,
        "duration" to duration,
        "viewedBy" to viewedBy,
        "replies" to replies.map { 
            mapOf(
                "userId" to it.userId,
                "userName" to it.userName,
                "message" to it.message,
                "timestamp" to it.timestamp
            )
        }
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Status {
            @Suppress("UNCHECKED_CAST")
            val viewedByList = (map["viewedBy"] as? List<String>) ?: emptyList()
            
            @Suppress("UNCHECKED_CAST")
            val repliesData = map["replies"] as? List<Map<String, Any?>> ?: emptyList()
            val replies = repliesData.map { replyMap ->
                StatusReply(
                    userId = replyMap["userId"] as? String ?: "",
                    userName = replyMap["userName"] as? String ?: "",
                    message = replyMap["message"] as? String ?: "",
                    timestamp = (replyMap["timestamp"] as? Long) ?: System.currentTimeMillis()
                )
            }

            return Status(
                id = id,
                userId = map["userId"] as? String ?: "",
                userName = map["userName"] as? String ?: "",
                userPhotoUrl = map["userPhotoUrl"] as? String ?: "",
                content = map["content"] as? String ?: "",
                type = try {
                    StatusType.valueOf(map["type"] as? String ?: "TEXT")
                } catch (e: Exception) {
                    StatusType.TEXT
                },
                backgroundColor = map["backgroundColor"] as? String ?: "#075E54",
                textColor = map["textColor"] as? String ?: "#FFFFFF",
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis(),
                expiresAt = (map["expiresAt"] as? Long) ?: (System.currentTimeMillis() + 24 * 60 * 60 * 1000),
                duration = (map["duration"] as? Long) ?: 5000,
                viewedBy = viewedByList,
                replies = replies
            )
        }
    }
}

/**
 * Réponse à un statut
 */
data class StatusReply(
    val userId: String = "",
    val userName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("")
}
