package com.example.nextalk.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextalk.NexTalkApplication
import com.example.nextalk.data.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SendMessageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = NexTalkApplication.instance.database
        val chatRepository = ChatRepository(database.conversationDao(), database.messageDao())
        val firestore = FirebaseFirestore.getInstance()

        return try {
            // Récupérer tous les messages en attente
            val pendingMessages = chatRepository.getPendingMessages()

            for (message in pendingMessages) {
                try {
                    // Envoyer le message à Firestore
                    firestore.collection("conversations")
                        .document(message.conversationId)
                        .collection("messages")
                        .document(message.id)
                        .set(message.toMap())
                        .await()

                    // Mettre à jour la conversation
                    firestore.collection("conversations")
                        .document(message.conversationId)
                        .update(
                            mapOf(
                                "lastMessage" to message.text,
                                "lastMessageTime" to message.timestamp,
                                "lastMessageSenderId" to message.senderId
                            )
                        )
                        .await()

                    // Marquer comme envoyé dans la base locale
                    chatRepository.markMessageAsSent(message.id)

                } catch (e: Exception) {
                    // Continuer avec les autres messages même si un échoue
                    e.printStackTrace()
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "send_pending_messages"
    }
}
