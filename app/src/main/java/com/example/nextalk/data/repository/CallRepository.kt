package com.example.nextalk.data.repository

import android.util.Log
import com.example.nextalk.data.local.dao.CallDao
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.CallStatus
import com.example.nextalk.data.model.CallType
import com.example.nextalk.util.withRetry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
     * Initier un appel avec retry automatique
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
        return withRetry(maxRetries = 3) {
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

            // Sauvegarder sur Firestore avec retry
            callsCollection.document(callId).set(call.toMap()).await()

            // Sauvegarder localement
            callDao.insertCall(call)
            
            Log.d(TAG, "Call initiated successfully: $callId")
            call
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
                mapOf(
                    "isVideoAccepted" to true,
                    "type" to "VIDEO"
                )
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
                mapOf("isCallRecorded" to true)
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
            val calls = callDao.getCallsByUser(userId).first()
            
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

    /**
     * Synchroniser les appels avec Firebase
     */
    suspend fun syncCallsFromFirebase(userId: String): Result<Unit> {
        return withRetry(maxRetries = 2) {
            Log.d(TAG, "Syncing calls from Firebase for user: $userId")
            
            val snapshot = callsCollection
                .whereEqualTo("callerId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val calls = snapshot.documents.mapNotNull { doc ->
                try {
                    Call.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing call from Firebase", e)
                    null
                }
            }

            // Insérer dans la DB locale
            calls.forEach { call ->
                callDao.insertCall(call)
            }

            Log.d(TAG, "Synced ${calls.size} calls from Firebase")
        }
    }

    /**
     * Nettoyer les appels anciens (plus de 30 jours)
     */
    suspend fun cleanOldCalls() {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            callDao.deleteCallsOlderThan(thirtyDaysAgo)
            Log.d(TAG, "Old calls cleaned successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old calls", e)
        }
    }

    /**
     * Marquer tous les appels manqués comme vus
     */
    suspend fun markMissedCallsAsSeen(userId: String) {
        try {
            val missedCalls = callDao.getMissedCalls().first()
            missedCalls.forEach { call ->
                if (call.receiverId == userId && call.status == CallStatus.MISSED) {
                    val updatedCall = call.copy(status = CallStatus.DECLINED)
                    callDao.updateCall(updatedCall)
                }
            }
            Log.d(TAG, "Marked ${missedCalls.size} missed calls as seen")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking missed calls as seen", e)
        }
    }

    /**
     * Obtenir un appel par ID
     */
    suspend fun getCallById(callId: String): Call? {
        return try {
            callDao.getCallById(callId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting call by ID", e)
            null
        }
    }

    /**
     * Obtenir le nombre d'appels manqués
     */
    suspend fun getMissedCallsCount(userId: String): Int {
        return try {
            callDao.getMissedCalls().first().count { 
                it.receiverId == userId && it.status == CallStatus.MISSED 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting missed calls count", e)
            0
        }
    }

    /**
     * Supprimer tous les appels d'une conversation
     */
    suspend fun deleteCallsByConversation(conversationId: String) {
        try {
            val calls = callDao.getCallsByConversation(conversationId).first()
            calls.forEach { call ->
                callsCollection.document(call.id).delete().await()
                callDao.deleteCall(call.id)
            }
            Log.d(TAG, "Deleted ${calls.size} calls from conversation")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting calls by conversation", e)
        }
    }

    // ============= NOUVELLES FONCTIONNALITÉS AVANCÉES =============

    /**
     * 👥 Initier un appel de groupe
     */
    suspend fun initiateGroupCall(
        conversationId: String,
        callerId: String,
        callerName: String,
        callerPhotoUrl: String,
        participantIds: List<String>,
        participantNames: List<String>,
        participantPhotoUrls: List<String>,
        type: CallType
    ): Result<Call> {
        return withRetry(maxRetries = 3) {
            val callId = UUID.randomUUID().toString()
            val call = Call(
                id = callId,
                conversationId = conversationId,
                callerId = callerId,
                callerName = callerName,
                callerPhotoUrl = callerPhotoUrl,
                receiverId = "", // Pas de receiver unique pour les appels de groupe
                receiverName = "",
                receiverPhotoUrl = "",
                type = type,
                status = CallStatus.OUTGOING,
                timestamp = System.currentTimeMillis(),
                isGroupCall = true,
                groupParticipants = participantIds,
                groupParticipantNames = participantNames,
                groupParticipantPhotos = participantPhotoUrls
            )

            // Sauvegarder sur Firestore
            callsCollection.document(callId).set(call.toMap()).await()

            // Sauvegarder localement
            callDao.insertCall(call)
            
            Log.d(TAG, "Group call initiated with ${participantIds.size} participants")
            call
        }
    }

    /**
     * 📝 Ajouter une note à un appel
     */
    suspend fun addCallNote(callId: String, note: String): Result<Unit> {
        return try {
            callsCollection.document(callId)
                .update(mapOf(
                    "note" to note,
                    "noteAddedAt" to System.currentTimeMillis()
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(note = note))
            }

            Log.d(TAG, "Note added to call: $callId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding call note", e)
            Result.failure(e)
        }
    }

    /**
     * 📊 Évaluer la qualité de l'appel
     */
    suspend fun rateCallQuality(
        callId: String,
        rating: Int, // 1-5
        feedback: String = ""
    ): Result<Unit> {
        return try {
            if (rating < 1 || rating > 5) {
                return Result.failure(Exception("Rating must be between 1 and 5"))
            }

            callsCollection.document(callId)
                .update(mapOf(
                    "qualityRating" to rating,
                    "qualityFeedback" to feedback,
                    "ratedAt" to System.currentTimeMillis()
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(
                    qualityRating = rating,
                    qualityFeedback = feedback
                ))
            }

            Log.d(TAG, "Call quality rated: $rating stars")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error rating call quality", e)
            Result.failure(e)
        }
    }

    /**
     * 🔄 Planifier un rappel automatique
     */
    suspend fun scheduleCallBack(
        conversationId: String,
        callerId: String,
        receiverId: String,
        scheduleTime: Long, // Timestamp
        type: CallType
    ): Result<String> {
        return try {
            val scheduledCallId = UUID.randomUUID().toString()
            
            val scheduledCallData = mapOf(
                "id" to scheduledCallId,
                "conversationId" to conversationId,
                "callerId" to callerId,
                "receiverId" to receiverId,
                "scheduleTime" to scheduleTime,
                "type" to type.name,
                "status" to "SCHEDULED",
                "createdAt" to System.currentTimeMillis()
            )

            callsCollection.document(scheduledCallId)
                .set(scheduledCallData)
                .await()

            Log.d(TAG, "Call scheduled for: $scheduleTime")
            Result.success(scheduledCallId)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling call", e)
            Result.failure(e)
        }
    }

    /**
     * 📞 Rappeler automatiquement le dernier appelant
     */
    suspend fun callBack(userId: String): Result<Call?> {
        return try {
            val recentCalls = callDao.getRecentCalls(10).first()
            
            // Trouver le dernier appel manqué ou décliné
            val lastMissedCall = recentCalls.firstOrNull { call ->
                (call.receiverId == userId && 
                 (call.status == CallStatus.MISSED || call.status == CallStatus.DECLINED))
            }

            if (lastMissedCall != null) {
                // Initier un nouvel appel vers l'appelant précédent
                val newCall = initiateCall(
                    conversationId = lastMissedCall.conversationId,
                    callerId = userId,
                    callerName = lastMissedCall.receiverName,
                    callerPhotoUrl = lastMissedCall.receiverPhotoUrl,
                    receiverId = lastMissedCall.callerId,
                    receiverName = lastMissedCall.callerName,
                    receiverPhotoUrl = lastMissedCall.callerPhotoUrl,
                    type = lastMissedCall.type
                )
                
                Result.success(newCall.getOrNull())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling back", e)
            Result.failure(e)
        }
    }

    /**
     * 🎥 Passer d'un appel vocal à vidéo
     */
    suspend fun upgradeToVideoCall(callId: String): Result<Unit> {
        return try {
            callsCollection.document(callId)
                .update(mapOf(
                    "type" to CallType.VIDEO.name,
                    "isVideoAccepted" to true,
                    "videoUpgradedAt" to System.currentTimeMillis()
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(
                    type = CallType.VIDEO,
                    isVideoAccepted = true
                ))
            }

            Log.d(TAG, "Call upgraded to video")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error upgrading to video", e)
            Result.failure(e)
        }
    }

    /**
     * 🔇 Activer/désactiver le mode silencieux pendant l'appel
     */
    suspend fun toggleMute(callId: String, isMuted: Boolean): Result<Unit> {
        return try {
            callsCollection.document(callId)
                .update(mapOf(
                    "isMuted" to isMuted,
                    "mutedAt" to if (isMuted) System.currentTimeMillis() else 0L
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(isMuted = isMuted))
            }

            Log.d(TAG, "Mute status: $isMuted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
            Result.failure(e)
        }
    }

    /**
     * 📷 Activer/désactiver la caméra pendant l'appel vidéo
     */
    suspend fun toggleCamera(callId: String, isCameraOn: Boolean): Result<Unit> {
        return try {
            callsCollection.document(callId)
                .update(mapOf(
                    "isCameraOn" to isCameraOn,
                    "cameraToggledAt" to System.currentTimeMillis()
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(isCameraOn = isCameraOn))
            }

            Log.d(TAG, "Camera status: $isCameraOn")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling camera", e)
            Result.failure(e)
        }
    }

    /**
     * 🔊 Activer/désactiver le haut-parleur
     */
    suspend fun toggleSpeaker(callId: String, isSpeakerOn: Boolean): Result<Unit> {
        return try {
            callsCollection.document(callId)
                .update(mapOf(
                    "isSpeakerOn" to isSpeakerOn,
                    "speakerToggledAt" to System.currentTimeMillis()
                ))
                .await()

            val call = callDao.getCallById(callId)
            if (call != null) {
                callDao.updateCall(call.copy(isSpeakerOn = isSpeakerOn))
            }

            Log.d(TAG, "Speaker status: $isSpeakerOn")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling speaker", e)
            Result.failure(e)
        }
    }

    /**
     * 📊 Obtenir les statistiques détaillées des appels
     */
    suspend fun getDetailedCallStats(userId: String): Map<String, Any> {
        return try {
            val calls = callDao.getCallsByUser(userId).first()
            
            val totalCalls = calls.size
            val totalDuration = calls.sumOf { it.duration }
            val missedCalls = calls.count { it.status == CallStatus.MISSED }
            val completedCalls = calls.count { it.status == CallStatus.COMPLETED }
            val declinedCalls = calls.count { it.status == CallStatus.DECLINED }
            val videoCalls = calls.count { it.type == CallType.VIDEO }
            val voiceCalls = calls.count { it.type == CallType.VOICE }
            
            val averageDuration = if (completedCalls > 0) totalDuration / completedCalls else 0L
            val longestCall = calls.maxByOrNull { it.duration }
            val shortestCall = calls.filter { it.duration > 0 }.minByOrNull { it.duration }
            
            val callsByDay = calls.groupBy {
                val date = java.util.Date(it.timestamp)
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(date)
            }.mapValues { it.value.size }

            mapOf(
                "totalCalls" to totalCalls,
                "totalDuration" to totalDuration,
                "averageDuration" to averageDuration,
                "missedCalls" to missedCalls,
                "completedCalls" to completedCalls,
                "declinedCalls" to declinedCalls,
                "videoCalls" to videoCalls,
                "voiceCalls" to voiceCalls,
                "longestCallDuration" to (longestCall?.duration ?: 0L),
                "shortestCallDuration" to (shortestCall?.duration ?: 0L),
                "callsByDay" to callsByDay,
                "answerRate" to if (totalCalls > 0) (completedCalls.toDouble() / totalCalls * 100) else 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detailed call stats", e)
            emptyMap()
        }
    }

    /**
     * 🎯 Obtenir les appels avec un utilisateur spécifique
     */
    suspend fun getCallsWithUser(currentUserId: String, otherUserId: String): Result<List<Call>> {
        return try {
            val allCalls = callDao.getCallsByUser(currentUserId).first()
            
            val callsWithUser = allCalls.filter { call ->
                (call.callerId == currentUserId && call.receiverId == otherUserId) ||
                (call.receiverId == currentUserId && call.callerId == otherUserId)
            }

            Result.success(callsWithUser)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calls with user", e)
            Result.failure(e)
        }
    }

    /**
     * 📅 Obtenir les appels par période
     */
    suspend fun getCallsByPeriod(
        userId: String,
        startTime: Long,
        endTime: Long
    ): Result<List<Call>> {
        return try {
            val allCalls = callDao.getCallsByUser(userId).first()
            
            val periodCalls = allCalls.filter { call ->
                call.timestamp in startTime..endTime
            }

            Result.success(periodCalls)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calls by period", e)
            Result.failure(e)
        }
    }

    /**
     * 🔔 Obtenir le nombre d'appels manqués récents
     */
    suspend fun getRecentMissedCallsCount(userId: String, since: Long): Int {
        return try {
            val missedCalls = callDao.getMissedCalls().first()
            missedCalls.count { call ->
                call.receiverId == userId && 
                call.status == CallStatus.MISSED &&
                call.timestamp >= since
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent missed calls count", e)
            0
        }
    }

    /**
     * 💾 Exporter l'historique des appels
     */
    suspend fun exportCallHistory(userId: String): Result<String> {
        return try {
            val calls = callDao.getCallsByUser(userId).first()
            
            val exportData = calls.joinToString("\n") { call ->
                "${call.timestamp},${call.type},${call.status},${call.duration},${call.callerName},${call.receiverName}"
            }

            val header = "Timestamp,Type,Status,Duration,Caller,Receiver\n"
            val fullExport = header + exportData

            Log.d(TAG, "Call history exported: ${calls.size} calls")
            Result.success(fullExport)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting call history", e)
            Result.failure(e)
        }
    }

    /**
     * 🏆 Obtenir les contacts les plus appelés
     */
    suspend fun getMostCalledContacts(userId: String, limit: Int = 5): Result<List<Pair<String, Int>>> {
        return try {
            val calls = callDao.getCallsByUser(userId).first()
            
            val contactCallCounts = mutableMapOf<String, Int>()
            
            calls.forEach { call ->
                val contactId = if (call.callerId == userId) call.receiverId else call.callerId
                val contactName = if (call.callerId == userId) call.receiverName else call.callerName
                
                if (contactId.isNotEmpty()) {
                    contactCallCounts[contactName] = (contactCallCounts[contactName] ?: 0) + 1
                }
            }

            val sortedContacts = contactCallCounts.toList()
                .sortedByDescending { it.second }
                .take(limit)

            Result.success(sortedContacts)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting most called contacts", e)
            Result.failure(e)
        }
    }

    /**
     * 🔄 Synchroniser les appels entrants avec Firebase
     */
    suspend fun syncIncomingCalls(userId: String): Result<Unit> {
        return withRetry(maxRetries = 2) {
            Log.d(TAG, "Syncing incoming calls for user: $userId")
            
            val snapshot = callsCollection
                .whereEqualTo("receiverId", userId)
                .whereGreaterThan("timestamp", System.currentTimeMillis() - 24 * 60 * 60 * 1000) // Dernières 24h
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val calls = snapshot.documents.mapNotNull { doc ->
                try {
                    Call.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing call", e)
                    null
                }
            }

            calls.forEach { call ->
                callDao.insertCall(call)
            }

            Log.d(TAG, "Synced ${calls.size} incoming calls")
        }
    }
}
