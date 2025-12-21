package com.example.nextalk.data.repository

import android.net.Uri
import android.util.Log
import com.example.nextalk.data.local.dao.ConversationDao
import com.example.nextalk.data.local.dao.MessageDao
import com.example.nextalk.data.model.Conversation
import com.example.nextalk.data.model.Message
import com.example.nextalk.data.model.MessageStatus
import com.example.nextalk.data.model.MessageType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val conversationsCollection = firestore.collection("conversations")

    companion object {
        private const val TAG = "ChatRepository"
    }

    // Obtenir ou créer une conversation entre deux utilisateurs
    suspend fun getOrCreateConversation(currentUserId: String, otherUserId: String): String {
        return try {
            // Rechercher une conversation existante
            val existingConversation = conversationsCollection
                .whereArrayContains("users", currentUserId)
                .get()
                .await()
                .documents
                .find { doc ->
                    val users = doc.get("users") as? List<*>
                    users?.contains(otherUserId) == true
                }

            if (existingConversation != null) {
                return existingConversation.id
            }

            // Créer une nouvelle conversation
            val conversationId = UUID.randomUUID().toString()
            val conversation = Conversation(
                id = conversationId,
                users = listOf(currentUserId, otherUserId),
                lastMessage = "",
                lastMessageTime = System.currentTimeMillis()
            )

            conversationsCollection.document(conversationId).set(conversation.toMap()).await()
            conversationDao.insertConversation(conversation)

            conversationId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating conversation", e)
            throw e
        }
    }

    // Obtenir les conversations d'un utilisateur (version simplifiée sans orderBy pour éviter l'index)
    fun getConversationsForUser(userId: String): Flow<List<Conversation>> = callbackFlow {
        val listener = conversationsCollection
            .whereArrayContains("users", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to conversations", error)
                    // Envoyer une liste vide au lieu de fermer le flow
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        Conversation(
                            id = doc.id,
                            users = (data["users"] as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList(),
                            lastMessage = data["lastMessage"] as? String ?: "",
                            lastMessageTime = (data["lastMessageTime"] as? Long)
                                ?: System.currentTimeMillis(),
                            lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
                            unreadCount = (data["unreadCount"] as? Long)?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing conversation", e)
                        null
                    }
                }?.sortedByDescending { it.lastMessageTime } ?: emptyList()

                trySend(conversations)
            }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    // Obtenir les messages d'une conversation
    fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
        val listener = conversationsCollection
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Message.fromMap(doc.data ?: emptyMap(), doc.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Messages flow error", e)
        emit(emptyList())
    }

    // Envoyer un message texte avec support des nouvelles fonctionnalités
    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        imageUrl: String = "",
        replyTo: com.example.nextalk.data.model.ReplyInfo? = null
    ): Result<Message> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                text = text,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                type = type,
                replyTo = replyTo
            )

            // Sauvegarder le message
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message.toMap())
                .await()

            // Mettre à jour la conversation
            conversationsCollection.document(conversationId).update(
                mapOf(
                    "lastMessage" to when (type) {
                        MessageType.IMAGE -> "📷 Photo"
                        MessageType.VOICE -> "🎤 Message vocal"
                        else -> text
                    },
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSenderId" to senderId
                )
            ).await()

            // Sauvegarder localement
            messageDao.insertMessage(message)

            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            Result.failure(e)
        }
    }

    // Envoyer un message hors-ligne (sera envoyé par WorkManager)
    suspend fun sendMessageOffline(
        conversationId: String,
        senderId: String,
        text: String,
        type: MessageType = MessageType.TEXT
    ): Message {
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING,
            type = type,
            isLocalOnly = true
        )

        messageDao.insertMessage(message)
        return message
    }

    // Envoyer une image
    suspend fun sendImageMessage(
        conversationId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Message> {
        return try {
            // Upload de l'image
            val imageRef = storage.reference.child("chat_images/${UUID.randomUUID()}.jpg")
            imageRef.putFile(imageUri).await()
            val imageUrl = imageRef.downloadUrl.await().toString()

            // Envoyer le message avec l'URL de l'image
            sendMessage(
                conversationId = conversationId,
                senderId = senderId,
                text = "",
                type = MessageType.IMAGE,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error sending image message", e)
            Result.failure(e)
        }
    }

    // Mettre à jour le statut d'un message
    suspend fun updateMessageStatus(
        conversationId: String,
        messageId: String,
        status: MessageStatus
    ) {
        try {
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("status", status.name)
                .await()

            messageDao.updateMessageStatus(messageId, status)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating message status", e)
        }
    }

    // Marquer les messages comme lus
    suspend fun markMessagesAsRead(conversationId: String, currentUserId: String) {
        try {
            val unreadMessages = conversationsCollection
                .document(conversationId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUserId)
                .get()
                .await()

            unreadMessages.documents.forEach { doc ->
                val status = doc.getString("status")
                if (status != MessageStatus.SEEN.name) {
                    doc.reference.update("status", MessageStatus.SEEN.name)
                }
            }

            conversationDao.markAsRead(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read", e)
        }
    }

    // Obtenir les messages en attente
    suspend fun getPendingMessages(): List<Message> = messageDao.getPendingMessages()

    // Marquer un message comme envoyé
    suspend fun markMessageAsSent(messageId: String) {
        messageDao.markAsSent(messageId)
    }

    // Sauvegarder localement
    suspend fun saveMessagesLocally(messages: List<Message>) {
        messageDao.insertMessages(messages)
    }

    suspend fun saveConversationsLocally(conversations: List<Conversation>) {
        conversationDao.insertConversations(conversations)
    }

    // Obtenir depuis la base locale
    fun getLocalMessages(conversationId: String): Flow<List<Message>> =
        messageDao.getMessagesByConversation(conversationId)

    fun getLocalConversations(): Flow<List<Conversation>> =
        conversationDao.getAllConversations()

    // ============= NOUVELLES FONCTIONNALITÉS =============

    /**
     * Mettre à jour les réactions d'un message
     */
    suspend fun updateMessageReactions(
        messageId: String,
        reactions: List<com.example.nextalk.data.model.MessageReaction>
    ) {
        try {
            // Trouver le message dans Firestore
            val querySnapshot = conversationsCollection
                .whereNotEqualTo("id", "") // Query bidon pour chercher dans tous les documents
                .get()
                .await()

            for (conversationDoc in querySnapshot.documents) {
                val messageRef = conversationDoc.reference
                    .collection("messages")
                    .document(messageId)
                
                val messageSnapshot = messageRef.get().await()
                if (messageSnapshot.exists()) {
                    // Message trouvé, mettre à jour les réactions
                    messageRef.update(
                        "reactions",
                        reactions.map { 
                            mapOf(
                                "emoji" to it.emoji,
                                "userId" to it.userId,
                                "timestamp" to it.timestamp
                            )
                        }
                    ).await()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating reactions", e)
        }
    }

    /**
     * Supprimer un message (marquage comme supprimé)
     */
    suspend fun deleteMessage(messageId: String) {
        try {
            // Trouver le message dans Firestore
            val querySnapshot = conversationsCollection
                .whereNotEqualTo("id", "")
                .get()
                .await()

            for (conversationDoc in querySnapshot.documents) {
                val messageRef = conversationDoc.reference
                    .collection("messages")
                    .document(messageId)
                
                val messageSnapshot = messageRef.get().await()
                if (messageSnapshot.exists()) {
                    // Marquer comme supprimé au lieu de supprimer complètement
                    messageRef.update(
                        mapOf(
                            "isDeleted" to true,
                            "text" to "",
                            "imageUrl" to ""
                        )
                    ).await()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting message", e)
            throw e
        }
    }

    /**
     * Modifier un message
     */
    suspend fun editMessage(messageId: String, newText: String) {
        try {
            val querySnapshot = conversationsCollection
                .whereNotEqualTo("id", "")
                .get()
                .await()

            for (conversationDoc in querySnapshot.documents) {
                val messageRef = conversationDoc.reference
                    .collection("messages")
                    .document(messageId)
                
                val messageSnapshot = messageRef.get().await()
                if (messageSnapshot.exists()) {
                    messageRef.update(
                        mapOf(
                            "text" to newText,
                            "isEdited" to true,
                            "editedAt" to System.currentTimeMillis()
                        )
                    ).await()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error editing message", e)
            throw e
        }
    }

    /**
     * Envoyer un message vocal
     */
    suspend fun sendVoiceMessage(
        conversationId: String,
        senderId: String,
        voiceUri: Uri,
        duration: Long
    ): Result<Message> {
        return try {
            Log.d(TAG, "Uploading voice message...")
            Log.d(TAG, "Voice URI: $voiceUri")
            Log.d(TAG, "Duration: ${duration}ms")
            
            // Upload du fichier audio avec metadata
            val fileName = "voice_messages/${UUID.randomUUID()}.3gp"
            val voiceRef = storage.reference.child(fileName)
            
            // Ajouter les metadata pour le type de contenu (3GP/AMR)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("audio/3gpp")
                .build()
            
            Log.d(TAG, "Starting upload to: $fileName")
            
            try {
                voiceRef.putFile(voiceUri, metadata).await()
                Log.d(TAG, "Upload completed, getting download URL...")
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                throw e
            }
            
            val voiceUrl = voiceRef.downloadUrl.await().toString()
            Log.d(TAG, "Voice URL: $voiceUrl")

            // Créer le message vocal
            val messageId = UUID.randomUUID().toString()
            val message = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                text = "",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                type = MessageType.VOICE,
                voiceUrl = voiceUrl,
                voiceDuration = duration
            )

            // Sauvegarder le message
            conversationsCollection
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .set(message.toMap())
                .await()

            // Mettre à jour la conversation
            conversationsCollection.document(conversationId).update(
                mapOf(
                    "lastMessage" to "🎤 Message vocal",
                    "lastMessageTime" to message.timestamp,
                    "lastMessageSenderId" to senderId
                )
            ).await()

            // Sauvegarder localement
            messageDao.insertMessage(message)

            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending voice message", e)
            Result.failure(e)
        }
    }

    /**
     * Mettre à jour le statut "en train d'écrire"
     */
    suspend fun updateTypingStatus(
        conversationId: String,
        userId: String,
        isTyping: Boolean
    ) {
        try {
            conversationsCollection
                .document(conversationId)
                .update("${userId}_typing", isTyping)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating typing status", e)
        }
    }

    /**
     * Observer le statut "en train d'écrire"
     */
    fun observeTypingStatus(conversationId: String, otherUserId: String): Flow<Boolean> = callbackFlow {
        val listener = conversationsCollection
            .document(conversationId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(false)
                    return@addSnapshotListener
                }

                val isTyping = snapshot?.getBoolean("${otherUserId}_typing") ?: false
                trySend(isTyping)
            }

        awaitClose { listener.remove() }
    }.catch {
        emit(false)
    }
}
