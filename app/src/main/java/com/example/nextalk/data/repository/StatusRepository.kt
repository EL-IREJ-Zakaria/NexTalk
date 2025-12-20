package com.example.nextalk.data.repository

import android.net.Uri
import android.util.Log
import com.example.nextalk.data.local.dao.StatusDao
import com.example.nextalk.data.model.Status
import com.example.nextalk.data.model.StatusReply
import com.example.nextalk.data.model.StatusType
import com.example.nextalk.service.MediaService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository pour gérer les statuts
 */
class StatusRepository(
    private val statusDao: StatusDao,
    private val mediaService: MediaService = MediaService()
) {

    private val firestore = FirebaseFirestore.getInstance()
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
            // Upload le média via MediaService
            val uploadResult = when (type) {
                StatusType.IMAGE -> mediaService.uploadStatusImage(mediaUri)
                StatusType.VIDEO -> mediaService.uploadStatusVideo(mediaUri)
                else -> return Result.failure(Exception("Invalid media type for status"))
            }

            // Vérifier que l'upload a réussi
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
            }

            val mediaUrl = uploadResult.getOrNull() ?: return Result.failure(Exception("No media URL returned"))

            val statusId = UUID.randomUUID().toString()
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
                mapOf("viewedBy" to updatedViewedBy)
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
                mapOf(
                    "replies" to updatedReplies.map {
                        mapOf(
                            "userId" to it.userId,
                            "userName" to it.userName,
                            "message" to it.message,
                            "timestamp" to it.timestamp
                        )
                    }
                )
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

    /**
     * Synchroniser les statuts avec Firebase
     */
    suspend fun syncStatusesFromFirebase(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Syncing statuses from Firebase for user: $userId")
            
            val snapshot = statusesCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .get()
                .await()

            val statuses = snapshot.documents.mapNotNull { doc ->
                try {
                    Status.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing status from Firebase", e)
                    null
                }
            }

            // Insérer dans la DB locale
            statuses.forEach { status ->
                statusDao.insertStatus(status)
            }

            Log.d(TAG, "Synced ${statuses.size} statuses from Firebase")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing statuses from Firebase", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les statuts paginés
     */
    suspend fun getStatusesPaginated(
        limit: Int = 20,
        lastTimestamp: Long? = null
    ): Result<List<Status>> {
        return try {
            val query = if (lastTimestamp != null) {
                statusesCollection
                    .whereGreaterThan("expiresAt", System.currentTimeMillis())
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .startAfter(lastTimestamp)
                    .limit(limit.toLong())
            } else {
                statusesCollection
                    .whereGreaterThan("expiresAt", System.currentTimeMillis())
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(limit.toLong())
            }

            val snapshot = query.get().await()
            val statuses = snapshot.documents.mapNotNull { doc ->
                try {
                    Status.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing status", e)
                    null
                }
            }

            Result.success(statuses)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated statuses", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les statuts non vus pour un utilisateur
     */
    fun getUnviewedStatuses(userId: String): Flow<List<Status>> = statusDao
        .getRecentStatuses(100)
        .catch { e ->
            Log.e(TAG, "Error getting unviewed statuses", e)
            emit(emptyList())
        }

    /**
     * Supprimer un statut avec son média associé
     */
    suspend fun deleteStatusWithMedia(statusId: String) {
        try {
            val status = statusDao.getStatusById(statusId)
            
            if (status != null && (status.type == StatusType.IMAGE || status.type == StatusType.VIDEO)) {
                // Supprimer le fichier média
                mediaService.deleteFile(status.content)
            }

            // Supprimer le statut de Firestore et de la DB locale
            statusesCollection.document(statusId).delete().await()
            statusDao.deleteStatus(statusId)
            
            Log.d(TAG, "Status and media deleted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting status with media", e)
        }
    }

    /**
     * Mettre à jour la durée d'un statut vidéo
     */
    suspend fun updateStatusDuration(statusId: String, duration: Long): Result<Unit> {
        return try {
            val status = statusDao.getStatusById(statusId) 
                ?: return Result.failure(Exception("Status not found"))

            val updatedStatus = status.copy(duration = duration)
            
            statusesCollection.document(statusId).update("duration", duration).await()
            statusDao.updateStatus(updatedStatus)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating status duration", e)
            Result.failure(e)
        }
    }
}
