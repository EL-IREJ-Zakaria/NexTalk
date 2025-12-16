package com.example.nextalk.data.repository

import android.util.Log
import com.example.nextalk.data.local.dao.CallDao
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.CallStatus
import com.example.nextalk.data.model.CallType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository pour gérer les appels
 */
class CallRepository(private val callDao: CallDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val callsCollection = firestore.collection("calls")

    companion object {
        private const val TAG = "CallRepository"
    }

    /**
     * Initier un appel
     */
    suspend fun initiateCall(
        conversationId: String,
        callerId: String,
        callerName: String,
        callerPhotoUrl: String,
        receiverId: String,
        receiverName: String,
        receiverPhotoUrl: String,
        type: CallType
    ): Result<Call> {
        return try {
            val callId = UUID.randomUUID().toString()
            val call = Call(
                id = callId,
                conversationId = conversationId,
                callerId = callerId,
                callerName = callerName,
                callerPhotoUrl = callerPhotoUrl,
                receiverId = receiverId,
                receiverName = receiverName,
                receiverPhotoUrl = receiverPhotoUrl,
                type = type,
                status = CallStatus.OUTGOING,
                timestamp = System.currentTimeMillis()
            )

            // Sauvegarder sur Firestore
            callsCollection.document(callId).set(call.toMap()).await()

            // Sauvegarder localement
            callDao.insertCall(call)

            Result.success(call)
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating call", e)
            Result.failure(e)
        }
    }

    /**
     * Mettre à jour le statut d'un appel
     */
    suspend fun updateCallStatus(
        callId: String,
        status: CallStatus,
        duration: Long = 0L
    ) {
        try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name,
                "endTime" to System.currentTimeMillis()
            )

            if (duration > 0) {
                updateData["duration"] = duration
            }

            callsCollection.document(callId).update(updateData).await()

            // Mettre à jour localement
            val call = callDao.getCallById(callId)
            if (call != null) {
                val updatedCall = call.copy(
                    status = status,
                    endTime = System.currentTimeMillis(),
                    duration = if (duration > 0) duration else call.duration
                )
                callDao.updateCall(updatedCall)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating call status", e)
        }
    }

    /**
     * Obtenir l'historique des appels pour une conversation
     */
    fun getCallsByConversation(conversationId: String): Flow<List<Call>> = callDao
        .getCallsByConversation(conversationId)
        .catch { e ->
            Log.e(TAG, "Error getting calls by conversation", e)
            emit(emptyList())
        }

    /**
     * Obtenir l'historique des appels pour un utilisateur
     */
    fun getCallsByUser(userId: String): Flow<List<Call>> = callDao
        .getCallsByUser(userId)
        .catch { e ->
            Log.e(TAG, "Error getting calls by user", e)
            emit(emptyList())
        }

    /**
     * Obtenir les appels récents
     */
    fun getRecentCalls(limit: Int = 20): Flow<List<Call>> = callDao
        .getRecentCalls(limit)
        .catch { e ->
            Log.e(TAG, "Error getting recent calls", e)
            emit(emptyList())
        }

    /**
     * Obtenir les appels manqués
     */
    fun getMissedCalls(): Flow<List<Call>> = callDao
        .getMissedCalls()
        .catch { e ->
            Log.e(TAG, "Error getting missed calls", e)
            emit(emptyList())
        }

    /**
     * Supprimer un appel
     */
    suspend fun deleteCall(callId: String) {
        try {
            callsCollection.document(callId).delete().await()
            callDao.deleteCall(callId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting call", e)
        }
    }

    /**
     * Activer la vidéo durant un appel vocal
     */
    suspend fun enableVideoInCall(callId: String) {
        try {
            callsCollection.document(callId).update(
                "isVideoAccepted" to true,
                "type" to "VIDEO"
            ).await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(isVideoAccepted = true))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling video", e)
        }
    }

    /**
     * Enregistrer un appel
     */
    suspend fun recordCall(callId: String) {
        try {
            callsCollection.document(callId).update(
                "isCallRecorded" to true
            ).await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(isCallRecorded = true))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording call", e)
        }
    }

    /**
     * Obtenir les statistiques d'appel
     */
    suspend fun getCallStats(userId: String): Map<String, Any?> {
        return try {
            val calls = callDao.getCallsByUser(userId).collect { list -> list }
            
            mapOf(
                "totalCalls" to calls.size,
                "totalDuration" to calls.sumOf { it.duration },
                "missedCalls" to calls.count { it.status == CallStatus.MISSED },
                "videoCalls" to calls.count { it.type == CallType.VIDEO }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting call stats", e)
            emptyMap()
        }
    }
}
