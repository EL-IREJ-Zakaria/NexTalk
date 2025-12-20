package com.example.nextalk.data.repository

import android.net.Uri
import android.util.Log
import com.example.nextalk.data.local.dao.MessageDao
import com.example.nextalk.data.model.Message
import com.example.nextalk.data.model.MessageStatus
import com.example.nextalk.data.model.MessageType
import com.example.nextalk.service.MediaService
import com.example.nextalk.util.withRetry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Extensions pour ChatRepository avec fonctionnalités avancées
 */
class ChatRepositoryExtensions(
    private val messageDao: MessageDao,
    private val mediaService: MediaService = MediaService()
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val conversationsCollection = firestore.collection("conversations")

    companion object {
        private const val TAG = "ChatRepoExtensions"
        private const val MESSAGES_PAGE_SIZE = 50
    }

    /**
     * Envoyer un message avec image compressée
     */
    suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        imageUri: Uri,
        caption: String = ""
    ): Result<Message> = withRetry(maxRetries = 2) {
        // Upload l'image
        val uploadResult = mediaService.uploadChatFile(imageUri, conversationId)
        
        if (uploadResult.isFailure) {
            throw uploadResult.exceptionOrNull() ?: Exception("Failed to upload image")
        }

        val imageUrl = uploadResult.getOrNull() ?: throw Exception("No image URL")

        // Créer et envoyer le message
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            text = caption,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            type = MessageType.IMAGE
        )

        conversationsCollection
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .set(message.toMap())
            .await()

        // Mettre à jour la conversation
        conversationsCollection.document(conversationId).update(
            mapOf(
                "lastMessage" to "📷 Photo",
                "lastMessageTime" to message.timestamp,
                "lastMessageSenderId" to senderId
            )
        ).await()

        messageDao.insertMessage(message)
        Log.d(TAG, "Image message sent successfully")
        
        message
    }

    /**
     * Envoyer un message vocal
     */
    suspend fun sendVoiceMessage(
        conversationId: String,
        senderId: String,
        audioUri: Uri,
        duration: Long
    ): Result<Message> = withRetry(maxRetries = 2) {
        // Upload l'audio
        val uploadResult = mediaService.uploadChatFile(audioUri, conversationId)
        
        if (uploadResult.isFailure) {
            throw uploadResult.exceptionOrNull() ?: Exception("Failed to upload audio")
        }

        val audioUrl = uploadResult.getOrNull() ?: throw Exception("No audio URL")

        // Créer le message
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            text = "Message vocal",
            imageUrl = audioUrl, // Réutiliser le champ imageUrl pour l'audio
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT,
            type = MessageType.VOICE
        )

        conversationsCollection
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .set(message.toMap())
            .await()

        conversationsCollection.document(conversationId).update(
            mapOf(
                "lastMessage" to "🎤 Message vocal",
                "lastMessageTime" to message.timestamp,
                "lastMessageSenderId" to senderId
            )
        ).await()

        messageDao.insertMessage(message)
        Log.d(TAG, "Voice message sent successfully")
        
        message
    }

    /**
     * Obtenir les messages paginés
     */
    suspend fun getMessagesPaginated(
        conversationId: String,
        lastTimestamp: Long? = null
    ): Result<List<Message>> {
        return try {
            val query = if (lastTimestamp != null) {
                conversationsCollection
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .startAfter(lastTimestamp)
                    .limit(MESSAGES_PAGE_SIZE.toLong())
            } else {
                conversationsCollection
                    .document(conversationId)
                    .collection("messages")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(MESSAGES_PAGE_SIZE.toLong())
            }

            val snapshot = query.get().await()
            val messages = snapshot.documents.mapNotNull { doc ->
                try {
                    Message.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message", e)
                    null
                }
            }.reversed() // Inverser pour avoir l'ordre chronologique

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated messages", e)
            Result.failure(e)
        }
    }

    /**
     * Rechercher des messages dans une conversation
     */
    suspend fun searchMessages(
        conversationId: String,
        query: String
    ): Result<List<Message>> {
        return try {
            val snapshot = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                try {
                    Message.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    null
                }
            }.filter { message ->
                message.text.contains(query, ignoreCase = true)
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching messages", e)
            Result.failure(e)
        }
    }

    /**
     * Marquer tous les messages comme lus
     */
    suspend fun markAllMessagesAsRead(
        conversationId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val snapshot = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                val status = doc.getString("status")
                if (status == MessageStatus.SENT.name || status == "DELIVERED") {
                    doc.reference.update("status", "READ").await()
                }
            }

            Log.d(TAG, "Marked ${snapshot.documents.size} messages as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir le nombre de messages non lus
     */
    suspend fun getUnreadMessageCount(
        conversationId: String,
        userId: String
    ): Result<Int> {
        return try {
            val snapshot = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            val unreadCount = snapshot.documents.count { doc ->
                val status = doc.getString("status")
                status == MessageStatus.SENT.name || status == "DELIVERED"
            }

            Result.success(unreadCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            Result.failure(e)
        }
    }

    /**
     * Supprimer un message pour tous
     */
    suspend fun deleteMessageForEveryone(
        conversationId: String,
        messageId: String
    ): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()

            messageDao.getMessageById(messageId)?.let { message ->
                messageDao.deleteMessage(message)
            }

            Log.d(TAG, "Message deleted for everyone")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            Result.failure(e)
        }
    }

    /**
     * Éditer un message
     */
    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ): Result<Unit> {
        return try {
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "text" to newText,
                        "isEdited" to true,
                        "editedAt" to System.currentTimeMillis()
                    )
                )
                .await()

            Log.d(TAG, "Message edited successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message", e)
            Result.failure(e)
        }
    }

    /**
     * Réagir à un message (emoji)
     */
    suspend fun reactToMessage(
        conversationId: String,
        messageId: String,
        userId: String,
        reaction: String
    ): Result<Unit> {
        return try {
            val messageRef = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)

            val doc = messageRef.get().await()
            val reactions = doc.get("reactions") as? MutableMap<String, Any> ?: mutableMapOf()
            
            reactions[userId] = reaction

            messageRef.update("reactions", reactions).await()

            Log.d(TAG, "Reaction added successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reaction", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les messages médias d'une conversation
     */
    suspend fun getMediaMessages(conversationId: String): Result<List<Message>> {
        return try {
            val snapshot = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereEqualTo("type", MessageType.IMAGE.name)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                try {
                    Message.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting media messages", e)
            Result.failure(e)
        }
    }

    /**
     * Synchroniser les messages depuis Firebase
     */
    suspend fun syncMessages(conversationId: String): Result<Unit> = withRetry(maxRetries = 2) {
        val snapshot = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()

        val messages = snapshot.documents.mapNotNull { doc ->
            try {
                Message.fromMap(doc.data ?: emptyMap(), doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message", e)
                null
            }
        }

        messageDao.insertMessages(messages)
        Log.d(TAG, "Synced ${messages.size} messages")
    }

    /**
     * Obtenir les messages locaux
     */
    fun getLocalMessages(conversationId: String): Flow<List<Message>> = 
        messageDao.getMessagesByConversation(conversationId)
            .catch { e ->
                Log.e(TAG, "Error getting local messages", e)
                emit(emptyList())
            }
}
