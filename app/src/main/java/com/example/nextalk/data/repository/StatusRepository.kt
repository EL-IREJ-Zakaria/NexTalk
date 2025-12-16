package com.example.nextalk.data.repository

import android.net.Uri
import android.util.Log
import com.example.nextalk.data.local.dao.StatusDao
import com.example.nextalk.data.model.Status
import com.example.nextalk.data.model.StatusReply
import com.example.nextalk.data.model.StatusType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository pour gérer les statuts
 */
class StatusRepository(private val statusDao: StatusDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val statusesCollection = firestore.collection("statuses")

    companion object {
        private const val TAG = "StatusRepository"
    }

    /**
     * Créer un statut texte
     */
    suspend fun createTextStatus(
        userId: String,
        userName: String,
        userPhotoUrl: String,
        text: String,
        backgroundColor: String = "#075E54",
        textColor: String = "#FFFFFF"
    ): Result<Status> {
        return try {
            val statusId = UUID.randomUUID().toString()
            val status = Status(
                id = statusId,
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                content = text,
                type = StatusType.TEXT,
                backgroundColor = backgroundColor,
                textColor = textColor,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            )

            statusesCollection.document(statusId).set(status.toMap()).await()
            statusDao.insertStatus(status)

            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating text status", e)
            Result.failure(e)
        }
    }

    /**
     * Créer un statut image/vidéo
     */
    suspend fun createMediaStatus(
        userId: String,
        userName: String,
        userPhotoUrl: String,
        mediaUri: Uri,
        type: StatusType
    ): Result<Status> {
        return try {
            val statusId = UUID.randomUUID().toString()
            val fileName = "statuses/${UUID.randomUUID()}.${if (type == StatusType.IMAGE) "jpg" else "mp4"}"
            val mediaRef = storage.reference.child(fileName)

            mediaRef.putFile(mediaUri).await()
            val mediaUrl = mediaRef.downloadUrl.await().toString()

            val status = Status(
                id = statusId,
                userId = userId,
                userName = userName,
                userPhotoUrl = userPhotoUrl,
                content = mediaUrl,
                type = type,
                createdAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000,
                duration = if (type == StatusType.VIDEO) 30000L else 5000L
            )

            statusesCollection.document(statusId).set(status.toMap()).await()
            statusDao.insertStatus(status)

            Result.success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating media status", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les statuts d'un utilisateur
     */
    fun getStatusesByUser(userId: String): Flow<List<Status>> = statusDao
        .getStatusesByUser(userId)
        .catch { e ->
            Log.e(TAG, "Error getting user statuses", e)
            emit(emptyList())
        }

    /**
     * Obtenir tous les utilisateurs avec statuts
     */
    fun getAllUsersWithStatuses(): Flow<List<String>> = statusDao
        .getAllUsersWithStatuses()
        .catch { e ->
            Log.e(TAG, "Error getting users with statuses", e)
            emit(emptyList())
        }

    /**
     * Obtenir les statuts récents
     */
    fun getRecentStatuses(limit: Int = 50): Flow<List<Status>> = statusDao
        .getRecentStatuses(limit)
        .catch { e ->
            Log.e(TAG, "Error getting recent statuses", e)
            emit(emptyList())
        }

    /**
     * Marquer un statut comme vu
     */
    suspend fun markStatusAsViewed(statusId: String, userId: String) {
        try {
            val status = statusDao.getStatusById(statusId) ?: return
            val updatedViewedBy = (status.viewedBy + userId).distinct()

            statusesCollection.document(statusId).update(
                "viewedBy" to updatedViewedBy
            ).await()

            statusDao.updateStatus(status.copy(viewedBy = updatedViewedBy))
        } catch (e: Exception) {
            Log.e(TAG, "Error marking status as viewed", e)
        }
    }

    /**
     * Ajouter une réponse à un statut
     */
    suspend fun replyToStatus(
        statusId: String,
        userId: String,
        userName: String,
        message: String
    ): Result<Unit> {
        return try {
            val status = statusDao.getStatusById(statusId) ?: return Result.failure(Exception("Status not found"))
            val reply = StatusReply(userId, userName, message)
            val updatedReplies = status.replies + reply

            statusesCollection.document(statusId).update(
                "replies" to updatedReplies.map {
                    mapOf(
                        "userId" to it.userId,
                        "userName" to it.userName,
                        "message" to it.message,
                        "timestamp" to it.timestamp
                    )
                }
            ).await()

            statusDao.updateStatus(status.copy(replies = updatedReplies))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error replying to status", e)
            Result.failure(e)
        }
    }

    /**
     * Supprimer un statut
     */
    suspend fun deleteStatus(statusId: String) {
        try {
            statusesCollection.document(statusId).delete().await()
            statusDao.deleteStatus(statusId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting status", e)
        }
    }

    /**
     * Supprimer les statuts expirés
     */
    suspend fun deleteExpiredStatuses() {
        try {
            statusDao.deleteExpiredStatuses()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expired statuses", e)
        }
    }

    /**
     * Obtenir le nombre de statuts d'un utilisateur
     */
    fun getStatusCountByUser(userId: String): Flow<Int> = statusDao
        .getStatusCountByUser(userId)
        .catch { e ->
            Log.e(TAG, "Error getting status count", e)
            emit(0)
        }
}
