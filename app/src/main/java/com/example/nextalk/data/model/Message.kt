package com.example.nextalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nextalk.data.local.Converters

enum class MessageStatus {
    PENDING,    // En attente d'envoi (mode hors-ligne)
    SENT,       // Envoyé
    RECEIVED,   // Reçu par le destinataire
    SEEN        // Lu par le destinataire
}

enum class MessageType {
    TEXT,
    IMAGE,
    EMOJI,
    VOICE,      // Message vocal
    FILE,       // Fichier
    STICKER     // Autocollant
}

/**
 * Représente une réaction à un message
 */
data class MessageReaction(
    val emoji: String = "",
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("")
}

/**
 * Informations sur le message auquel on répond
 */
data class ReplyInfo(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: MessageType = MessageType.TEXT
) {
    constructor() : this("")
    
    fun toMap(): Map<String, Any?> = mapOf(
        "messageId" to messageId,
        "senderId" to senderId,
        "senderName" to senderName,
        "text" to text,
        "type" to type.name
    )
    
    companion object {
        fun fromMap(map: Map<String, Any?>): ReplyInfo {
            return ReplyInfo(
                messageId = map["messageId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                senderName = map["senderName"] as? String ?: "",
                text = map["text"] as? String ?: "",
                type = try {
                    MessageType.valueOf(map["type"] as? String ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                }
            )
        }
    }
}

@Entity(tableName = "messages")
@TypeConverters(Converters::class)
data class Message(
    @PrimaryKey
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.PENDING,
    val type: MessageType = MessageType.TEXT,
    val isLocalOnly: Boolean = false, // Pour le mode hors-ligne
    
    // Nouvelles fonctionnalités innovantes
    val reactions: List<MessageReaction> = emptyList(), // Réactions emoji
    val replyTo: ReplyInfo? = null, // Réponse à un message
    val voiceDuration: Long = 0L, // Durée du message vocal en millisecondes
    val voiceUrl: String = "", // URL du message vocal
    val isEdited: Boolean = false, // Message modifié
    val editedAt: Long = 0L, // Timestamp de modification
    val isDeleted: Boolean = false, // Message supprimé (affiche "Message supprimé")
    val linkPreviewUrl: String = "", // URL de prévisualisation de lien
    val linkPreviewTitle: String = "", // Titre du lien
    val linkPreviewDescription: String = "", // Description du lien
    val linkPreviewImage: String = "" // Image du lien
) {
    // Constructeur sans argument pour Firebase
    constructor() : this("")

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "senderId" to senderId,
        "text" to text,
        "imageUrl" to imageUrl,
        "timestamp" to timestamp,
        "status" to status.name,
        "type" to type.name,
        "reactions" to reactions.map { 
            mapOf("emoji" to it.emoji, "userId" to it.userId, "timestamp" to it.timestamp) 
        },
        "replyTo" to replyTo?.toMap(),
        "voiceDuration" to voiceDuration,
        "voiceUrl" to voiceUrl,
        "isEdited" to isEdited,
        "editedAt" to editedAt,
        "isDeleted" to isDeleted,
        "linkPreviewUrl" to linkPreviewUrl,
        "linkPreviewTitle" to linkPreviewTitle,
        "linkPreviewDescription" to linkPreviewDescription,
        "linkPreviewImage" to linkPreviewImage
    )
    
    /**
     * Retourne les réactions groupées par emoji avec le compte
     */
    fun getGroupedReactions(): Map<String, Int> {
        return reactions.groupBy { it.emoji }.mapValues { it.value.size }
    }
    
    /**
     * Vérifie si un utilisateur a réagi avec un emoji spécifique
     */
    fun hasUserReacted(userId: String, emoji: String): Boolean {
        return reactions.any { it.userId == userId && it.emoji == emoji }
    }

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Message {
            @Suppress("UNCHECKED_CAST")
            val reactionsData = map["reactions"] as? List<Map<String, Any?>> ?: emptyList()
            val reactionsList = reactionsData.map { reactionMap ->
                MessageReaction(
                    emoji = reactionMap["emoji"] as? String ?: "",
                    userId = reactionMap["userId"] as? String ?: "",
                    timestamp = (reactionMap["timestamp"] as? Long) ?: System.currentTimeMillis()
                )
            }
            
            @Suppress("UNCHECKED_CAST")
            val replyToMap = map["replyTo"] as? Map<String, Any?>
            val replyInfo = replyToMap?.let { ReplyInfo.fromMap(it) }
            
            return Message(
                id = id,
                conversationId = map["conversationId"] as? String ?: "",
                senderId = map["senderId"] as? String ?: "",
                text = map["text"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                status = try {
                    MessageStatus.valueOf(map["status"] as? String ?: "SENT")
                } catch (e: Exception) {
                    MessageStatus.SENT
                },
                type = try {
                    MessageType.valueOf(map["type"] as? String ?: "TEXT")
                } catch (e: Exception) {
                    MessageType.TEXT
                },
                reactions = reactionsList,
                replyTo = replyInfo,
                voiceDuration = (map["voiceDuration"] as? Long) ?: 0L,
                voiceUrl = map["voiceUrl"] as? String ?: "",
                isEdited = map["isEdited"] as? Boolean ?: false,
                editedAt = (map["editedAt"] as? Long) ?: 0L,
                isDeleted = map["isDeleted"] as? Boolean ?: false,
                linkPreviewUrl = map["linkPreviewUrl"] as? String ?: "",
                linkPreviewTitle = map["linkPreviewTitle"] as? String ?: "",
                linkPreviewDescription = map["linkPreviewDescription"] as? String ?: "",
                linkPreviewImage = map["linkPreviewImage"] as? String ?: ""
            )
        }
    }
}
